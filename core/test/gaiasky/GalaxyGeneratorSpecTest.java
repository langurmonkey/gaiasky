/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.render.gdx.graphics.CPUGalaxyVolumeGenerator;
import gaiasky.render.gdx.graphics.VolumeCalibration;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.scene.record.GalaxyGenerator;
import gaiasky.scene.record.GalaxySpec;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static gaiasky.scene.record.GalaxyAttributes.ChannelType;

/**
 * The single draw that feeds both paths of a procedural galaxy: every morphology has to yield the
 * volume fields its billboards describe, and generating them must not disturb the billboards.
 */
public class GalaxyGeneratorSpecTest {

    private static final long SEED = 20261102L;

    private static GalaxySpec spec(GalaxyGenerator.GalaxyMorphology gm,
                                   long seed) {
        return new GalaxyGenerator().generateGalaxySpec(gm, seed);
    }

    private static List<GalaxyAttributes> ofType(List<GalaxyAttributes> fields,
                                                 ChannelType type) {
        List<GalaxyAttributes> out = new ArrayList<>();
        for (var f : fields) {
            if (f.type == type) {
                out.add(f);
            }
        }
        return out;
    }

    @Test
    public void everyMorphologyHasSomethingToEmit() {
        for (var gm : GalaxyGenerator.GalaxyMorphology.values()) {
            var spec = spec(gm, SEED);
            Assert.assertNotNull(gm + " produced no spec", spec);
            Assert.assertFalse(gm + " has no emissive volume field", spec.emissiveFields().isEmpty());
            Assert.assertTrue(gm + " has no fields at all", spec.hasVolume());
        }
    }

    @Test
    public void volumeFieldsMirrorTheBillboardHalf() {
        for (var gm : GalaxyGenerator.GalaxyMorphology.values()) {
            var spec = spec(gm, SEED);
            List<GalaxyAttributes> volumetric = new ArrayList<>();
            for (var ds : spec.half) {
                Assert.assertTrue(gm + " routes a non-volumetric channel to the half pass", ds.type.isVolumetric());
                volumetric.add(ds);
            }
            Assert.assertEquals(gm + " volume field count", volumetric.size(), spec.volumeFields.length);
            for (int i = 0; i < volumetric.size(); i++) {
                var from = volumetric.get(i);
                var to = spec.volumeFields[i];
                Assert.assertEquals(gm + " type", from.type, to.type);
                Assert.assertEquals(gm + " distribution", from.distribution, to.distribution);
                // The intensity is the one exception: a sprite alpha is not a density weight (see
                // volumeIntensityIsADensityWeightNotASpriteAlpha).
                Assert.assertNotEquals(gm + " kept the sprite intensity", from.intensity, to.intensity, 1e-9f);
                Assert.assertEquals(gm + " base radius", from.baseRadius, to.baseRadius, 1e-6f);
            }
        }
    }

    @Test
    public void volumeFieldsCarryNoSamplerParameters() {
        for (var gm : GalaxyGenerator.GalaxyMorphology.values()) {
            for (var field : spec(gm, SEED).volumeFields) {
                // A particle sampler parameter would mean the copy went through the subclass.
                Assert.assertFalse(gm + " field is a billboard dataset", field instanceof BillboardDataset);
            }
        }
    }

    @Test
    public void dustOccludesByDefault() {
        for (var gm : GalaxyGenerator.GalaxyMorphology.values()) {
            var spec = spec(gm, SEED);
            for (var dust : ofType(spec.occlusiveFields(), ChannelType.DUST)) {
                Assert.assertTrue(gm + " dust field has no occlusion threshold", dust.occlusionThreshold > 0f);
            }
            // Only dust occludes, and only dust may carry a threshold.
            for (var emissive : spec.emissiveFields()) {
                Assert.assertEquals(gm + " emissive field asks to occlude", 0f, emissive.occlusionThreshold, 0f);
            }
        }
    }

    @Test
    public void slotsFitTheShaderLimits() {
        for (var gm : GalaxyGenerator.GalaxyMorphology.values()) {
            var spec = spec(gm, SEED);
            Assert.assertTrue(gm + " emissive field count",
                              spec.emissiveFields().size() <= GalaxyAttributes.MAX_FIELDS);
            Assert.assertTrue(gm + " occlusive field count",
                              spec.occlusiveFields().size() <= GalaxyAttributes.MAX_FIELDS);
        }
    }

