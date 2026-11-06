/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.GalaxyAttributes;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * The arm modulation of the field model is a Gaussian of the distance to the arm centreline, and the
 * centreline is the analytic log spiral of the field's pitch. Two properties follow, and both are
 * checked here because both have broken before: a lane has exactly one ridge, and that ridge is
 * where the spiral says it is.
 */
public class ArmCentrelineTest {

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
        var field = new GalaxyAttributes(dust);
        field.intensity = 1f;
        return GalaxyShape.from(VolumeLayout.packFields(List.of(field), 1), 0);
    }

    /** Angular position of the arm centreline at a radius, without warp. **/
    private static float theta(GalaxyShape s,
                               float r) {
        return GalaxyDensityField.pitchB(s.baseAngle)
                * (float) Math.log(Math.max(r / (0.1f * s.baseRadius), 1e-4f));
    }

    /** Samples the arm modulation over one arm period and returns the azimuth of the ridge. **/
    private static float ridge(GalaxyShape s,
                               float r) {
        float period = (float) (2 * Math.PI / s.numArms);
        float start = theta(s, r) - 0.5f * period;
        int steps = 4000;
        float best = -1f, bestPhi = 0f;
        for (int i = 0; i < steps; i++) {
            float phi = start + i * period / steps;
            float v = GalaxyDensityField.armFactor(s, r, phi, 0f);
            if (v > best) {
                best = v;
                bestPhi = phi;
            }
        }
        return bestPhi;
    }

    /** Number of local maxima of the arm modulation over one arm period. **/
    private static int ridges(GalaxyShape s,
                              float r) {
        float period = (float) (2 * Math.PI / s.numArms);
        float start = theta(s, r) - 0.5f * period;
        int steps = 4000;
        int count = 0;
        float prev = GalaxyDensityField.armFactor(s, r, start, 0f);
        float curr = GalaxyDensityField.armFactor(s, r, start + period / steps, 0f);
        for (int i = 2; i <= steps; i++) {
            float next = GalaxyDensityField.armFactor(s, r, start + i * period / steps, 0f);
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
        for (float r = 0.2f; r <= 0.9f; r += 0.1f) {
            Assert.assertEquals("one ridge at r = " + r, 1, ridges(s, r));
        }
    }

    @Test
    public void theRidgeSitsOnTheSpiral() {
        var s = dust();
        for (float r = 0.2f; r <= 0.9f; r += 0.1f) {
            Assert.assertEquals("ridge at r = " + r, theta(s, r), ridge(s, r), 1e-3f);
        }
    }

    @Test
    public void theWarpBendsTheRidgeByAFractionOfThePeriod() {
        // The warp is a phase displacement in fractions of the arm period, so the ridge moves by
        // that fraction of it and by nothing else.
        var s = dust();
        float r = 0.6f;
        float period = (float) (2 * Math.PI / s.numArms);
        for (float warp : new float[]{-0.3f, 0.15f, 0.4f}) {
            float expected = theta(s, r) + warp * period;
            float best = -1f, bestPhi = 0f;
            float start = expected - 0.5f * period;
            for (int i = 0; i < 4000; i++) {
                float phi = start + i * period / 4000f;
                float v = GalaxyDensityField.armFactor(s, r, phi, warp);
                if (v > best) {
                    best = v;
                    bestPhi = phi;
                }
            }
            Assert.assertEquals("warp " + warp, expected, bestPhi, 1e-3f);
        }
    }
}
