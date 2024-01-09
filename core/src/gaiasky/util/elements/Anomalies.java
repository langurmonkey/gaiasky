/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.elements;

import gaiasky.util.math.MathUtilsDouble;

public class Anomalies {
    /** The Constant TWO_PI. */
    public static final double TWO_PI = 2 * Math.PI;

    /**
     * Reduce the given angle to the interval <i>[0 2pi[</i>.
     *
     * @param x Angle (units: <code>rad</code>).
     * @return the angle in the range of <i>0</i> through <i>2pi</i>.
     */
    public static double reduce(double x) {
        return MathUtilsDouble.normalizeAngle(x, Math.PI);
    }

    /**
     * Compute eccentric anomaly from eccentricity and true anomaly.
     *
     * @param v
     *            eccentric anomaly.
     * @param ecc
     *            eccentricity.
     *
     * @return the eccentric anomaly.
     *
     * @throws IllegalArgumentException
     *             if <code>ecc</code> is not in the interval <i>[0 1[</i>.
     */
    public static double true2ecc(final double v, final double ecc) {
        if ((ecc < 0) || (ecc >= 1)) {
            throw new IllegalArgumentException(
                    "Eccentricity out of range (" + ecc + ")");
        }

        final double x = reduce(v);

        // Recovery of the reduction.
        final double dx = v - x;

        return true2eccConstrained(v, ecc) + dx;
    }

    /**
     * Compute eccentric anomaly from eccentricity and true anomaly which is
     * assumed to be between <i>0</i> and <i>2pi</i>.<br> Note that, in this
     * version, no consistency checks are performed on the input.
     *
     * @param v
     *            eccentric anomaly.
     * @param ecc
     *            eccentricity.
     *
     * @return the eccentric anomaly in the range of <i>0</i> through <i>2pi</i>.
     */
    public static double true2eccConstrained(final double v,
                                             final double ecc) {
        double eA = 2 * Math.atan(
                Math.sqrt((1 - ecc) / (1 + ecc)) * Math.tan(v / 2));

        if (eA < 0) {
            eA += TWO_PI;
        }

        return eA;
    }

    /**


     /**
     * Compute eccentric anomaly from eccentricity and mean anomaly (solving
     * Kepler Equation). It will use the values for error tolerance and number
     * of iterations currently set in the {@code kepler} object.
     *
     * @param kepler
     *            Solver.
     * @param mA
     *            Mean anomaly.
     * @param ecc
     *            eccentricity.
     *
     * @return the eccentric anomaly.
     *
     * @throws IllegalArgumentException
     *             if <code>ecc</code> is not in the interval <i>[0 1[</i>.
     */
    public static double mean2ecc(final KeplerSolver kepler,
                                  final double mA,
                                  final double ecc) {
        return kepler.eccAnom(mA, ecc);
    }
}
