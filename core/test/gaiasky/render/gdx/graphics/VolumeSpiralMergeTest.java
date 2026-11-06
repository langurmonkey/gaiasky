/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.scene.record.GalaxyAttributes.ChannelType;
import gaiasky.scene.record.GalaxyAttributes.Distribution;
import gaiasky.scene.record.GalaxyGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * The two spirals are one curve in the volume pipeline and two entries in the particle one.
 * <p>
 * The field model draws both with {@code gxArmFactor} - a logarithmic spiral of the field's pitch -
 * so {@code SPIRAL} and {@code SPIRAL_LOG} describe the same density; only the particle generator
 * tells them apart, and only to scale a dust sprite. {@code Distribution#toVolumeDistribution} folds
 * the second onto the first wherever a field is packed, and the merge must not leak into the
 * billboard half of the same draw.
 */
public class VolumeSpiralMergeTest {

    private static GalaxyAttributes dust(Distribution distribution) {
        var f = new GalaxyAttributes();
        f.type = ChannelType.DUST;
        f.distribution = distribution;
        f.baseRadius = 1f;
        f.minRadius = 0.1f;
        f.numArms = 2;
        f.armSigma = 0.6f;
        f.baseAngle = 500f;
        f.heightScale = 0.02f;
        f.intensity = 1f;
        return f;
    }

    @Test
    public void bothSpiralsPackAsOne() {
        var plain = VolumeLayout.packFields(List.of(dust(Distribution.SPIRAL)), 1);
        var log = VolumeLayout.packFields(List.of(dust(Distribution.SPIRAL_LOG)), 1);

        Assert.assertEquals(GalaxyShape.SPIRAL, Math.round(log[0]));
        Assert.assertEquals("the volume has one spiral, so the packed fields are the same",
                            plain[0], log[0], 0f);
    }

    @Test
    public void theOtherDistributionsAreUntouched() {
        // Only the duplicate is folded: everything else keeps its own id.
        for (var d : Distribution.values()) {
            if (d.isLogarithmicSpiral()) {
                continue;
            }
            var packed = VolumeLayout.packFields(List.of(dust(d)), 1);
            Assert.assertEquals(d.name(), d.ordinal(), Math.round(packed[0]));
        }
    }

    @Test
    public void bothNamesSampleTheSameDensity() {
        var sample = new GalaxyDensityField.Sample();
        float[] a = sampleAll(GalaxyShape.from(VolumeLayout.packFields(List.of(dust(Distribution.SPIRAL)), 1), 0), sample);
        float[] b = sampleAll(GalaxyShape.from(VolumeLayout.packFields(List.of(dust(Distribution.SPIRAL_LOG)), 1), 0), sample);

        Assert.assertArrayEquals(a, b, 0f);
        float peak = 0f;
        for (float v : a) {
            peak = Math.max(peak, v);
        }
        Assert.assertTrue("the equality above has to be about a field that exists", peak > 0f);
    }

    @Test
    public void theParticleHalfKeepsTheTwoApart() {
        var gen = new GalaxyGenerator();
        boolean sawLogSpiralInParticles = false;
        // Scattered seeds, not 0..n: the first nextFloat() of java.util.Random is biased high for
        // consecutive small seeds, and the genus a spiral gets is the first draw of the generator.
        for (long i = 0; i < 200 && !sawLogSpiralInParticles; i++) {
            long seed = i * 7919L + 12345L;
            var spec = gen.generateGalaxySpec(GalaxyGenerator.GalaxyMorphology.Sb, seed);
            for (var field : spec.volumeFields) {
                Assert.assertFalse("volume field " + field.type + " keeps the second spiral name",
                                   field.distribution.isLogarithmicSpiral());
            }
            for (var dataset : spec.half) {
                sawLogSpiralInParticles |= dataset.distribution.isLogarithmicSpiral();
            }
        }
        Assert.assertTrue("the draw has to still produce the second spiral for the billboards",
                          sawLogSpiralInParticles);
    }

    /** Mid-plane density of a field over a fixed grid, row by row. **/
    private static float[] sampleAll(GalaxyShape s,
                                     GalaxyDensityField.Sample sample) {
        int n = 24;
        float[] out = new float[n * n];
        for (int j = 0; j < n; j++) {
            float z = -1f + 2f * j / (n - 1);
            for (int i = 0; i < n; i++) {
                float x = -1f + 2f * i / (n - 1);
                GalaxyDensityField.sample(s, x, 0f, z, 0.05f, sample);
                out[j * n + i] = sample.density;
            }
        }
        return out;
    }
}
