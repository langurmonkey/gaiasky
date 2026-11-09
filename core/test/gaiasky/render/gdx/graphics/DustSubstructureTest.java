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
 * The dust must not share the gas's clump realisation: the marcher emits the gas and absorbs the
 * dust at the same sample, so a shared realisation puts the dust exactly where the gas is brightest
 * and the two cancel.
 */
public class DustSubstructureTest {

    private static final long SEED = 87224L;

    private static GalaxyAttributes field(ChannelType type,
                                          Distribution distribution) {
        var f = new GalaxyAttributes();
        f.type = type;
        f.distribution = distribution;
        f.baseRadius = 1f;
        f.minRadius = 0.1f;
        f.numArms = 2;
        f.armSigma = 0.38f;
        f.baseAngle = 651.35f;
        f.heightScale = 0.02f;
        f.intensity = 1f;
        return f;
    }

    private static GalaxyShape shape(GalaxyAttributes f) {
        return GalaxyShape.from(VolumeLayout.packFields(List.of(f), 1), 0);
    }

    /** Angular position of the arm centreline at a radius, without warp. **/
    private static float theta(GalaxyShape s,
                               float r) {
        return GalaxyDensityField.pitchB(s.baseAngle) * (float) Math.log(r / (0.1f * s.baseRadius));
    }

    /** Mean and standard deviation of a series. **/
    private static float[] stats(float[] v) {
        float mean = 0f;
        for (float x : v) {
            mean += x;
        }
        mean /= v.length;
        float var = 0f;
        for (float x : v) {
            var += (x - mean) * (x - mean);
        }
        return new float[]{mean, (float) Math.sqrt(var / v.length)};
    }

    /** Densities sampled across the lane at a radius, over the lane and its surroundings. **/
    private static float[] acrossLane(GalaxyShape s,
                                      float r) {
        var sample = new GalaxyDensityField.Sample();
        int n = 600;
        float[] v = new float[n];
        float t0 = theta(s, r);
        for (int i = 0; i < n; i++) {
            float d = -0.15f + 0.3f * i / (n - 1);
            float px = r * (float) Math.cos(t0) - d * (float) Math.sin(t0);
            float pz = r * (float) Math.sin(t0) + d * (float) Math.cos(t0);
            GalaxyDensityField.sample(s, px, 0f, pz, 0.005f, sample);
            v[i] = sample.density;
        }
        return v;
    }

    @Test
    public void theGasFieldIsUnchanged() {
        // The clump seed is a parameter with a default of zero, and the gas passes zero: the gas
        // field is bit-identical to what it was before the dust got its own realisation. These are
        // the densities the previous field produced at six points of a generated Sb galaxy.
        var spec = new GalaxyGenerator().generateGalaxySpec(GalaxyGenerator.GalaxyMorphology.Sb, SEED);
        GalaxyAttributes gas = null;
        for (var f : spec.volumeFields) {
            if (f.type == ChannelType.GAS) {
                gas = f;
            }
        }
        Assert.assertNotNull(gas);
        var s = shape(gas);
        var sample = new GalaxyDensityField.Sample();
        float[][] points = {{0.12f, 0.01f, 0.20f}, {0.35f, 0f, -0.18f}, {-0.42f, 0.02f, 0.31f},
                {0.05f, -0.01f, 0.55f}, {-0.21f, 0f, -0.44f}, {0.6f, 0.03f, 0.05f}};
        float[] expected = {0.013018824f, 0.08236768f, 0f, 7.723014e-4f, 0.03350771f, 0f};
        for (int i = 0; i < points.length; i++) {
            GalaxyDensityField.sample(s, points[i][0], points[i][1], points[i][2], 0.007f, sample);
            Assert.assertEquals("gas at point " + i, expected[i], sample.density, 0f);
        }
    }

    @Test
    public void theDustDrawsAnIndependentClumpRealisation() {
        // A dust field sharing the gas realisation sits where the gas is brightest, and the marcher
        // emits the gas and absorbs it at the same sample, so the two cancel. Correlated here means
        // shared; the two draws have to be independent but identical in distribution.
        int n = 0;
        float[] a = new float[8000], b = new float[8000];
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                for (int k = 0; k < 20; k++) {
                    float x = -0.8f + 1.6f * i / 19f, y = -0.1f + 0.2f * j / 19f, z = -0.8f + 1.6f * k / 19f;
                    a[n] = GalaxyDensityField.clumps(x, y, z, 1f, 0f);
                    b[n] = GalaxyDensityField.clumps(x, y, z, 1f, GalaxyDensityField.DUST_CLUMP_SEED);
                    n++;
                }
            }
        }
        float[] sa = stats(a), sb = stats(b);
        float cov = 0f;
        for (int i = 0; i < n; i++) {
            cov += (a[i] - sa[0]) * (b[i] - sb[0]);
        }
        float corr = cov / (n * sa[1] * sb[1]);
        Assert.assertEquals("the two draws have the same statistics", sa[0], sb[0], 0.05f);
        Assert.assertEquals("and the same spread", sa[1], sb[1], 0.05f);
        Assert.assertTrue("the dust must not clump with the gas, correlation " + corr, Math.abs(corr) < 0.1f);
    }

    @Test
    public void theDustKeepsALaneAcrossTheArm() {
        // Whatever is done to the dust, the densest point stays on the centreline and falls away
        // from it: a lane that loses its ridge is no longer a lane.
        var s = shape(field(ChannelType.DUST, Distribution.SPIRAL));
        float r = 0.5f;
        float[] v = acrossLane(s, r);
        int mid = v.length / 2;
        float ridge = Math.max(v[mid - 2], Math.max(v[mid - 1], Math.max(v[mid], v[mid + 1])));
        Assert.assertTrue("there has to be a lane at r = " + r, ridge > 0f);
        float quarter = 0.075f;  // Two arm sigmas out, where the Gaussian is down by e^-2.
        int offset = (int) (quarter / 0.3f * (v.length - 1));
        int left = mid - offset, right = mid + offset;
        Assert.assertTrue("the lane has to be a ridge: flank " + v[left] + ", ridge " + ridge,
                          v[left] < 0.5f * ridge);
        Assert.assertTrue("on both sides: flank " + v[right], v[right] < 0.5f * ridge);
    }
}
