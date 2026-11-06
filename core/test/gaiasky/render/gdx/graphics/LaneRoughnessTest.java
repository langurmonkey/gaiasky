/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.scene.record.GalaxyGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * The ragged dust edge. {@code laneRough} displaces the arm centreline across itself, and the
 * displacement has to go on the <em>signed</em> angular distance: applied to its magnitude, the two
 * flanks of a lane move in opposite senses, a negative displacement puts the ridge at a constant
 * distance on either side of the centreline (one lane drawn as two), and the field loses most of its
 * peak density on the way. Both are checked here.
 */
public class LaneRoughnessTest {

    private static GalaxyShape dust() {
        var dust = new BillboardDataset();
        dust.type = GalaxyAttributes.ChannelType.DUST;
        dust.distribution = GalaxyAttributes.Distribution.SPIRAL;
        dust.baseRadius = 1.0f;
        dust.minRadius = 0.1f;
        dust.numArms = 2;
        dust.armSigma = 0.6f;
        dust.baseAngle = 400f;
        dust.heightScale = 0.02f;
        dust.laneRough = 0f;
        var field = new GalaxyAttributes(dust);
        field.intensity = 1f;
        return GalaxyShape.from(VolumeLayout.packFields(List.of(field), 1), 0);
    }

    /** Angular position of the arm centreline at a radius, without warp. **/
    private static float theta(GalaxyShape s, float r) {
        return GalaxyDensityField.pitchB(s.baseAngle)
                * (float) Math.log(Math.max(r / (0.1f * s.baseRadius), 1e-4f));
    }

    /** Samples the arm modulation over one arm period and returns the azimuth of the ridge. **/
    private static float ridge(GalaxyShape s, float r, float rough) {
        float period = (float) (2 * Math.PI / s.numArms);
        float start = theta(s, r) - 0.5f * period;
        int steps = 4000;
        float best = -1f, bestPhi = 0f;
        for (int i = 0; i < steps; i++) {
            float phi = start + i * period / steps;
            float v = GalaxyDensityField.armFactor(s, r, phi, 0f, rough);
            if (v > best) {
                best = v;
                bestPhi = phi;
            }
        }
        return bestPhi;
    }

    /** Number of local maxima of the arm modulation over one arm period. **/
    private static int ridges(GalaxyShape s, float r, float rough) {
        float period = (float) (2 * Math.PI / s.numArms);
        float start = theta(s, r) - 0.5f * period;
        int steps = 4000;
        int count = 0;
        float prev = GalaxyDensityField.armFactor(s, r, start, 0f, rough);
        float curr = GalaxyDensityField.armFactor(s, r, start + period / steps, 0f, rough);
        for (int i = 2; i <= steps; i++) {
            float next = GalaxyDensityField.armFactor(s, r, start + i * period / steps, 0f, rough);
            if (curr > prev && curr >= next) {
                count++;
            }
            prev = curr;
            curr = next;
        }
        return count;
    }

    @Test
    public void oneLaneStaysOneLane() {
        var s = dust();
        // A displacement in either direction shifts the ridge; it must not duplicate it.
        Assert.assertEquals(1, ridges(s, 0.6f, 0f));
        Assert.assertEquals(1, ridges(s, 0.6f, 1f));
        Assert.assertEquals(1, ridges(s, 0.6f, -1f));
        Assert.assertEquals(1, ridges(s, 0.6f, 3f));
        Assert.assertEquals(1, ridges(s, 0.6f, -3f));
    }

    @Test
    public void theRidgeMovesByTheDisplacement() {
        var s = dust();
        float r = 0.6f;
        float q1 = (float) Math.sqrt(1.0 + Math.pow(GalaxyDensityField.pitchB(s.baseAngle), 2));
        float sigma = s.armSigma * GalaxyDensityField.ARM_WIDTH * s.baseRadius;
        // The ridge sits where d * r / q1 + rough * sigma == 0.
        for (float rough : new float[]{-2f, -1f, 0.5f, 1f, 2f}) {
            float expected = theta(s, r) - rough * sigma * q1 / r;
            Assert.assertEquals("rough " + rough, expected, ridge(s, r, rough), 1e-3f);
        }
    }

    @Test
    public void theRoughEdgeKeepsThePeakDensity() {
        // Same field, mid-plane slice: the amplitude of the displacement can only move the lane, so a
        // rough field has to keep the peak of the smooth one. With the displacement on the magnitude
        // it drops by more than an order of magnitude, and every point of the lane gets dimmer.
        var gen = new GalaxyGenerator();
        var spec = gen.generateGalaxySpec(GalaxyGenerator.GalaxyMorphology.Sb, 87224L);
        GalaxyAttributes dust = null;
        for (var f : spec.volumeFields) {
            if (f.type == GalaxyAttributes.ChannelType.DUST) {
                dust = f;
            }
        }
        Assert.assertNotNull("the morphology has to have dust", dust);

        float smooth = peak(dust, 0f);
        Assert.assertTrue(smooth > 0f);
        for (float rough : new float[]{0.5f, 1f, 2f}) {
            Assert.assertTrue("laneRough " + rough + " lost the peak: " + peak(dust, rough) + " of " + smooth,
                              peak(dust, rough) > 0.5f * smooth);
        }
    }

    /** Highest mid-plane density of the field with the given roughness. **/
    private static float peak(GalaxyAttributes field, float rough) {
        var copy = new GalaxyAttributes(field);
        copy.laneRough = rough;
        var s = GalaxyShape.from(VolumeLayout.packFields(List.of(copy), 1), 0);
        var sample = new GalaxyDensityField.Sample();
        int n = 200;
        float half = s.baseRadius * 1.05f;
        float cell = 2f * half / (316f * 1.05f);
        float peak = 0f;
        for (int j = 0; j < n; j++) {
            float pz = -half + j * 2 * half / n;
            for (int i = 0; i < n; i++) {
                float px = -half + i * 2 * half / n;
                GalaxyDensityField.sample(s, px, 0f, pz, cell, sample);
                peak = Math.max(peak, sample.density);
            }
        }
        return peak;
    }
}
