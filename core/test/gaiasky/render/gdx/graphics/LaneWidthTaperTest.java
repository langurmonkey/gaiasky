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
 * The width of a lane along the arm.
 * <p>
 * The property that matters is not the width profile as a formula but that the profile is visible:
 * the voxels are 8-bit alpha normalised by the field peak and the dust envelope falls off steeply,
 * so a widening that happens where the field is under one step changes nothing on screen. These
 * check both: the profile itself, and that at the outer end of the dust it puts real stored density
 * on the screen.
 */
public class LaneWidthTaperTest {

    /** Narrow arms, the case where the widening has the most room: six radial spokes. **/
    private static GalaxyAttributes spokes(float taper) {
        var f = new GalaxyAttributes();
        f.type = ChannelType.DUST;
        f.distribution = Distribution.SPIRAL;
        f.baseRadius = 1f;
        f.minRadius = 0.05f;
        f.numArms = 6;
        f.armSigma = 0.08f;
        f.baseAngle = 0f;
        f.heightScale = 0.02f;
        f.intensity = 1f;
        f.laneWidthTaper = taper;
        return f;
    }

    private static GalaxyShape shape(GalaxyAttributes f) {
        return GalaxyShape.from(VolumeLayout.packFields(List.of(f), 1), 0);
    }

