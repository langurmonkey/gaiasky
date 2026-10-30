/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.GalaxyAttributes;
import org.junit.Assert;
import org.junit.Test;

/**
 * {@link GalaxyAttributes#packFields(float[], int)} fills the flat uniform that
 * {@code gxUnpack()} in {@code assets/shader/lib/galaxy.glsl} reads back. The two have no compile-time
 * link, so the layout is pinned here: an offset that moves on one side and not the other would
 * silently shuffle every field after it.
 */
public class FieldPackingTest {

    @Test
    public void packMatchesTheShaderLayout() {
        var ds = new BillboardDataset();
        ds.distribution = GalaxyAttributes.Distribution.SPIRAL;
        ds.baseRadius = 1.0f;
        ds.minRadius = 0.19f;
        ds.numArms = 2;
        ds.armSigma = 0.187f;
        ds.baseAngle = 704f;
        ds.eccentricity = new float[]{0.23f, 0.11f};
        ds.spiralDeltaPos = new float[]{-0.5f, 0.5f};
        ds.aspect = 0.3f;
        ds.warpStrength = 0.05f;
        ds.heightScale = 0.02f;
        ds.heightProfile = GalaxyAttributes.HeightProfile.SMOOTH_INC;
        ds.colorNoise = 0.08f;
        ds.intensity = 0.7f;
        ds.type = GalaxyAttributes.ChannelType.DUST;
        ds.baseColors = new float[]{1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f};
        ds.laneWarp = 0.35f;
        ds.dustMask = 0.6f;
        ds.laneNoiseScale = 2.5f;
        ds.laneRough = 1.25f;
        ds.dustMaskFreq = 14f;
        ds.dustMaskOctaves = 5;

        float[] packed = new float[GalaxyAttributes.MAX_FIELDS * GalaxyAttributes.FIELD_STRIDE];
        ds.packFields(packed, GalaxyAttributes.FIELD_STRIDE);

        int o = GalaxyAttributes.FIELD_STRIDE;
        Assert.assertEquals(2f, packed[o], 0f);      // distribution SPIRAL
        Assert.assertEquals(1.0f, packed[o + 1], 0f);
        Assert.assertEquals(0.19f, packed[o + 2], 0f);
        Assert.assertEquals(2f, packed[o + 3], 0f);
        Assert.assertEquals(0.187f, packed[o + 4], 0f);
        Assert.assertEquals(704f, packed[o + 5], 0f);
        Assert.assertEquals(0.23f, packed[o + 6], 0f);
        Assert.assertEquals(0.11f, packed[o + 7], 0f);
        Assert.assertEquals(-0.5f, packed[o + 8], 0f);
        Assert.assertEquals(0.5f, packed[o + 9], 0f);
        Assert.assertEquals(0.3f, packed[o + 10], 0f);
        Assert.assertEquals(0.05f, packed[o + 11], 0f);
        Assert.assertEquals(0.02f, packed[o + 12], 0f);
        Assert.assertEquals(3f, packed[o + 13], 0f);  // height profile SMOOTH_INC
        Assert.assertEquals(0.08f, packed[o + 14], 0f);
        Assert.assertEquals(0.7f, packed[o + 15], 0f); // weight, from intensity
        for (int i = 0; i < 12; i++) {
            Assert.assertEquals(i + 1f, packed[o + 16 + i], 0f);
        }
        Assert.assertEquals(0f, packed[o + 28], 0f); // channel DUST
        Assert.assertEquals(0.35f, packed[o + 29], 0f); // GX_F_LANE_WARP
        Assert.assertEquals(0.6f, packed[o + 30], 0f);  // GX_F_DUST_MASK
        Assert.assertEquals(2.5f, packed[o + 31], 0f);  // GX_F_LANE_NOISE_SCALE
        Assert.assertEquals(1.25f, packed[o + 32], 0f); // GX_F_LANE_ROUGH
        Assert.assertEquals(14f, packed[o + 33], 0f);   // GX_F_DUST_MASK_FREQ
        Assert.assertEquals(5f, packed[o + 34], 0f);    // GX_F_DUST_MASK_OCTAVES
        // Nothing outside this field's window was touched.
        Assert.assertEquals(0f, packed[o + GalaxyAttributes.FIELD_STRIDE - 1 + 1], 0f);
        Assert.assertEquals(0f, packed[0], 0f);
    }

