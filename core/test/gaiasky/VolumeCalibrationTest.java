/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.render.gdx.graphics.VolumeCalibration;
import org.junit.Assert;
import org.junit.Test;

/**
 * Pins the arithmetic documented in {@link VolumeCalibration}. The constants in it are measured from
 * two different places - the metadata files of the Milky Way volume, and the texture bytes
 * themselves - and the class documents how to go from the first to the second. If either moves
 * without the other, a procedural galaxy stops matching the Milky Way and nothing at compile time
 * would notice.
 */
public class VolumeCalibrationTest {

    /**
     * The metadata of {@code gal-med/galaxy_emissive_metadata.txt}: grid 399x40x400, voxel size
     * 0.003425 box units, mean ray integral 0.00027606 in box units.
     */
    private static final int METADATA_THIN_VOXELS = 40;
    private static final double METADATA_VOXEL_SIZE = 0.003425;
    private static final double METADATA_EMISSIVE_MEAN_RAY_INTEGRAL = 0.00027606;
    private static final double METADATA_OCCLUSIVE_MEAN_RAY_INTEGRAL = 0.00000265;

    @Test
    public void metadataMatchesTheMeasuredTexture() {
        // The metadata integral is in box units: one voxel contributes its luma times its own length
        // along the ray. In UVW the box is one unit thick, so the two differ by exactly that length.
        double thinExtent = METADATA_THIN_VOXELS * METADATA_VOXEL_SIZE;
        Assert.assertEquals(VolumeCalibration.MW_EMISSIVE_MEAN_LUMA,
                            METADATA_EMISSIVE_MEAN_RAY_INTEGRAL / thinExtent, 5e-6);
        Assert.assertEquals(VolumeCalibration.MW_OCCLUSIVE_MEAN_LUMA,
                            METADATA_OCCLUSIVE_MEAN_RAY_INTEGRAL / thinExtent, 5e-8);
    }

    @Test
    public void targetsArePerUnitIntensity() {
        Assert.assertEquals(VolumeCalibration.MW_EMISSIVE_MEAN_LUMA / VolumeCalibration.MW_GAS_INTENSITY,
                            VolumeCalibration.EMISSIVE_TARGET_MEAN_LUMA, 1e-12);
        Assert.assertEquals(VolumeCalibration.MW_OCCLUSIVE_MEAN_LUMA / VolumeCalibration.MW_DUST_INTENSITY,
                            VolumeCalibration.OCCLUSIVE_TARGET_MEAN_LUMA, 1e-12);
        // Calibrating per unit intensity is only useful if intensity actually scales the result.
        Assert.assertTrue(VolumeCalibration.EMISSIVE_TARGET_MEAN_LUMA > VolumeCalibration.MW_EMISSIVE_MEAN_LUMA);
        Assert.assertTrue(VolumeCalibration.OCCLUSIVE_TARGET_MEAN_LUMA > VolumeCalibration.MW_OCCLUSIVE_MEAN_LUMA);
    }

    @Test
    public void occlusiveSlotIsAnchoredOnItsPeak() {
        Assert.assertEquals(VolumeCalibration.MW_OCCLUSIVE_PEAK / VolumeCalibration.MW_DUST_INTENSITY,
                            VolumeCalibration.OCCLUSIVE_TARGET_PEAK, 1e-12);
        // At the Milky Way's own dust intensity the stored peak is the Milky Way's own peak.
        Assert.assertEquals(VolumeCalibration.MW_OCCLUSIVE_PEAK,
                            VolumeCalibration.OCCLUSIVE_TARGET_PEAK * VolumeCalibration.MW_DUST_INTENSITY, 1e-12);
        // The Milky Way's dust intensity is 0.06, so a field at unit intensity is about 17 times its
        // dust and the target peak lands just below a full byte.
        Assert.assertTrue(VolumeCalibration.OCCLUSIVE_TARGET_PEAK > 0.9);
        Assert.assertTrue(VolumeCalibration.OCCLUSIVE_TARGET_PEAK <= 1.0);
    }

    @Test
    public void dustDiagnosticIsAPeakToMeanContrast() {
        Assert.assertEquals(VolumeCalibration.MW_OCCLUSIVE_MEAN_LUMA / VolumeCalibration.MW_OCCLUSIVE_PEAK,
                            VolumeCalibration.MW_OCCLUSIVE_MEAN_PEAK_RATIO, 1e-12);
        // The Milky Way's dust is sparse lanes: 0.3% of the voxels are lit, and the mean is four
        // orders of magnitude below the peak. A smooth field of the same mean would be far denser.
        Assert.assertTrue(VolumeCalibration.MW_OCCLUSIVE_MEAN_PEAK_RATIO < 1e-3);
    }
}
