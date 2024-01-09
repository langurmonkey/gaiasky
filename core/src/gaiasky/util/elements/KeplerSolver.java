/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.elements;

import java.io.Serial;

/**
 * Solve the elliptical
 * <a href="http://en.wikipedia.org/wiki/Kepler_equation">Kepler Equation</a>
 * by bisections and Newton method with Taylor-expanded trigonometric functions.
 *
 * <p>
 * This class is immutable.
 * </p>
 *
 * The algorithm is taken from
 * <blockquote>
 *  T. Fukushima, 1996,
 *  <i>Astron. J</i>, <b>112</b>, 2858.
 * </blockquote>
 * <br>
 * It is designed for a general equation of the form
 * <pre>
 *  <code>
 *   D - e<sub>X</sub> sin D + e<sub>Y</sub> cos D = L,
 *  </code>
 *  with
 *  <code>
 *   e<sub>X</sub><sup>2</sup> + e<sub>Y</sub><sup>2</sup> &le; 1
 *  </code>
 * </pre>
 * <br>
 * But this implementation deals only with the case
 * <pre>
 *  <code>
 *   e<sub>Y</sub> = 0
 *  </code>
 * </pre>
 * and is optimized for that case.<br>
 *
 * The method to solve the Kepler equation is given in two versions:
 * <ul>
 *  <li> {@link #eccAnom(double,double) eccAnom(M,e)}</li>
 *  <li> {@link #eccAnomConstrained(double,double) eccAnomConstrained(M,e)}</li>
 * </ul>
 *
 * The second version is faster, but it is assumed that the mean anomaly is
 * between <i>0</i> and <i>2&pi;</i>.<br>
 *
 * Methods to compute the true anomaly from the eccentric anomaly and the
 * eccentricity are also available in two versions.<br>
 *
 * <p>
 * Re-coded from Fortran and C.
 * </p>
 *
 * @author <a href="mailto:Sergei.Klioner@tu-dresden.de">Sergei Klioner</a>
 * @author <a href="mailto:gilles.sadowski@ulb.ac.be">Gilles Sadowski (GS)</a>
 *
 * @version $Id: KeplerSolver.java 563374 2017-05-23 11:21:17Z gsadowsk $
 */
public class KeplerSolver {
    // Numerical constants.
    private static final double A3 = 1d / 3;
    private static final double A6 = 1d / 6;
    private static final double A30 = 1d / 30;
    private static final double A90 = 1d / 90;
    private static final double A620 = 1d / 620;

    // Constants for the bisection level.
    private static final int DIM0 = 7;
    private static final int DIM1 = 8;
    private static final int DIM2 = 9;

    // Internal auxiliary arrays to store the coefficients.
    private static final double[] a;
    private static final double[] s;
    private static final double[] c;

    static {
        final int max = 512;
        a = new double[max];
        s = new double[max];
        c = new double[max];

        int ixend = 1;
        int i = 0;
        double theta = Math.PI;
        a[0] = theta;
        s[0] = 0;
        c[0] = -1;

        for (int n = 2; n <= DIM2; n++) {
            ixend *= 2;
            theta /= 2;

            for (int ix = 1; ix <= ixend; ix++) {
                ++i;
                a[i] = theta * ((2 * ix) - 1);
                s[i] = Math.sin(a[i]);
                c[i] = Math.cos(a[i]);
            }
        }
    }

    /** Error tolerance. */
    private final double errMax;
    /** Maximum number of iterations. */
    private final int iterMax;

    /**
     * @param errMax Error tolerance.
     * @param iterMax Maximum number of iterations.
     */
    public KeplerSolver(double errMax,
                        int iterMax) {
        this.errMax = errMax;
        this.iterMax = iterMax;
    }

    /**
     * Constructor with default values for tolerance ({@code 1e-16})
     * and maximum number of iterations ({@code 50}).
     */
    public KeplerSolver() {
        this(1e-16, 50);
    }

    /**
     * Solve Kepler Equation (see reference above). This method calls
     * {@link #eccAnomConstrained(double,double) eccAnomConstrained} to perform
     * the actual resolution.
     *
     * @param mA Mean anomaly.
     * @param ecc Eccentricity.
     * @return the eccentric anomaly.
     * @throws IllegalArgumentException if <code>ecc</code> is not in the
     * interval <i>[0, 1)</i>.
     */
    public final double eccAnom(double mA,
                                double ecc) {
        if (ecc < 0
                || ecc >= 1) {
            throw new IllegalArgumentException("Eccentricity out of range: " + ecc);
        }

        final double x = Anomalies.reduce(mA);

        // Recovery of the reduction to [0, 2pi).
        final double dx = mA - x;

        return eccAnomConstrained(x, ecc) + dx;
    }