    @Test
    public void packsEveryFieldOfAFullSlotIntoItsOwnWindow() {
        // GX_MAX_FIELDS fields go into one packed array at stride FIELD_STRIDE, and nothing spills
        // into the neighbouring window. Field i is given the intensity i + 1, so a shifted offset
        // shows up as a wrong weight.
        int max = GalaxyAttributes.MAX_FIELDS;
        float[] packed = new float[max * GalaxyAttributes.FIELD_STRIDE];
        var fields = new BillboardDataset[max];
        for (int i = 0; i < max; i++) {
            fields[i] = new BillboardDataset();
            fields[i].intensity = i + 1;
        }
        for (int i = 0; i < max; i++) {
            fields[i].packFields(packed, i * GalaxyAttributes.FIELD_STRIDE);
        }
        for (int i = 0; i < max; i++) {
            int o = i * GalaxyAttributes.FIELD_STRIDE;
            Assert.assertEquals(i + 1f, packed[o + 15], 0f);        // GX_F_WEIGHT
            Assert.assertEquals(5f, packed[o + 28], 0f);            // GX_F_CHANNEL, the default POINT
            Assert.assertEquals(0f, packed[o + 29], 0f);            // GX_F_LANE_WARP, off by default
        }
        // A full slot must fit in the guaranteed uniform space of a GL 4.3 compute shader (1024
        // components), with room for the grid and calibration uniforms on top.
        Assert.assertTrue(max * GalaxyAttributes.FIELD_STRIDE <= 1024);
        Assert.assertEquals(8, max); // mirrors GX_MAX_FIELDS in galaxy.glsl
    }

    @Test
    public void armSweepGrowsWithBaseAngle() {
        // baseAngle is in tenths of a degree and measured from the radial direction, so a small
        // value is a set of radial spokes. This is the trap the descriptor falls into: 12 reads like
        // "12 degrees" and is 1.2, i.e. a 3.6 deg sweep over a disk starting at minRadius = 0.05.
        var ds = new BillboardDataset();
        ds.distribution = GalaxyAttributes.Distribution.SPIRAL;
        ds.minRadius = 0.05f;

        ds.baseAngle = 12f;
        Assert.assertEquals(3.6, ds.spiralArmSweepDegrees(), 0.2);

        ds.baseAngle = 600f;
        Assert.assertEquals(297.0, ds.spiralArmSweepDegrees(), 1.0);

        // Larger is always more wound, and never decreases.
        double previous = -1;
        for (float a = 0; a <= 850; a += 50) {
            ds.baseAngle = a;
            double sweep = ds.spiralArmSweepDegrees();
            Assert.assertTrue(sweep >= previous);
            previous = sweep;
        }

        // A 4-arm spiral that visibly curves is the 400-700 band the generator recommends.
        ds.baseAngle = 500f;
        Assert.assertTrue(ds.spiralArmSweepDegrees() > 150.0);

        // Non-spirals have no sweep to report.
        ds.distribution = GalaxyAttributes.Distribution.SPHERE;
        Assert.assertEquals(0.0, ds.spiralArmSweepDegrees(), 0.0);
    }

    @Test
    public void logarithmicSpiralIsItsOwnDistribution() {
        // This predicate decides which knobs the procedural galaxy window shows, so SPIRAL and
        // SPIRAL_LOG must not answer for each other.
        Assert.assertTrue(GalaxyAttributes.Distribution.SPIRAL.isDensityWaveSpiral());
        Assert.assertFalse(GalaxyAttributes.Distribution.SPIRAL.isLogarithmicSpiral());
        Assert.assertTrue(GalaxyAttributes.Distribution.SPIRAL_LOG.isLogarithmicSpiral());
        Assert.assertFalse(GalaxyAttributes.Distribution.SPIRAL_LOG.isDensityWaveSpiral());
        Assert.assertTrue(GalaxyAttributes.Distribution.SPIRAL.isAnySpiral());
        Assert.assertTrue(GalaxyAttributes.Distribution.SPIRAL_LOG.isAnySpiral());
        Assert.assertFalse(GalaxyAttributes.Distribution.DISK.isAnySpiral());
    }

    @Test
    public void distributioOrdinalsMatchTheShader() {
        // The GX_* constants in galaxy.glsl depend on these ordinals.
        Assert.assertEquals(0, GalaxyAttributes.Distribution.SPHERE.ordinal());
        Assert.assertEquals(2, GalaxyAttributes.Distribution.SPIRAL.ordinal());
        Assert.assertEquals(3, GalaxyAttributes.Distribution.SPIRAL_LOG.ordinal());
        Assert.assertEquals(5, GalaxyAttributes.Distribution.ELLIPSOID.ordinal());
        Assert.assertEquals(0, GalaxyAttributes.HeightProfile.CONSTANT.ordinal());
        Assert.assertEquals(4, GalaxyAttributes.HeightProfile.SMOOTH_DEC.ordinal());
        // And the GX_CHANNEL_* constants depend on these.
        Assert.assertEquals(0, GalaxyAttributes.ChannelType.DUST.ordinal());
        Assert.assertEquals(1, GalaxyAttributes.ChannelType.BULGE.ordinal());
        Assert.assertEquals(3, GalaxyAttributes.ChannelType.GAS.ordinal());
    }
}
