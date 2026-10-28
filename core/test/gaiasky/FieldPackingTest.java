/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.scene.record.BillboardDataset;
import org.junit.Assert;
import org.junit.Test;

/**
 * {@link BillboardDataset#packFields(float[], int)} fills the flat uniform that
 * {@code gxUnpack()} in {@code assets/shader/lib/galaxy.glsl} reads back. The two have no compile-time
 * link, so the layout is pinned here: an offset that moves on one side and not the other would
 * silently shuffle every field after it.
 */
public class FieldPackingTest {

    @Test
    public void packMatchesTheShaderLayout() {
        var ds = new BillboardDataset();
        ds.distribution = BillboardDataset.Distribution.SPIRAL;
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
        ds.heightProfile = BillboardDataset.HeightProfile.SMOOTH_INC;
        ds.colorNoise = 0.08f;
        ds.intensity = 0.7f;
        ds.baseColors = new float[]{1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f};

        float[] packed = new float[BillboardDataset.MAX_FIELDS * BillboardDataset.FIELD_STRIDE];
        ds.packFields(packed, BillboardDataset.FIELD_STRIDE);

        int o = BillboardDataset.FIELD_STRIDE;
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
        // Nothing outside this field's window was touched.
        Assert.assertEquals(0f, packed[o + BillboardDataset.FIELD_STRIDE - 1 + 1], 0f);
        Assert.assertEquals(0f, packed[0], 0f);
    }

    @Test
    public void distributioOrdinalsMatchTheShader() {
        // The GX_* constants in galaxy.glsl depend on these ordinals.
        Assert.assertEquals(0, BillboardDataset.Distribution.SPHERE.ordinal());
        Assert.assertEquals(2, BillboardDataset.Distribution.SPIRAL.ordinal());
        Assert.assertEquals(3, BillboardDataset.Distribution.SPIRAL_LOG.ordinal());
        Assert.assertEquals(5, BillboardDataset.Distribution.ELLIPSOID.ordinal());
        Assert.assertEquals(0, BillboardDataset.HeightProfile.CONSTANT.ordinal());
        Assert.assertEquals(4, BillboardDataset.HeightProfile.SMOOTH_DEC.ordinal());
    }
}
