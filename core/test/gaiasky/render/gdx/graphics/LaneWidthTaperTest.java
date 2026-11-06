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
 * The width of a lane along the arm, and the envelope that has to move with it. A positive taper
 * thins the centre and thickens the outskirts, which is only worth anything if the outskirts have
 * dust: on the plain envelope the lanes are under one 8-bit step past half the radius, so the taper
 * would have nothing to widen there. The two move together, and both are checked here.
 */
public class LaneWidthTaperTest {

    private static GalaxyAttributes field(ChannelType type,
                                          float taper) {
        var f = new GalaxyAttributes();
        f.type = type;
        f.distribution = Distribution.SPIRAL;
        f.baseRadius = 1f;
        f.minRadius = 0.05f;
        f.numArms = 2;
        f.armSigma = 0.6f;
        f.baseAngle = 500f;
        f.heightScale = 0.02f;
        f.intensity = 1f;
        f.laneWidthTaper = taper;
        return f;
    }

    private static GalaxyShape shape(ChannelType type,
                                     float taper) {
        return GalaxyShape.from(VolumeLayout.packFields(List.of(field(type, taper)), 1), 0);
    }

    /** Half-width at half maximum of the lane at a radius, in galaxy space. **/
    private static float halfWidth(GalaxyShape s,
                                   float r) {
        float b = GalaxyDensityField.pitchB(s.baseAngle);
        float phi = b * (float) Math.log(r / (0.1f * s.baseRadius));
        float q1 = (float) Math.sqrt(1.0 + b * b);
        // Walk along the arc at a fixed radius: the angular offset from the centreline is linear in
        // the step, so the perpendicular distance is too.
        int n = 20000;
        // Below half an arm period: past that the sample reaches the next lane, not this one.
        float limit = 1.5f;
        for (int i = 1; i <= n; i++) {
            float phiAt = phi + limit * i / n;
            if (GalaxyDensityField.armFactor(s, r, phiAt, 0f) < 0.5f) {
                return limit * i / n * r / q1;
            }
        }
        Assert.fail("the lane has no edge at r = " + r);
        return 0f;
    }

