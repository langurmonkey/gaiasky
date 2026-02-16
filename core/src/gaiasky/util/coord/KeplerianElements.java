/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

import static gaiasky.util.math.MathUtilsDouble.PI2;

/**
 * Contains utilities to convert cartesian positions to Keplerian elements and back.
 */
public class KeplerianElements {

    /**
     * This method automatically computes the standard gravitational
     * parameter (mu) of the orbit if the period and the semi-major axis
     * are set as mu=4 pi^2 a^3 / T^2.
     */
    public static double computeMu(boolean km3s2, double period, double semiMajorAxis) {
        if (period > 0 && semiMajorAxis > 0) {
            double a = semiMajorAxis; // In km.
            // Compute mu from period and semi-major axis.
            double T = period * Nature.D_TO_S; // d to s.
            var mu = (4.0 * FastMath.PI * FastMath.PI * a * a * a) / (T * T);
            if (!km3s2) {
                mu *= 1.0e9;
            }
            return mu;
        }
        return 0;
    }

    /**
     * Get the cartesian position vector for the given true anomaly (nu).
     *
     * @param out The vector to store the result.
     * @param nu  The true anomaly.
     */
    public static void keplerianToCartesianNu(Vector3D out,
                                              double nu,
                                              double i,
                                              double e,
                                              double ascendingNode,
                                              double argOfPericenter,
                                              double semiMajorAxis) {
        double inc = FastMath.toRadians(i);
        double ascNode = FastMath.toRadians(ascendingNode);
        double argP = FastMath.toRadians(argOfPericenter);

        // Distance
        double r = semiMajorAxis * (1 - e * e) / (1 + e * FastMath.cos(nu));
        // Perifocal coordinates
        double xPf = r * FastMath.cos(nu);
        double yPf = r * FastMath.sin(nu);

        // Rotation matrix values.
        double cosO = FastMath.cos(ascNode);
        double sinO = FastMath.sin(ascNode);
        double cosI = FastMath.cos(inc);
        double sinI = FastMath.sin(inc);
        double cosW = FastMath.cos(argP);
        double sinW = FastMath.sin(argP);

        // Direct rotation.
        double x = xPf * (cosO * cosW - sinO * sinW * cosI) + yPf * (-cosO * sinW - sinO * cosW * cosI);
        double y = xPf * (sinO * cosW + cosO * sinW * cosI) + yPf * (-sinO * sinW + cosO * cosW * cosI);
        double z = xPf * (sinW * sinI) + yPf * (cosW * sinI);

        // From regular XYZ to X'Y'Z' (Gaia Sky coordinates).
        out.set(y, z, x).scl(Constants.KM_TO_U);
    }

    /**
     * Get the cartesian position vector for the given delta time from epoch.
     *
     * @param out    The vector to store the result.
     * @param dtDays The Julian days from epoch.
     */
    public static void keplerianToCartesianTime(Vector3D out,
                                                double dtDays,
                                                double period,
                                                double i,
                                                double e,
                                                double ascendingNode,
                                                double argOfPericenter,
                                                double semiMajorAxis,
                                                double meanAnomaly) {

        double inc = FastMath.toRadians(i);
        double ascNode = FastMath.toRadians(ascendingNode);
        double argP = FastMath.toRadians(argOfPericenter);

        // Mean anomaly at time.
        double M = KeplerianElements.getMeanAnomalyAt(dtDays, period, meanAnomaly);
        // Solve Kepler's equations.
        double E = solveKepler(M, e);
        // True anomaly at target time.
        double nu = getTrueAnomaly(E, e);
        // Radius
        double r = semiMajorAxis * (1 - e * FastMath.cos(E));

        // Perifocal coordinates.
        double xPf = r * FastMath.cos(nu);
        double yPf = r * FastMath.sin(nu);

        // Uncomment for velocity vector.
        //double p = semiMajorAxis * (1 - e * e);
        //double vxPf = -FastMath.sqrt(mu / p) * FastMath.sin(nu);
        //double vyPf = FastMath.sqrt(mu / p) * (e + FastMath.cos(nu));
        //double vzPf = 0;

        // Rotation matrix values.
        double cosO = FastMath.cos(ascNode);
        double sinO = FastMath.sin(ascNode);
        double cosI = FastMath.cos(inc);
        double sinI = FastMath.sin(inc);
        double cosW = FastMath.cos(argP);
        double sinW = FastMath.sin(argP);

        // Direct rotation.
        double x = xPf * (cosO * cosW - sinO * sinW * cosI) + yPf * (-cosO * sinW - sinO * cosW * cosI);
        double y = xPf * (sinO * cosW + cosO * sinW * cosI) + yPf * (-sinO * sinW + cosO * cosW * cosI);
        double z = xPf * (sinW * sinI) + yPf * (cosW * sinI);

        // From regular XYZ to X'Y'Z' (Gaia Sky coordinates).
        out.set(y, z, x).scl(Constants.KM_TO_U);
    }

    private static double getMeanAnomalyAt(double dtDays, double period, double meanAnomaly) {
        // Mean orbital motion [rad/day]
        double n = PI2 / period;
        // Mean anomaly at epoch
        double M0 = FastMath.toRadians(meanAnomaly);
        // Mean anomaly at epoch + dt
        double M = (M0 + n * dtDays) % PI2;
        if (M < 0) M += PI2;
        return M;
    }

    /**
     * Gets the true anomaly for the given solution to Kepler's equation.
     *
     * @param E Solution to Kepler's equation.
     *
     * @return The true anomaly.
     */
    private static double getTrueAnomaly(double E, double e) {
        return 2.0 * FastMath.atan2(FastMath.sqrt(1.0 + e) * FastMath.sin(E / 2.0),
                                    FastMath.sqrt(1.0 - e) * FastMath.cos(E / 2.0));
    }

    /**
     * Solves Kepler's equation: M = E - e * sin(E)
     *
     * @param M The mean anomaly at the time.
     * @param e The eccentricity.
     *
     * @return The solution to Kepler's equation, E.
     */
    private static double solveKepler(double M, double e) {
        double E = (e < 0.8) ? M : FastMath.PI;
        for (int i = 0; i < 100; i++) {
            double f = E - e * FastMath.sin(E) - M;
            double fPrime = 1.0 - e * FastMath.cos(E);
            double dE = -f / fPrime;
            E += dE;
            if (FastMath.abs(dE) < 1.0e-10) break;
        }
        return E;
    }
    /**
     * Convert the given time from epoch (in days) to the true anomaly angle.
     *
     * @param dtDays The time from epoch, in days.
     *
     * @return The true anomaly, in radians.
     */
    public static double timeToTrueAnomaly(double dtDays, double period, double meanAnomaly, double e) {
        // Mean anomaly at time.
        double M = getMeanAnomalyAt(dtDays, period, meanAnomaly);
        // Solve eccentric anomaly with Kepler's equations.
        double E = solveKepler(M, e);
        // True anomaly at target time.
        return getTrueAnomaly(E, e);
    }
}
