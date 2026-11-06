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
import gaiasky.scene.record.GalaxyAttributes.HeightProfile;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * The field model is a switch: each distribution reads a different subset of the parameters. The
 * procedural volume window builds only the controls of the subset the chosen distribution reads, so
 * this pins which parameter is live where. A parameter that changes the density of a distribution it
 * is not supposed to reach is a knob shown in the wrong pane; one that changes nothing is a knob that
 * looks broken.
 * <p>
 * The density is summed over a mid-plane slice of the field's box. Both halves of every comparison
 * sample the same points, so only a real change in the field can move the sum.
 */
public class DistributionControlsTest {

    /** A neutral field of the given distribution: no noise, no warp, one arm modulation. **/
    private static GalaxyAttributes field(Distribution distribution) {
        var f = new GalaxyAttributes();
        f.type = ChannelType.GAS;
        f.distribution = distribution;
        f.baseRadius = 1f;
        f.minRadius = 0.05f;
        f.numArms = 2;
        f.armSigma = 0.6f;
        f.baseAngle = 500f;
        f.aspect = 1f;
        f.eccentricity = new float[]{0f, 0f};
        f.warpStrength = 0f;
        f.heightScale = 0.05f;
        f.heightProfile = HeightProfile.CONSTANT;
        f.colorNoise = 0f;
        f.intensity = 1f;
        f.noiseMask = 0f;
        f.laneWarp = 0f;
        f.laneNoiseScale = 1f;
        f.laneRough = 0f;
        return f;
    }

    /** Total density over a slice of the field's box, which every distribution shares. **/
    private static double total(GalaxyAttributes a) {
        var s = GalaxyShape.from(VolumeLayout.packFields(List.of(a), 1), 0);
        var sample = new GalaxyDensityField.Sample();
        int nx = 33, ny = 9;
        double sum = 0;
        for (int i = 0; i < nx; i++) {
            float x = -1.1f + 2.2f * i / (nx - 1);
            for (int k = 0; k < nx; k++) {
                float z = -1.1f + 2.2f * k / (nx - 1);
                for (int j = 0; j < ny; j++) {
                    float y = -0.06f + 0.12f * j / (ny - 1);
                    GalaxyDensityField.sample(s, x, y, z, 0.02f, sample);
                    sum += sample.density;
                }
            }
        }
        return sum;
    }

    @Test
    public void flatMeansTheDistributionsWithAVerticalProfile() {
        // gxIsFlat in galaxy.glsl, which GalaxyDensityField mirrors, includes the bar.
        Assert.assertTrue(Distribution.DISK.isFlat());
        Assert.assertTrue(Distribution.DISK_GAUSS.isFlat());
        Assert.assertTrue(Distribution.SPIRAL.isFlat());
        Assert.assertTrue(Distribution.SPIRAL_LOG.isFlat());
        Assert.assertTrue(Distribution.BAR.isFlat());
        Assert.assertFalse(Distribution.SPHERE.isFlat());
        Assert.assertFalse(Distribution.SPHERE_GAUSS.isFlat());
        Assert.assertFalse(Distribution.ELLIPSOID.isFlat());
        Assert.assertFalse(Distribution.CONE.isFlat());
        Assert.assertFalse(Distribution.IRREGULAR.isFlat());
    }

    @Test
    public void flatteningOnlyReachesTheEllipsoid() {
        // The radii of a spiral are spherical distances that the arm modulation then shapes; its
        // flattening values are stored and packed but no branch reads them.
        for (var d : Distribution.values()) {
            var zero = field(d);
            var flattened = field(d);
            flattened.eccentricity = new float[]{0.8f, 0.6f};
            if (d.isEllipsoid()) {
                Assert.assertTrue(d + " has to read its flattening",
                                  Math.abs(total(zero) - total(flattened)) > 1e-6);
            } else {
                Assert.assertEquals(d + " reads a flattening it has no use for",
                                    total(zero), total(flattened), 1e-9);
            }
        }
    }

