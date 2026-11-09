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
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Structure inside the lanes.
 * <p>
 * The knob it replaces was thresholded, so its mean multiplier was a fraction of one and it deleted
 * the lane instead of texturing it. These check the properties that make the replacement usable:
 * both of its halves are zero-mean, what it varies is longer along the arm than across it, the ridge
 * survives, the gas is not touched, and the structure stays above the 8-bit floor.
 */
public class ArmStructureTest {

    private static final float CELL = 2f / 316f;

    private static GalaxyAttributes field(float structure,
                                          float armSigma,
                                          ChannelType type) {
        var f = new GalaxyAttributes();
        f.type = type;
        f.distribution = Distribution.SPIRAL;
        f.baseRadius = 1f;
        f.minRadius = 0f;
        f.numArms = 4;
        f.armSigma = armSigma;
        f.baseAngle = 0f;
        f.heightScale = 0.02f;
        f.intensity = 1f;
        f.armStructure = structure;
        return f;
    }

    private static GalaxyShape shape(GalaxyAttributes f) {
        return GalaxyShape.from(VolumeLayout.packFields(List.of(f), 1), 0);
    }

    /** Density ratios along a radial line through a lane centre (pitch 0, so the arms are spokes). **/
    private static float[] alongArm(GalaxyShape on,
                                    GalaxyShape off,
                                    int n) {
        var sample = new GalaxyDensityField.Sample();
        float[] v = new float[n];
        for (int i = 0; i < n; i++) {
            float r = 0.2f + 0.6f * i / (n - 1f);
            GalaxyDensityField.sample(on, r, 0f, 0f, CELL, sample);
            float a = sample.density;
            GalaxyDensityField.sample(off, r, 0f, 0f, CELL, sample);
            v[i] = a / Math.max(sample.density, 1e-9f);
        }
        return v;
    }

    /** Density ratios along a tangential line across one lane at a radius. **/
    private static float[] acrossLane(GalaxyShape on,
                                      GalaxyShape off,
                                      float r,
                                      int n) {
        var sample = new GalaxyDensityField.Sample();
        float[] v = new float[n];
        for (int i = 0; i < n; i++) {
            float d = -0.06f + 0.12f * i / (n - 1f);
            float phi = d / r;
            float px = r * (float) Math.cos(phi), pz = r * (float) Math.sin(phi);
            GalaxyDensityField.sample(on, px, 0f, pz, CELL, sample);
            float a = sample.density;
            GalaxyDensityField.sample(off, px, 0f, pz, CELL, sample);
            v[i] = a / Math.max(sample.density, 1e-9f);
        }
        return v;
    }

    private static float cv(float[] v) {
        double mean = 0;
        for (float x : v) {
            mean += x;
        }
        mean /= v.length;
        double q = 0;
        for (float x : v) {
            q += (x - mean) * (x - mean);
        }
        return (float) (Math.sqrt(q / v.length) / Math.max(mean, 1e-9));
    }

    /** Crossings of a series about its own mean. **/
    private static int crossings(float[] v) {
        double mean = 0;
        for (float x : v) {
            mean += x;
        }
        mean /= v.length;
        int c = 0;
        for (int i = 1; i < v.length; i++) {
            if ((v[i - 1] - mean) * (v[i] - mean) < 0) {
                c++;
            }
        }
        return c;
    }

    @Test
    public void zeroIsOffExactly() {
        // Both halves have to be the identity at zero, not merely small: the field it produces must
        // be the field of before the knob existed.
        var s = shape(field(0f, 0.1f, ChannelType.DUST));
        for (int i = 0; i < 200; i++) {
            float r = 0.1f + 0.8f * i / 199f;
            float phi = 13f * i;
            float px = r * (float) Math.cos(phi), pz = r * (float) Math.sin(phi);
            Assert.assertEquals("shift at r = " + r, 0f,
                                GalaxyDensityField.armStructureShift(s, px, 0f, pz, r, phi, 0f, CELL), 0f);
            Assert.assertEquals("texture at r = " + r, 1f,
                                GalaxyDensityField.armStructureTexture(s, 0f, r, phi, 0f, CELL), 0f);
        }
        // And the arm modulation still has its exact analytic ridge.
        Assert.assertEquals(1f, GalaxyDensityField.armFactor(s, 0.5f, 0f, 0f), 1e-6f);
    }

    @Test
    public void bothHalvesAreZeroMean() {
        // This is what the old knob got wrong: a thresholded field has a mean well below one, so
        // turning it up removed the lane. Averaged over the azimuth, neither half may shift the
        // density up or down.
        var s = shape(field(1f, 0.1f, ChannelType.DUST));
        double tex = 0, shift = 0;
        int n = 0;
        for (int i = 0; i < 360; i++) {
            float phi = (float) (2 * Math.PI * i / 360);
            for (float r = 0.25f; r <= 0.55f; r += 0.005f) {
                float px = r * (float) Math.cos(phi), pz = r * (float) Math.sin(phi);
                tex += GalaxyDensityField.armStructureTexture(s, 0f, r, phi, 0f, CELL);
                shift += GalaxyDensityField.armStructureShift(s, px, 0f, pz, r, phi, 0f, CELL);
                n++;
            }
        }
        Assert.assertEquals("the density modulation averages to one", 1f, (float) (tex / n), 0.05f);
        Assert.assertEquals("the shift averages to no displacement", 0f, (float) (shift / n), 0.1f);
    }