    /** Half-width at half maximum of the arm factor at a radius, in galaxy space. **/
    private static float halfWidth(GalaxyShape s,
                                   float r) {
        float b = GalaxyDensityField.pitchB(s.baseAngle);
        float phi = b * (float) Math.log(r / (0.1f * s.baseRadius));
        float q1 = (float) Math.sqrt(1.0 + b * b);
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

    /** Arc width of a lane at a radius, in voxels, where the stored alpha is above a level. **/
    private static float visibleVoxels(GalaxyShape s,
                                       float rn,
                                       float codes) {
        var sample = new GalaxyDensityField.Sample();
        float voxel = 2f / 316f;
        float peak = 0f;
        int g = 240;
        for (int i = 0; i < g; i++) {
            for (int j = 0; j < g; j++) {
                GalaxyDensityField.sample(s, -1f + 2f * i / (g - 1f), 0f, -1f + 2f * j / (g - 1f), voxel, sample);
                peak = Math.max(peak, sample.density);
            }
        }
        float period = (float) (2 * Math.PI / s.numArms);
        int n = 3000, above = 0;
        for (int i = 0; i < n; i++) {
            float phi = -period / 2 + period * i / n;
            GalaxyDensityField.sample(s, rn * (float) Math.cos(phi), 0f, rn * (float) Math.sin(phi), voxel, sample);
            if (255f * sample.density / peak > codes) {
                above++;
            }
        }
        return above * period * rn / n / voxel;
    }

    @Test
    public void zeroKeepsTheWidthAndTheEnvelope() {
        var s = shape(spokes(0f));
        Assert.assertEquals(halfWidth(s, 0.3f), halfWidth(s, 0.9f), 1e-3f);
        for (float r = 0f; r <= 1f; r += 0.1f) {
            Assert.assertEquals(1f, GalaxyDensityField.armWidthMultiplier(s, r), 0f);
        }
        Assert.assertEquals("the envelope has to be the plain one",
                            GalaxyDensityField.DISK_SCALE, GalaxyDensityField.dustDiskScale(s), 0f);
    }

    @Test
    public void positiveThinsTheCentreAndWidensTheOuterDust() {
        var s = shape(spokes(1f));
        float plain = GalaxyDensityField.armWidthMultiplier(shape(spokes(0f)), 0.7f);
        // Thin at the centre, at the clamp, and monotonically wider outwards.
        Assert.assertEquals(GalaxyDensityField.ARM_WIDTH_MIN,
                            GalaxyDensityField.armWidthMultiplier(s, 0f), 0f);
        float prev = 0f;
        for (float r = 0f; r <= 1f; r += 0.02f) {
            float m = GalaxyDensityField.armWidthMultiplier(s, r);
            Assert.assertTrue("monotone at r = " + r, m >= prev);
            prev = m;
        }
        // The widening happens inside the dust's own radial span, not past it.
        Assert.assertTrue("still nominal at the start of the span",
                          GalaxyDensityField.armWidthMultiplier(s, GalaxyDensityField.LANE_TAPER_INNER) < 0.5f);
        Assert.assertTrue("saturated by the end of it",
                          GalaxyDensityField.armWidthMultiplier(s, GalaxyDensityField.LANE_TAPER_OUTER)
                                  == GalaxyDensityField.armWidthMultiplier(s, 1f));
        // Measured, not just computed: at 0.7 the lane is several times the plain one.
        Assert.assertTrue("the lane has to be much wider at 0.7",
                          halfWidth(s, 0.7f) > 4f * halfWidth(shape(spokes(0f)), 0.7f) * plain);
    }

    @Test
    public void negativeIsTheMirrorImage() {
        var wide = shape(spokes(-1f));
        var narrow = shape(spokes(1f));
        // Over the span where neither end is clamped - outside it they hit different limits, the
        // narrow end at the ARM_WIDTH_MIN floor and the wide one at the disk cap.
        for (float r = 0.3f; r <= 0.55f; r += 0.05f) {
            Assert.assertEquals("at r = " + r, 1f,
                                GalaxyDensityField.armWidthMultiplier(wide, r)
                                        * GalaxyDensityField.armWidthMultiplier(narrow, r), 1e-4f);
        }
        Assert.assertTrue("wide at the centre and thin outside",
                          halfWidth(wide, 0.2f) > 2f * halfWidth(wide, 0.8f));
        // It widens the part of the disk that is bright anyway, so it needs no envelope stretch.
        Assert.assertEquals(GalaxyDensityField.DISK_SCALE, GalaxyDensityField.dustDiskScale(wide), 0f);
    }

    @Test
    public void theEnvelopeFollowsAPositiveTaper() {
        Assert.assertEquals((1f + GalaxyDensityField.DUST_TAPER_EXTENT) * GalaxyDensityField.DISK_SCALE,
                            GalaxyDensityField.dustDiskScale(shape(spokes(1f))), 1e-6f);
        Assert.assertEquals((1f + 0.5f * GalaxyDensityField.DUST_TAPER_EXTENT) * GalaxyDensityField.DISK_SCALE,
                            GalaxyDensityField.dustDiskScale(shape(spokes(0.5f))), 1e-6f);
    }

    @Test
    public void theWidenedOuterLaneIsActuallyOnScreen() {
        // The property that failed before: geometry alone is not enough, the widened lane has to be
        // above the quantisation floor at the radius where it is widened.
        var plain = shape(spokes(0f));
        var wide = shape(spokes(1f));
        float plainWidth = visibleVoxels(plain, 0.7f, 8f);
        float wideWidth = visibleVoxels(wide, 0.7f, 8f);
        Assert.assertTrue("the plain field is a hairline out there: " + plainWidth, plainWidth < 3f);
        Assert.assertTrue("the tapered one has to be visibly thick: " + plainWidth + " -> " + wideWidth,
                          wideWidth > 6f);
    }

    @Test
    public void theWidthStaysInsideWhatTheGridCanCarry() {
        // A lane may not become wider than a fifth of the disk, and a field with wide arms keeps its
        // nominal width instead of being narrowed by the cap.
        for (float armSigma : new float[]{0.05f, 0.08f, 0.3f, 0.6f, 1.2f}) {
            var f = spokes(1f);
            f.armSigma = armSigma;
            var s = shape(f);
            for (float r = 0f; r <= 1.2f; r += 0.05f) {
                float m = GalaxyDensityField.armWidthMultiplier(s, r);
                Assert.assertTrue("m = " + m + " at r = " + r + ", armSigma " + armSigma,
                                  m >= GalaxyDensityField.ARM_WIDTH_MIN && m <= GalaxyDensityField.ARM_WIDTH_MAX);
                // sigma = armSigma * ARM_WIDTH * m <= a fifth of the disk, unless the nominal width
                // is already past that, in which case the taper leaves it alone.
                float sigma = armSigma * GalaxyDensityField.ARM_WIDTH * m;
                float nominal = armSigma * GalaxyDensityField.ARM_WIDTH;
                Assert.assertTrue("lane wider than the disk at r = " + r + ", armSigma " + armSigma,
                                  sigma <= Math.max(nominal, 0.2f) + 1e-6f);
            }
            // Never narrowed below the nominal width, whatever the arms.
            Assert.assertTrue("nominal width kept", GalaxyDensityField.armWidthMultiplier(s, 1f) >= 1f);
        }
    }

    @Test
    public void theGasIsUntouched() {
        var plain = spokes(0f);
        plain.type = ChannelType.GAS;
        var tapered = spokes(1f);
        tapered.type = ChannelType.GAS;
        var a = shape(plain);
        var b = shape(tapered);
        for (float r = 0.3f; r <= 0.95f; r += 0.1f) {
            Assert.assertEquals("gas width at r = " + r, 1f,
                                GalaxyDensityField.armWidthMultiplier(b, r), 0f);
            Assert.assertEquals("gas lane at r = " + r, halfWidth(a, r), halfWidth(b, r), 0f);
        }
        Assert.assertEquals(GalaxyDensityField.DISK_SCALE, GalaxyDensityField.dustDiskScale(b), 0f);
    }
}