    /**
     * Solve Kepler Equation by bisections and Newton method with Taylor
     * expanded trigonometric functions. Note that, for performance, no
     * consistency checks are performed on the input.
     *
     * @param mA Mean anomaly, assumed to be between <i>0</i> and <i>2pi</i>.
     * @param ecc Eccentricity, assumed to be between <i>0</i> and <i>1</i>.
     * @return the eccentric anomaly in the range of <i>0</i> through <i>2pi</i>.
     * @throws IllegalStateException if the root could not be found within
     * the specified accuracy in less than the number of iterations.
     */
    public final double eccAnomConstrained(double mA,
                                           double ecc) {
        final double e2 = ecc * ecc;

        // Selection of bisection level (Revised Nov. 11, 1996).
        final int ndim = (e2 < 0.5) ? DIM0 : (e2 < 0.75 ? DIM1 : DIM2);

        // Bisection search.
        int i = 0;

        double f0;
        for (int n = 2; n <= ndim; n++) {
            f0 = a[i] - (ecc * s[i]) - mA;
            i = (2 * i) + 1;

            if (f0 <= 0) {
                ++i;
            }
        }

        // Newton method using Taylor-expanded trigonometric functions.
        final double f3 = ecc * c[i];
        final double f2 = ecc * s[i];
        final double f1 = 1 - f3;
        f0 = a[i] - f2 - mA;

        double d = -f0 / f1;

        for (int iter = 1; iter <= iterMax; iter++) {
            final double d2 = d * d * 0.5;
            final double sd = d * (1 - (d2 * (A3 - (d2 * (A30 - (d2 * A620))))));
            final double dcd = d2 * (1 - (d2 * (A6 - (d2 * A90))));
            final double df2 = (-f2 * dcd) + (f3 * sd);
            final double df3 = (-f3 * dcd) - (f2 * sd);
            final double fx = f1 - df3;
            final double fy = (f0 + d) - df2;
            d -= (fy / fx);

            if (Math.abs(fy) < errMax) { // Convergence is achieved.
                return a[i] + d;
            }
        }

        // No convergence.
        throw new SolutionNotFoundException(errMax,
                                            error(a[i] + d, mA, ecc),
                                            iterMax,
                                            mA,
                                            ecc);
    }

    /**
     * Compute the absolute error of Kepler equation given the mean and
     * eccentric anomalies and the eccentricity.
     *
     * @param eA eccentric anomaly.
     * @param mA mean anomaly.
     * @param ecc eccentricity.
     * @return the error.
     */
    public static double error(double eA,
                               double mA,
                               double ecc) {
        return meanAnomConstrained(eA, ecc) - mA;
    }

    /**
     * Compute mean anomaly (applying Kepler Equation). Note that, in this
     * version, no consistency checks are performed on the input.
     *
     * @param eA Eccentric anomaly.
     * @param ecc Eccentricity.
     * @return the mean anomaly.
     */
    public static double meanAnomConstrained(double eA,
                                             double ecc) {
        return eA - ecc * Math.sin(eA);
    }

    /**
     * Compute mean anomaly (applying Kepler Equation).
     *
     * @param eA Eccentric anomaly.
     * @param ecc Eccentricity.
     * @return the mean anomaly.
     * @throws IllegalArgumentException if <code>ecc</code> is not in the
     * interval <i>[0, 1)</i>.
     */
    public static double meanAnom(double eA,
                                  double ecc) {
        if (ecc < 0 ||
                ecc >= 1) {
            throw new IllegalArgumentException("Eccentricity out of range: " + ecc);
        }

        final double x = Anomalies.reduce(eA);

        // Recovery of the reduction to [0, 2pi).
        final double dx = eA - x;

        return meanAnomConstrained(x, ecc) + dx;
    }

    /**
     * Exception for reporting convergence failure.
     */
    private static class SolutionNotFoundException
            extends IllegalStateException {
        /**
         *
         */
        @Serial
        private static final long serialVersionUID = 7109980448563098982L;

        /** Error message template. */
        private final static String FORMAT =
                "Root with accuracy %e not found (actual error is %e) " +
                        "within %d iterations for the given " +
                        "mean anomaly (%e) and eccentricity (%e)";

        /**
         * @param tolerance Error tolerance.
         * @param actualError Actual error.
         * @param iterations Allowed number of iterations.
         * @param meanAnomaly Requested mean anomaly.
         * @param eccentricity Requested eccentricity.
         */
        SolutionNotFoundException(double tolerance,
                                  double actualError,
                                  int iterations,
                                  double meanAnomaly,
                                  double eccentricity) {
            super(String.format((java.util.Locale) null,
                                FORMAT,
                                tolerance,
                                actualError,
                                iterations,
                                meanAnomaly,
                                eccentricity));
        }
    }
}
