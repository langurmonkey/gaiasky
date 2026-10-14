/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import gaiasky.render.RenderGroup;
import gaiasky.scene.component.Volume;
import org.junit.Assert;
import org.junit.Test;

/**
 * {@link Volume#writeDepth} is what makes a volume occlude the objects drawn after it (labels,
 * grids, stars, planets). These tests pin the default, the attribute contract that the scene
 * JSON relies on, and the placement of the depth pre-pass in the render order - none of which
 * fails loudly if it regresses: a missing setter only logs a loader error at startup, and a
 * pre-pass in the wrong position is silently wiped or silently too late.
 */
public class VolumeComponentTest {

    @Test
    public void writeDepthDefaultsToTrue() {
        // Volumes occlude unless a scene explicitly opts out.
        Assert.assertTrue(new Volume().writeDepth);
    }

    @Test
    public void setWriteDepthHandlesBothBooleanValuesAndNull() {
        var v = new Volume();

        v.setWriteDepth(false);
        Assert.assertFalse(v.writeDepth);

        v.setWriteDepth(true);
        Assert.assertTrue(v.writeDepth);

        // Null must not clobber the current value.
        v.setWriteDepth(false);
        v.setWriteDepth(null);
        Assert.assertFalse(v.writeDepth);
    }

    @Test
    public void jsonLoaderContract() throws Exception {
        // JsonLoader.fillEntity() looks for "set" + property with the JSON value class (Boolean for
        // a boolean attribute) and falls back to a direct public field. Both paths must exist for
        // the "writeDepth" attribute declared in archetypes/attributemap.json under Volume.
        Method m = ClassReflection.getMethod(Volume.class, "setWriteDepth", Boolean.class);
        Assert.assertNotNull("missing Volume.setWriteDepth(Boolean)", m);
        Assert.assertNotNull("missing public Volume.writeDepth field",
                             ClassReflection.getField(Volume.class, "writeDepth"));
    }

    @Test
    public void depthPrePassSitsBetweenTheGridClearAndTheStars() {
        // The depth-only pre-pass has to run after MODEL_VERT_GRID, whose post-runnable clears the
        // depth buffer (so that grids leave no depth behind) - anything written before that is
        // wiped. And it has to run before POINT_STAR, which is the first pass that reads depth and
        // so the first one the volume can occlude.
        Assert.assertTrue("VOLUME_DEPTH must run after MODEL_VERT_GRID, which clears depth",
                          RenderGroup.VOLUME_DEPTH.priority > RenderGroup.MODEL_VERT_GRID.priority);
        Assert.assertTrue("VOLUME_DEPTH must run before POINT_STAR to occlude the stars",
                          RenderGroup.VOLUME_DEPTH.priority < RenderGroup.POINT_STAR.priority);
        // The colour pass is later still, and must stay that way: it is additive and needs the
        // depth the pre-pass wrote to be rejected where it is not the front of the volume.
        Assert.assertTrue(RenderGroup.VOLUME_DEPTH.priority < RenderGroup.VOLUME.priority);
    }

    @Test
    public void materialLoadsTheDepthThresholds() throws Exception {
        // The scene loader resolves a JSON property to an exact-typed setter (JsonLoader
        // .searchMethod does no widening): a scalar 0.05 arrives as Double, an array of decimals
        // as double[], and an array of integers as int[]. If one of these setter overloads is
        // missing, the load fails with a reflection error and the volume silently keeps the
        // default of "never occludes" - which is the opposite of what the scene asked for.
        //
        // MaterialComponent is loaded without initialising it, because its static initialiser
        // builds a BiomeLUTManager out of GaiaSky.settings() and needs a live instance.
        Class<?> material = Class.forName("gaiasky.scene.record.MaterialComponent",
                                          false,
                                          VolumeComponentTest.class.getClassLoader());

        Assert.assertNotNull("missing MaterialComponent.setVolumeDepthThreshold(double[])",
                             material.getDeclaredMethod("setVolumeDepthThreshold", double[].class));
        Assert.assertNotNull("missing MaterialComponent.setVolumeDepthThreshold(int[])",
                             material.getDeclaredMethod("setVolumeDepthThreshold", int[].class));
        Assert.assertEquals("volumeDepthThreshold must be a double[] for the loader to fill it",
                            double[].class,
                            material.getDeclaredField("volumeDepthThreshold").getType());
    }
}