    @Test
    public void aspectOnlyReachesTheBar() {
        for (var d : Distribution.values()) {
            var square = field(d);
            var stretched = field(d);
            stretched.aspect = 2.5f;
            if (d.isBar()) {
                Assert.assertTrue("the bar has to read its aspect ratio",
                                  Math.abs(total(square) - total(stretched)) > 1e-6);
            } else {
                Assert.assertEquals(d + " reads an aspect ratio it has no use for",
                                    total(square), total(stretched), 1e-9);
            }
        }
    }

    @Test
    public void verticalStructureOnlyReachesTheFlatDistributions() {
        for (var d : Distribution.values()) {
            var thin = field(d);
            var thick = field(d);
            thick.heightScale = 0.25f;
            var flat = field(d);
            var warped = field(d);
            warped.warpStrength = 0.2f;
            if (d.isFlat()) {
                Assert.assertTrue(d + " has to read its height scale",
                                  Math.abs(total(thin) - total(thick)) > 1e-6);
                Assert.assertTrue(d + " has to read its warp",
                                  Math.abs(total(flat) - total(warped)) > 1e-6);
            } else {
                Assert.assertEquals(d + " reads a height scale it has no use for",
                                    total(thin), total(thick), 1e-9);
                Assert.assertEquals(d + " reads a warp it has no use for",
                                    total(flat), total(warped), 1e-9);
            }
        }
    }

    @Test
    public void armsOnlyReachTheSpirals() {
        for (var d : Distribution.values()) {
            var two = field(d);
            var five = new GalaxyAttributes(two);
            five.numArms = 5;
            var wide = field(d);
            var narrow = new GalaxyAttributes(wide);
            narrow.armSigma = 0.1f;
            if (d.isAnySpiral()) {
                Assert.assertTrue(d + " has to read its arm count", Math.abs(total(two) - total(five)) > 1e-6);
                Assert.assertTrue(d + " has to read its arm sigma", Math.abs(total(wide) - total(narrow)) > 1e-6);
            } else {
                Assert.assertEquals(d + " reads an arm count it has no use for",
                                    total(two), total(five), 1e-9);
                Assert.assertEquals(d + " reads an arm sigma it has no use for",
                                    total(wide), total(narrow), 1e-9);
            }
        }
    }

    @Test
    public void theBaseAngleIsAPitchForSpiralsAndAHalfAngleForTheCone() {
        for (var d : Distribution.values()) {
            var wide = field(d);
            var tight = new GalaxyAttributes(wide);
            tight.baseAngle = 300f;
            boolean live = d.isAnySpiral() || d == Distribution.CONE;
            if (live) {
                Assert.assertTrue(d + " has to read its base angle",
                                  Math.abs(total(wide) - total(tight)) > 1e-6);
            } else {
                Assert.assertEquals(d + " reads a base angle it has no use for",
                                    total(wide), total(tight), 1e-9);
            }
        }
    }

    @Test
    public void laneKnobsOnlyReachTheDustOfASpiral() {
        for (var d : Distribution.values()) {
            for (var channel : new ChannelType[]{ChannelType.GAS, ChannelType.DUST}) {
                var plain = field(d);
                plain.type = channel;
                var warped = new GalaxyAttributes(plain);
                warped.laneWarp = 0.8f;
                var rough = new GalaxyAttributes(plain);
                rough.laneRough = 2f;

                boolean spiral = d.isAnySpiral();
                // The arm warp displaces the centreline of both channel types; the ragged edge is
                // only applied to the dust, which is the only field with an occluding edge.
                Assert.assertEquals(d + "/" + channel + " reads an arm warp it has no use for",
                                    spiral ? 1 : 0, Math.abs(total(plain) - total(warped)) > 1e-6 ? 1 : 0);
                boolean roughLive = spiral && channel == ChannelType.DUST;
                Assert.assertEquals(d + "/" + channel + " reads a lane roughness it has no use for",
                                    roughLive ? 1 : 0, Math.abs(total(plain) - total(rough)) > 1e-6 ? 1 : 0);
            }
        }
    }
}
