/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.camera;

import gaiasky.util.Constants;
import org.junit.Assert;
import org.junit.Test;

/**
 * The speed curve used by the camera in free mode. It is linear in the distance to the nearest
 * reference up to one astronomical unit and sub-linear beyond it, so that leaving the solar system
 * does not multiply the top speed by the same factor as the distance.
 */
public class SpeedScalingCurveTest {

    private static final double AU = 1.495978707e11;
    private static final double PC = 3.0856775814913673e16;
    private static final double DIST_SMOOTH_UP = 5000.0 * Constants.MPC_TO_U;

    private static double knee() {
        return AU * Constants.M_TO_U;
    }

    private static double linear(double distance) {
        // 2e16 at DIST_SMOOTH_UP, the linear branch of the curve.
        return 2.0e16 * distance / DIST_SMOOTH_UP;
    }

    @Test
    public void theSolarSystemIsUnchanged() {
        // Up to 1 AU the curve must be exactly the old linear one, to the last bit.
        for (double au : new double[]{0.001, 0.1, 0.5, 1.0}) {
            double d = au * AU * Constants.M_TO_U;
            Assert.assertEquals(linear(d), NaturalCamera.distanceToSpeedScale(d, DIST_SMOOTH_UP), 1e-12 * linear(d));
        }
    }

    @Test
    public void theCurveIsContinuous() {
        double k = knee();
        double at = NaturalCamera.distanceToSpeedScale(k, DIST_SMOOTH_UP);
        double in = NaturalCamera.distanceToSpeedScale(k * (1 - 1e-9), DIST_SMOOTH_UP);
        double out = NaturalCamera.distanceToSpeedScale(k * (1 + 1e-9), DIST_SMOOTH_UP);
        Assert.assertEquals(at, in, 1e-6 * at);
        Assert.assertEquals(at, out, 1e-6 * at);
    }

    @Test
    public void theCurveIsMonotonic() {
        double previous = 0;
        for (double d = 1e-3; d < DIST_SMOOTH_UP; d *= 3) {
            double v = NaturalCamera.distanceToSpeedScale(d, DIST_SMOOTH_UP);
            Assert.assertTrue("not monotonic at " + d, v >= previous);
            previous = v;
        }
    }

    @Test
    public void theInterstellarRegimeIsSlower() {
        // One parsec out the curve is ~6x slower than the linear one, i.e. the camera takes ~50 s
        // instead of ~8 s to cross the reference distance.
        double d = PC * Constants.M_TO_U;
        double linear = linear(d);
        double curve = NaturalCamera.distanceToSpeedScale(d, DIST_SMOOTH_UP);
        Assert.assertTrue(curve < linear / 4);
        Assert.assertTrue(curve > linear / 10);

        // ... and a kiloparsec out, much slower still.
        double kpc = 1e3 * PC * Constants.M_TO_U;
        Assert.assertTrue(NaturalCamera.distanceToSpeedScale(kpc, DIST_SMOOTH_UP) < linear(kpc) / 12);
    }

    @Test
    public void theCurveIsScaleInvariant() {
        // Whoever changes DISTANCE_SCALE_FACTOR, the same physical distance has to give the same
        // physical speed.
        double mToU = Constants.M_TO_U;
        try {
            double d = 10 * PC * mToU;
            double reference = NaturalCamera.distanceToSpeedScale(d, DIST_SMOOTH_UP);
            for (double scale : new double[]{1e2, 1e4}) {
                Constants.M_TO_U = mToU * scale;
                double scaled = NaturalCamera.distanceToSpeedScale(d * scale, DIST_SMOOTH_UP * scale);
                Assert.assertEquals(reference, scaled, 1e-9 * reference);
            }
        } finally {
            Constants.M_TO_U = mToU;
        }
    }

    @Test
    public void degenerateDistancesGiveNoSpeed() {
        Assert.assertEquals(0, NaturalCamera.distanceToSpeedScale(0, DIST_SMOOTH_UP), 0);
        Assert.assertEquals(0, NaturalCamera.distanceToSpeedScale(-1, DIST_SMOOTH_UP), 0);
        Assert.assertEquals(0, NaturalCamera.distanceToSpeedScale(Double.NaN, DIST_SMOOTH_UP), 0);
        Assert.assertEquals(0, NaturalCamera.distanceToSpeedScale(1, 0), 0);
    }
}