    /** Stored alpha of the densest point at a radius, in 1/255 of the field peak. **/
    private static float ridgeCodes(GalaxyShape s,
                                    float rn) {
        var sample = new GalaxyDensityField.Sample();
        float peak = 0f;
        int m = 160;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                GalaxyDensityField.sample(s, -1f + 2f * i / (m - 1f), 0f, -1f + 2f * j / (m - 1f), 0.007f, sample);
                peak = Math.max(peak, sample.density);
            }
        }
        float period = (float) (2 * Math.PI / s.numArms);
        float ridge = 0f;
        for (int i = 0; i < 720; i++) {
            float phi = -period / 2 + period * i / 720f;
            GalaxyDensityField.sample(s, rn * (float) Math.cos(phi), 0f, rn * (float) Math.sin(phi), 0.007f, sample);
            ridge = Math.max(ridge, sample.density);
        }
        return 255f * ridge / peak;
    }

    @Test
    public void zeroKeepsTheWidthAndTheEnvelope() {
        var s = shape(ChannelType.DUST, 0f);
        Assert.assertEquals(halfWidth(s, 0.3f), halfWidth(s, 0.9f), 1e-3f);
        for (float r = 0f; r <= 1f; r += 0.1f) {
            Assert.assertEquals(1f, GalaxyDensityField.armWidthMultiplier(s, r), 0f);
        }
        Assert.assertEquals("the envelope has to be the plain one",
                            GalaxyDensityField.DISK_SCALE, GalaxyDensityField.dustDiskScale(s), 0f);
    }

    @Test
    public void positiveThinsTheCentreAndWidensTheRim() {
        var s = shape(ChannelType.DUST, 1f);
        Assert.assertEquals("centre, clamped", GalaxyDensityField.ARM_WIDTH_MIN,
                            GalaxyDensityField.armWidthMultiplier(s, 0f), 0f);
        Assert.assertEquals(1f, GalaxyDensityField.armWidthMultiplier(s, 0.5f), 1e-6f);
        Assert.assertEquals(2.8284f, GalaxyDensityField.armWidthMultiplier(s, 1f), 1e-3f);
        // Monotone outwards, and wider at the rim than the plain lane by a wide margin.
        float prev = 0f;
        for (float r = 0f; r <= 1f; r += 0.05f) {
            float m = GalaxyDensityField.armWidthMultiplier(s, r);
            Assert.assertTrue("monotone at r = " + r, m >= prev);
            prev = m;
        }
        // Measured at 0.7, past the crossover: at exactly half the radius the multiplier is 1.
        Assert.assertTrue("the outer lane has to be much wider than the plain one",
                          halfWidth(s, 0.7f) > 1.5f * halfWidth(shape(ChannelType.DUST, 0f), 0.7f));
        Assert.assertTrue("and the centre narrower",
                          halfWidth(s, 0.2f) < 0.9f * halfWidth(shape(ChannelType.DUST, 0f), 0.2f));
    }

    @Test
    public void negativeIsTheMirrorImage() {
        var wide = shape(ChannelType.DUST, -1f);
        var narrow = shape(ChannelType.DUST, 1f);
        // From 0.1: at the very centre the multiplier is clamped, so the two stop being reciprocal.
        for (float r = 0.1f; r <= 1f; r += 0.1f) {
            Assert.assertEquals("at r = " + r, 1f,
                                GalaxyDensityField.armWidthMultiplier(wide, r)
                                        * GalaxyDensityField.armWidthMultiplier(narrow, r), 1e-5f);
        }
        // The multiplier at 0.35 is 1.37, and the half-width scales with it.
        Assert.assertTrue("the inner lane has to be wider than the plain one",
                          halfWidth(wide, 0.35f) > 1.3f * halfWidth(shape(ChannelType.DUST, 0f), 0.35f));
        // It thickens the part of the disk that is bright anyway, so it needs no envelope stretch.
        Assert.assertEquals(GalaxyDensityField.DISK_SCALE, GalaxyDensityField.dustDiskScale(wide), 0f);
    }

    @Test
    public void theEnvelopeFollowsAPositiveTaper() {
        // Doubling the scale radius at 1 is what puts the outer lanes above the alpha floor.
        Assert.assertEquals(2f * GalaxyDensityField.DISK_SCALE,
                            GalaxyDensityField.dustDiskScale(shape(ChannelType.DUST, 1f)), 1e-6f);
        Assert.assertEquals(1.5f * GalaxyDensityField.DISK_SCALE,
                            GalaxyDensityField.dustDiskScale(shape(ChannelType.DUST, 0.5f)), 1e-6f);
        // ... and at a radius the plain field has already lost, the tapered one still shows a lane.
        float plain = ridgeCodes(shape(ChannelType.DUST, 0f), 0.6f);
        float tapered = ridgeCodes(shape(ChannelType.DUST, 1f), 0.6f);
        Assert.assertTrue("the plain field is under a few codes out there: " + plain, plain < 6f);
        Assert.assertTrue("the tapered one has to be well above it: " + plain + " -> " + tapered,
                          tapered > 3f * plain && tapered > 4f);
    }

    @Test
    public void theGasIsUntouched() {
        // The gas is a smooth exponential along the whole disk; its width is not a lane's.
        var plain = shape(ChannelType.GAS, 0f);
        var tapered = shape(ChannelType.GAS, 1f);
        for (float r = 0.3f; r <= 0.95f; r += 0.1f) {
            Assert.assertEquals("gas width at r = " + r, 1f,
                                GalaxyDensityField.armWidthMultiplier(tapered, r), 0f);
            Assert.assertEquals("gas lane at r = " + r,
                                halfWidth(plain, r), halfWidth(tapered, r), 0f);
        }
        Assert.assertEquals(GalaxyDensityField.DISK_SCALE, GalaxyDensityField.dustDiskScale(tapered), 0f);
    }

    @Test
    public void theWidthStaysInsideWhatTheGridCanCarry() {
        var s = shape(ChannelType.DUST, 1f);
        for (float r = 0f; r <= 1.2f; r += 0.05f) {
            float m = GalaxyDensityField.armWidthMultiplier(s, r);
            Assert.assertTrue("width multiplier " + m + " at r = " + r,
                              m >= GalaxyDensityField.ARM_WIDTH_MIN && m <= GalaxyDensityField.ARM_WIDTH_MAX);
        }
        Assert.assertEquals(GalaxyDensityField.armWidthMultiplier(s, 1f),
                            GalaxyDensityField.armWidthMultiplier(s, 5f), 0f);
    }
}
