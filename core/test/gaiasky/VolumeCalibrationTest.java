/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.render.gdx.graphics.VolumeCalibration;
import gaiasky.scene.record.GalaxyAttributes;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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

    private static GalaxyAttributes field(float intensity, float fraction) {
        var f = new GalaxyAttributes();
        f.intensity = intensity;
        f.occlusionThreshold = fraction;
        return f;
    }

    @Test
    public void occlusionThresholdIsTheIntensityWeightedSumOfTheFractions() {
        // One field: the slot threshold is that field's own fraction of its own peak, so the
        // fraction, and not the intensity, is what decides where the dust starts to occlude.
        var lanes = field(0.65f, 0.02f);
        float threshold = VolumeCalibration.occlusiveThreshold(List.of(lanes));
        Assert.assertEquals(VolumeCalibration.OCCLUSIVE_TARGET_PEAK * 0.65 * 0.02, threshold, 1e-7);
        // Below the stored peak, i.e. the field does occlude somewhere.
        Assert.assertTrue(threshold < VolumeCalibration.OCCLUSIVE_TARGET_PEAK * lanes.intensity);
        // The fraction is recovered whatever the intensity: scaling the field scales both sides.
        var brighter = field(1.3f, 0.02f);
        Assert.assertEquals(0.02,
                            VolumeCalibration.occlusiveThreshold(List.of(brighter))
                                    / (VolumeCalibration.OCCLUSIVE_TARGET_PEAK * brighter.intensity),
                            1e-6);

        // Nothing asking to occlude leaves the slot switched off; the raymarcher reads 0 that way.
        Assert.assertEquals(0f, VolumeCalibration.occlusiveThreshold(List.of(new GalaxyAttributes())), 0f);
        Assert.assertEquals(0f, VolumeCalibration.occlusiveThreshold(List.of()), 0f);
        Assert.assertEquals(0f, VolumeCalibration.occlusiveThreshold(null), 0f);

        // Fields add up, weighted by intensity, and the result is clamped to the stored range.
        Assert.assertEquals(VolumeCalibration.OCCLUSIVE_TARGET_PEAK * (0.65 * 0.02 + 0.02 * 0.71),
                            VolumeCalibration.occlusiveThreshold(List.of(field(0.65f, 0.02f), field(0.02f, 0.71f))),
                            1e-7);
        Assert.assertEquals(1f, VolumeCalibration.occlusiveThreshold(List.of(field(1f, 1f), field(1f, 1f))), 0f);
    }

    @Test
    public void materialComponentTakesTheDerivedThresholdWhenTheSceneSaysNothing() throws Exception {
        // Loaded without initialising: MaterialComponent's static initialiser needs a live GaiaSky.
        Class<?> material = Class.forName("gaiasky.scene.record.MaterialComponent",
                                          false,
                                          VolumeCalibrationTest.class.getClassLoader());
        Assert.assertNotNull("missing MaterialComponent.setVolumeDepthThresholdDerived(int, float)",
                             material.getDeclaredMethod("setVolumeDepthThresholdDerived", int.class, float.class));
    }
}