    @Test
    public void sameSeedYieldsTheSameBillboardsAsTheLegacyPath() {
        for (var gm : GalaxyGenerator.GalaxyMorphology.values()) {
            var generator = new GalaxyGenerator();
            var spec = generator.generateGalaxySpec(gm, SEED);
            var legacy = generator.generateGalaxy(gm, SEED);

            Assert.assertEquals(gm + " full count", spec.full.length, legacy.getFirst().length);
            Assert.assertEquals(gm + " half count", spec.half.length, legacy.getSecond().length);
            for (int i = 0; i < spec.full.length; i++) {
                Assert.assertEquals(gm + " full type " + i, spec.full[i].type, legacy.getFirst()[i].type);
                Assert.assertEquals(gm + " full count " + i, spec.full[i].particleCount, legacy.getFirst()[i].particleCount);
                Assert.assertArrayEquals(gm + " full colors " + i, spec.full[i].baseColors, legacy.getFirst()[i].baseColors, 0f);
            }
            for (int i = 0; i < spec.half.length; i++) {
                Assert.assertEquals(gm + " half type " + i, spec.half[i].type, legacy.getSecond()[i].type);
                Assert.assertEquals(gm + " half count " + i, spec.half[i].particleCount, legacy.getSecond()[i].particleCount);
                Assert.assertArrayEquals(gm + " half colors " + i, spec.half[i].baseColors, legacy.getSecond()[i].baseColors, 0f);
            }
        }
    }

    @Test
    public void volumeIntensityIsADensityWeightNotASpriteAlpha() {
        // The generator's per-channel intensities are sprite alphas: the gas cloud ships 0.011
        // because it is drawn with thousands of large sprites. Carried into a density field, that
        // quantises the whole gas channel to zero next to a bulge at ~1, and the galaxy renders
        // with everything except its gas. Every volumetric channel has to start from a unit weight.
        for (var gm : GalaxyGenerator.GalaxyMorphology.values()) {
            for (var field : spec(gm, SEED).volumeFields) {
                Assert.assertEquals(gm + " " + field.type + " kept its sprite intensity",
                                    GalaxyAttributes.initialVolumeIntensity(field.type), field.intensity, 0f);
            }
        }
    }

    @Test
    public void everyVolumeFieldStoresAVisibleAlpha() {
        // One slot of the previous test's kind: the numbers below are the ones the shader computes,
        // so a field below the threshold is a field that the 8-bit store rounds to black.
        for (var gm : new GalaxyGenerator.GalaxyMorphology[]{GalaxyGenerator.GalaxyMorphology.Sc,
                                                             GalaxyGenerator.GalaxyMorphology.Sb,
                                                             GalaxyGenerator.GalaxyMorphology.E5,
                                                             GalaxyGenerator.GalaxyMorphology.Im}) {
            var spec = spec(gm, SEED);

            var emissive = spec.emissiveFields();
            var grid = GalaxyAttributes.volumeGrid(emissive, 20_400, gaiasky.util.Settings.GraphicsQuality.LOW);
            var stats = new float[6 + GalaxyAttributes.MAX_FIELDS];
            CPUGalaxyVolumeGenerator.voxelise(emissive, grid, stats);
            // The emissive write pass divides by the mean luma at unit intensity and multiplies by
            // the target (galgen.volume.comp.glsl); the reference cancels out of the expression.
            float gain = (float) (VolumeCalibration.EMISSIVE_TARGET_MEAN_LUMA / Math.max(stats[3], 1e-9));
            for (int i = 0; i < emissive.size(); i++) {
                float stored = stats[6 + i] * emissive.get(i).intensity * gain;
                Assert.assertTrue(gm + " " + emissive.get(i).type + " stores alpha " + stored
                                          + ", which rounds to black", stored > 0.02f);
            }

            var occlusive = spec.occlusiveFields();
            if (!occlusive.isEmpty()) {
                for (var field : occlusive) {
                    float stored = (float) Math.min(1.0, VolumeCalibration.OCCLUSIVE_TARGET_PEAK * field.intensity);
                    Assert.assertTrue(gm + " " + field.type + " stores alpha " + stored,
                                      stored > 0.5f);
                }
            }
        }
    }

    @Test
    public void nullMorphologyIsRecordedInTheSpec() {
        var spec = spec(null, SEED);
        Assert.assertNotNull(spec.morphology);
        Assert.assertEquals(SEED, spec.seed);
    }
}