    @Test
    public void theFieldKeepsItsBrightness() {
        // Redistributed, not deleted. The old knob was left with 12% of the dust; this one must stay
        // in the same league as the plain field even on the widest lanes. It is not exactly one,
        // and it cannot be: the dust contrast raises the density to a power, so redistributing it
        // at fixed linear mean moves the stored mean by up to a third.
        var sample = new GalaxyDensityField.Sample();
        for (float armSigma : new float[]{0.08f, 0.1f, 0.3f}) {
            var on = shape(field(1f, armSigma, ChannelType.DUST));
            var off = shape(field(0f, armSigma, ChannelType.DUST));
            double a = 0, b = 0;
            int g = 220;
            for (int i = 0; i < g; i++) {
                for (int j = 0; j < g; j++) {
                    float px = -1f + 2f * i / (g - 1f), pz = -1f + 2f * j / (g - 1f);
                    GalaxyDensityField.sample(on, px, 0f, pz, CELL, sample);
                    a += sample.density;
                    GalaxyDensityField.sample(off, px, 0f, pz, CELL, sample);
                    b += sample.density;
                }
            }
            float ratio = (float) (a / b);
            Assert.assertTrue("armSigma " + armSigma + " keeps " + ratio + " of the density",
                              ratio > 0.6f && ratio < 1.6f);
        }
    }

    @Test
    public void theStructureIsLongerAlongTheArmThanAcrossIt() {
        // A lane with structure has to be striated along the flow, not pockmarked: the same unit of
        // path crosses more features sideways than forwards.
        var on = shape(field(1f, 0.1f, ChannelType.DUST));
        var off = shape(field(0f, 0.1f, ChannelType.DUST));
        int n = 500;
        float alongLen = 0.6f, acrossLen = 0.12f;
        float along = crossings(alongArm(on, off, n)) / alongLen;
        float across = crossings(acrossLane(on, off, 0.45f, n)) / acrossLen;
        Assert.assertTrue("crossings per unit length along " + along + ", across " + across,
                          across > 2f * along);
        // And it does vary in both directions, so it is not a uniform shift.
        Assert.assertTrue("no variation along the arm", cv(alongArm(on, off, n)) > 0.1f);
        Assert.assertTrue("no variation across the lane", cv(acrossLane(on, off, 0.45f, n)) > 0.3f);
    }

    @Test
    public void theLaneKeepsItsRidge() {
        // Structure must not dissolve the lane: the densest point of the cross-section stays a peak.
        var on = shape(field(1f, 0.1f, ChannelType.DUST));
        var off = shape(field(0f, 0.1f, ChannelType.DUST));
        var sample = new GalaxyDensityField.Sample();
        int n = 400;
        for (float r = 0.25f; r <= 0.55f; r += 0.05f) {
            float ridgeOn = 0f, ridgeOff = 0f;
            for (int i = 0; i < n; i++) {
                float d = -0.08f + 0.16f * i / (n - 1f);
                float phi = d / r;
                float px = r * (float) Math.cos(phi), pz = r * (float) Math.sin(phi);
                GalaxyDensityField.sample(on, px, 0f, pz, CELL, sample);
                ridgeOn = Math.max(ridgeOn, sample.density);
                GalaxyDensityField.sample(off, px, 0f, pz, CELL, sample);
                ridgeOff = Math.max(ridgeOff, sample.density);
            }
            Assert.assertTrue("the lane lost its ridge at r = " + r, ridgeOn > 0.3f * ridgeOff);
        }
    }

    @Test
    public void theGasIsUntouched() {
        // The gas is a smooth exponential along the whole disk; the structure is the dust's.
        var on = shape(field(1f, 0.1f, ChannelType.GAS));
        var off = shape(field(0f, 0.1f, ChannelType.GAS));
        var sample = new GalaxyDensityField.Sample();
        for (int i = 0; i < 50; i++) {
            float r = 0.1f + 0.8f * i / 49f;
            float phi = 2.3f * i;
            float px = r * (float) Math.cos(phi), pz = r * (float) Math.sin(phi);
            GalaxyDensityField.sample(on, px, 0.01f, pz, CELL, sample);
            float a = sample.density;
            GalaxyDensityField.sample(off, px, 0.01f, pz, CELL, sample);
            Assert.assertEquals("gas at r = " + r, sample.density, a, 0f);
        }
    }

    @Test
    public void theStructureSurvivesTheQuantisation() {
        // The whole point: what the knob varies has to be above one step of the 8-bit alpha, or it is
        // invisible no matter how strong it is.
        var on = shape(field(1f, 0.1f, ChannelType.DUST));
        var off = shape(field(0f, 0.1f, ChannelType.DUST));
        var sample = new GalaxyDensityField.Sample();
        float peak = 0f;
        int g = 240;
        for (int i = 0; i < g; i++) {
            for (int j = 0; j < g; j++) {
                GalaxyDensityField.sample(off, -1f + 2f * i / (g - 1f), 0f, -1f + 2f * j / (g - 1f), CELL, sample);
                peak = Math.max(peak, sample.density);
            }
        }
        int n = 300;
        float[] codes = new float[n];
        for (int i = 0; i < n; i++) {
            float r = 0.25f + 0.3f * i / (n - 1f);
            GalaxyDensityField.sample(on, r, 0f, 0f, CELL, sample);
            codes[i] = 255f * sample.density / peak;
        }
        float mean = 0, var = 0;
        for (float c : codes) {
            mean += c;
        }
        mean /= n;
        for (float c : codes) {
            var += (c - mean) * (c - mean);
        }
        Assert.assertTrue("the structure has to swing several codes, got " + Math.sqrt(var / n),
                          Math.sqrt(var / n) > 2f);
    }
}
