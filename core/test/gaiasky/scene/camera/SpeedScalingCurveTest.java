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
 * reference up to 50 AU, sub-linear from there out to the local group, and linear again beyond it,
 * so that leaving the solar system does not multiply the top speed by the same factor as the
 * distance, but the extragalactic regime stays crossable.
 */
public class SpeedScalingCurveTest {

    private static final double AU = 1.495978707e11;
    private static final double PC = 3.0856775814913673e16;
    private static final double DIST_SMOOTH_UP = 5000.0 * Constants.MPC_TO_U;
    /** The knees of the curve, as the implementation defines them. */
    private static final double KNEE_PC = 50.0 * AU / PC;
    private static final double FAR_LINEAR_KNEE_PC = 1.0e7;
    private static final double FAR_EXPONENT = 0.94;

    private static double at(double pc) {
        return NaturalCamera.distanceToSpeedScale(pc * PC * Constants.M_TO_U, DIST_SMOOTH_UP);
    }

    private static double linear(double pc) {
        // 2e16 at DIST_SMOOTH_UP, the linear branch of the curve.
        return 2.0e16 * (pc * PC * Constants.M_TO_U) / DIST_SMOOTH_UP;
    }

    @Test
    public void theSolarSystemIsUnchanged() {
        // Up to the knee the curve must be exactly the linear one, to the last bit.
        for (double au : new double[]{0.001, 0.1, 0.5, 1.0, 10.0, 49.0}) {
            double d = au * AU * Constants.M_TO_U;
            Assert.assertEquals(linear(au * AU / PC), NaturalCamera.distanceToSpeedScale(d, DIST_SMOOTH_UP),
                                1e-12 * linear(au * AU / PC));
        }
    }

    @Test
    public void theCurveIsContinuous() {
        for (double knee : new double[]{KNEE_PC, FAR_LINEAR_KNEE_PC}) {
            double at = at(knee);
            double in = at(knee * (1 - 1e-9));
            double out = at(knee * (1 + 1e-9));
            Assert.assertEquals(at, in, 1e-6 * at);
            Assert.assertEquals(at, out, 1e-6 * at);
        }
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
        // Past the knee the curve sits below the linear one, and further below the further out.
        for (double pc : new double[]{1, 1e3, 1e6}) {
            double curve = at(pc);
            Assert.assertTrue("not slower than linear at " + pc + " pc", curve < linear(pc));
            // Growing as d^0.94, so it loses ground against the linear branch as it goes.
            Assert.assertTrue(curve / linear(pc) < at(1) / linear(1) + 1e-9);
        }
    }

    @Test
    public void theExtragalacticRegimeIsLinearAgain() {
        // Past the ramp, doubling the distance doubles the speed: constant crossing time again, and
        // the curve grows in a straight line instead of flattening out.
        for (double pc : new double[]{2e8, 5e8, 1e9, 2e9}) {
            Assert.assertEquals(2.0, at(2 * pc) / at(pc), 1e-9);
        }
        // Below the second knee it does not: the branch has the exponent.
        Assert.assertEquals(Math.pow(2, FAR_EXPONENT), at(2 * FAR_LINEAR_KNEE_PC / 100) / at(FAR_LINEAR_KNEE_PC / 100), 1e-9);

        // ... and the exponential branch alone would be far slower out there, otherwise the change
        // is a no-op. At 1 Gpc the camera has to be at least twice as fast as it was.
        double powerOnly = at(FAR_LINEAR_KNEE_PC) * Math.pow(1e9 / FAR_LINEAR_KNEE_PC, FAR_EXPONENT);
        Assert.assertTrue("the linear branch adds nothing at 1 Gpc", at(1e9) > 2.0 * powerOnly);
    }

    @Test
    public void theRampIsSmooth() {
        // It starts on the exponent branch at the knee and ends on the linear one a decade later,
        // without a step in between.
        Assert.assertEquals(at(FAR_LINEAR_KNEE_PC * (1 + 1e-9)) / at(FAR_LINEAR_KNEE_PC), 1.0, 1e-6);
        for (double pc = FAR_LINEAR_KNEE_PC; pc < 10 * FAR_LINEAR_KNEE_PC; pc *= 1.01) {
            double power = at(FAR_LINEAR_KNEE_PC) * Math.pow(pc / FAR_LINEAR_KNEE_PC, FAR_EXPONENT);
            double v = at(pc);
            // Never below the exponent branch, never above the linear one.
            Assert.assertTrue("below the power branch at " + pc, v >= power * (1 - 1e-9));
            Assert.assertTrue("above the linear branch at " + pc, v <= linear(pc) * (1 + 1e-9));
        }
    }

    @Test
    public void theCurveIsScaleInvariant() {
        // Whoever changes DISTANCE_SCALE_FACTOR, the same physical distance has to give the same
        // physical speed.
        double mToU = Constants.M_TO_U;
        try {
            for (double pc : new double[]{10, 1e6, 1e9}) {
                double d = pc * PC * mToU;
                double reference = NaturalCamera.distanceToSpeedScale(d, DIST_SMOOTH_UP);
                for (double scale : new double[]{1e2, 1e4}) {
                    Constants.M_TO_U = mToU * scale;
                    double scaled = NaturalCamera.distanceToSpeedScale(d * scale, DIST_SMOOTH_UP * scale);
                    Assert.assertEquals(reference, scaled, 1e-9 * reference);
                }
                Constants.M_TO_U = mToU;
            }
        } finally {
            Constants.M_TO_U = mToU;
        }
    }

    @Test
    public void theNoReferenceSentinelIsScaleFree() {
        // The sentinel is compared against distances in internal units, so a finite one means a
        // different physical distance at every distance scale factor: 1e20 units sat at 3.2 Gpc with
        // the default factor but at 3.2 Mpc with 1e6, below the saturation distance of the curve and
        // inside the range of real measurements. Everything past the local group then read as "no
        // reference", the smoothed distance stopped moving and the camera crawled.
        Assert.assertTrue(Double.isInfinite(NaturalCamera.NO_CLOSEST_BODY_DISTANCE));
        // And it must stay past the saturation distance, whatever the scale factor is.
        for (double scale : new double[]{1, 1e4, 1e6}) {
            Constants.M_TO_U *= scale;
            NaturalCamera.closestBodyDistance(Double.NaN, 0, false);
            Assert.assertTrue(NaturalCamera.NO_CLOSEST_BODY_DISTANCE > 5000.0 * Constants.MPC_TO_U);
            Constants.M_TO_U /= scale;
        }
    }

    @Test
    public void theSurfaceSpeedIsAWalkingPaceAtEveryScale() {
        // One metre above a surface the curve gives ten metres per second, in internal units, so the
        // value has to follow the distance scale factor. Baking it in at class load (the camera is
        // built before the factor is initialised) or scaling it twice both break this.
        double mToU = Constants.M_TO_U;
        try {
            for (double scale : new double[]{1, 1e4, 1e6}) {
                Constants.M_TO_U = mToU * scale;
                double atOneMetre = NaturalCamera.altitudeToSpeedScale(Constants.M_TO_U);
                Assert.assertEquals(10 * Constants.M_TO_U, atOneMetre, 1e-9 * 10 * Constants.M_TO_U);
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
