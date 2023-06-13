/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IGPUVertsRenderable;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.ImmediateModeRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.component.Verts;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.view.VertsView;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3d;
import org.lwjgl.opengl.GL30;

import java.util.List;

public class PrimitiveVertexRenderSystem<T extends IGPUVertsRenderable> extends ImmediateModeRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(PrimitiveVertexRenderSystem.class);

    final static double baseWidthAngle = Math.toRadians(.13);
    final static double baseWidthAngleTan = Math.tan(baseWidthAngle);

    protected final boolean lines;
    protected final VertsView vertsView;
    protected ICamera camera;
    protected int coordOffset;

    public PrimitiveVertexRenderSystem(SceneRenderer sceneRenderer,
                                       RenderGroup rg,
                                       float[] alphas,
                                       ExtShaderProgram[] shaders,
                                       boolean lines) {
        super(sceneRenderer, rg, alphas, shaders);
        this.lines = lines;
        this.vertsView = new VertsView();
        EventManager.instance.subscribe(this, Event.GPU_DISPOSE_VERTS_OBJECT);
    }

    public boolean isLine() {
        return lines;
    }

    public boolean isPoint() {
        return !lines;
    }

    @Override
    protected void initShaderProgram() {
        if (isLine()) {
            Gdx.gl.glEnable(GL30.GL_LINE_SMOOTH);
            Gdx.gl.glHint(GL30.GL_NICEST, GL30.GL_LINE_SMOOTH_HINT);
        } else if (isPoint()) {
            Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);
        }
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param nVertices The max number of vertices this mesh data can hold
     *
     * @return The index of the new mesh data
     */
    private int addMeshData(int nVertices) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attributes);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        coordOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        return mdi;
    }

    @Override
    public void renderStud(List<IRenderable> renderables,
                           ICamera camera,
                           double t) {
        if (isLine()) {
            initShaderProgram();
        }
        ExtShaderProgram shaderProgram = getShaderProgram();
        shaderProgram.begin();

        this.camera = camera;
        renderables.forEach(r -> {
            Render render = (Render) r;
            if (!Mapper.verts.has(render.entity)) {
                logger.error("Can't render entities without a " + Verts.class.getSimpleName() + " component.");
            }
            var trajectory = Mapper.trajectory.get(render.entity);
            var coordEnabled = trajectory != null && trajectory.orbitTrail;

            // We use a VertsView object.
            vertsView.setEntity(render.entity);
            T renderable = (T) vertsView;
            /*
             * ADD LINES
             */
            if (!inGpu(render)) {
                // Actually add data.
                PointCloudData data = renderable.getPointCloud();
                int nPoints = data.getNumPoints();

                // Initialize or fetch mesh data. We reuse if we can.
                if (getOffset(render) < 0) {
                    setOffset(render, addMeshData(nPoints));
                } else {
                    curr = meshes.get(getOffset(render));
                    // Check we still have capacity, otherwise, reinitialize.
                    if (curr.numVertices != data.getNumPoints()) {
                        curr.clear();
                        curr.mesh.dispose();
                        meshes.set(getOffset(render), null);
                        setOffset(render, addMeshData(nPoints));
                    }
                }
                // Coord maps time.
                boolean hasTime = data.hasTime();
                long t0 = hasTime ? data.getDate(0).getEpochSecond() : 0;
                long t1 = hasTime ? data.getDate(data.getNumPoints() - 1).getEpochSecond() : 0;
                long t01 = t1 - t0;

                // Ensure vertices capacity.
                ensureTempVertsSize((nPoints + 2) * curr.vertexSize);
                curr.vertices = tempVerts;
                float[] cc = renderable.getColor();
                for (int point_i = 0; point_i < nPoints; point_i++) {
                    coord(!hasTime ? (coordEnabled ? (float) point_i / (float) nPoints : 1f) : (float) ((double) (data.getDate(point_i).getEpochSecond() - t0) / (double) t01));
                    color(cc[0], cc[1], cc[2], 1.0);
                    vertex((float) data.getX(point_i), (float) data.getY(point_i), (float) data.getZ(point_i));
                }

                // Close loop.
                if (renderable.isClosedLoop()) {
                    coord(1f);
                    color(cc[0], cc[1], cc[2], 1.0);
                    vertex((float) data.getX(0), (float) data.getY(0), (float) data.getZ(0));
                }

                int count = nPoints * curr.vertexSize;
                setCount(render, count);
                curr.mesh.setVertices(curr.vertices, 0, count);
                curr.vertices = null;

                setInGpu(render, true);
            }
            curr = meshes.get(getOffset(render));

            /*
             * RENDER
             */

            var base = Mapper.base.get(render.entity);

            // Regular.
            if (isLine()) {
                float lw = vertsView.getPrimitiveSize();
                shaderProgram.setUniformf("u_lineWidthTan", (float) (lw * 0.8f * baseWidthAngleTan * Settings.settings.scene.lineWidth * camera.getFovFactor()));
            } else {
                shaderProgram.setUniformf("u_pointSize", renderable.getPrimitiveSize());
            }

            shaderProgram.setUniformMatrix("u_worldTransform", renderable.getLocalTransform());
            shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_alpha", (float) (renderable.getAlpha()) * getAlpha(renderable) * base.opacity * 0.6f);
            shaderProgram.setUniformf("u_coordEnabled", coordEnabled ? 1f : -1f);
            if (trajectory != null && trajectory.body != null) {
                var bodyGraph = Mapper.graph.get(trajectory.body);
                shaderProgram.setUniformf("u_bodyPos", bodyGraph.translation.x.floatValue(), bodyGraph.translation.y.floatValue(), bodyGraph.translation.z.floatValue());
            } else {
                shaderProgram.setUniformf("u_bodyPos", Float.NaN, Float.NaN, Float.NaN);
            }
            shaderProgram.setUniformf("u_trailMap", trajectory != null ? trajectory.trailMap : 0.0f);
            shaderProgram.setUniformf("u_coordPos", trajectory != null ? (float) trajectory.coord : 1f);
            shaderProgram.setUniformf("u_period", trajectory != null && trajectory.oc != null ? (float) trajectory.oc.period : 0f);
            Entity parent = renderable.getParentEntity();
            if (parent != null) {
                Vector3d urp = Mapper.attitude.has(parent) ? Mapper.attitude.get(parent).nonRotatedPos : null;
                if (urp != null)
                    shaderProgram.setUniformf("u_parentPos", (float) urp.x, (float) urp.y, (float) urp.z);
                else
                    shaderProgram.setUniformf("u_parentPos", 0, 0, 0);
            }

            // Rel, grav, z-buffer.
            addEffectsUniforms(shaderProgram, camera);
            curr.mesh.render(shaderProgram, renderable.getGlPrimitive());

        });
        shaderProgram.end();
    }

    private void coord(float value) {
        curr.vertices[curr.vertexIdx + coordOffset] = value;
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        attributes.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.Generic, 1, "a_coord"));

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (event == Event.GPU_DISPOSE_VERTS_OBJECT) {
            IRenderable renderable = (IRenderable) source;
            RenderGroup rg = (RenderGroup) data[0];
            if (rg == RenderGroup.LINE_GPU || rg == RenderGroup.POINT_GPU) {
                setInGpu(renderable, false);
            }
        }
    }
}
