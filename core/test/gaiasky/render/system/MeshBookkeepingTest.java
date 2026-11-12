/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.ComponentTypes;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.camera.ICamera;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * The per-renderable offsets and counts are what the draw call is built from, so their bookkeeping is
 * pinned here. The bug this covers: dropping a renderable's mesh (offset -1) left its count behind, so
 * the next build appended a new count while getCount() kept returning the first one. The renderer then
 * drew that stale number of instances from a buffer filled with fewer, which reads past the end of the
 * instance buffer: harmless zeros on some drivers, visible garbage on others.
 */
public class MeshBookkeepingTest {

    /** Minimal concrete system: only the bookkeeping is exercised here, no meshes are created. */
    private static class TestSystem extends ImmediateModeRenderSystem {
        TestSystem() {
            super(null, null, null, null);
        }

        @Override
        protected void initVertices() {
        }

        protected void addAttributesDivisor1(Array<VertexAttribute> attributes, int primitive) {
        }

        protected void offsets0(MeshData curr, InstancedRenderSystem.InstancedModel model) {
        }

        protected void offsets1(MeshData curr, InstancedRenderSystem.InstancedModel model) {
        }

        @Override
        public void renderStud(List<IRenderable> renderables, ICamera camera, double t) {
        }

        void offset(IRenderable r, int o) {
            setOffset(r, o);
        }

        void count(IRenderable r, int c) {
            setCount(r, c);
        }

        int offsetOf(IRenderable r) {
            return getOffset(r);
        }

        int countOf(IRenderable r) {
            return getCount(r);
        }
    }

    private static class DummyRenderable implements IRenderable {
        @Override
        public ComponentTypes getComponentType() {
            return null;
        }

        @Override
        public double getDistToCamera() {
            return 0;
        }

        @Override
        public float getOpacity() {
            return 1;
        }

        @Override
        public boolean isHalfResolutionBuffer() {
            return false;
        }
    }

    @Test
    public void unknownRenderableHasNoOffsetNorCount() {
        var sys = new TestSystem();
        var r = new DummyRenderable();
        assertEquals(-1, sys.offsetOf(r));
        assertEquals(-1, sys.countOf(r));
    }

    @Test
    public void offsetAndCountRoundTrip() {
        var sys = new TestSystem();
        var r = new DummyRenderable();
        sys.offset(r, 5);
        sys.count(r, 120);
        assertEquals(5, sys.offsetOf(r));
        assertEquals(120, sys.countOf(r));
    }

    @Test
    public void forgettingTheMeshForgetsTheCount() {
        var sys = new TestSystem();
        var r = new DummyRenderable();
        sys.offset(r, 3);
        sys.count(r, 900);

        // What a GPU dispose does.
        sys.offset(r, -1);

        assertEquals(-1, sys.offsetOf(r));
        assertEquals(-1, sys.countOf(r));

        // The rebuild must be measured by its own count, not by the first one ever buffered.
        sys.offset(r, 3);
        sys.count(r, 12);
        assertEquals(3, sys.offsetOf(r));
        assertEquals(12, sys.countOf(r));
    }

    @Test
    public void severalMeshesPerRenderableAreKeptInOrder() {
        // A renderable with more than one mesh reads the pair at the same index.
        var sys = new TestSystem();
        var r = new DummyRenderable();
        sys.offset(r, 0);
        sys.count(r, 8);
        sys.offset(r, 1);
        sys.count(r, 20);

        assertEquals(0, sys.offsetOf(r));
        assertEquals(8, sys.countOf(r));
        assertEquals(1, sys.getOffsets(r).get(1));
        assertEquals(20, sys.getCounts(r).get(1));
    }

    @Test
    public void aFreshMeshHasNoInstancesToDraw() {
        var md = new ImmediateModeRenderSystem.MeshData();
        // Nothing has been uploaded yet, so nothing may be drawn: instanceCount is the only safe
        // number to pass to glDrawArraysInstanced.
        assertEquals(0, md.instanceCount);

        md.instanceCount = 42;
        md.instanceIdx = 42;
        md.clear();
        assertEquals(0, md.instanceCount);
        assertEquals(0, md.instanceIdx);
    }
}
