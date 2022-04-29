/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*
 * Copyright (C) 2006-2011 Gaia Data Processing and Analysis Consortium
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */
package gaiasky.util.gaia.utils;

import gaiasky.util.Logger;

/**
 * Class with static utility methods to deal with B-splines.
 *
 * @author Uwe Lammers and David Hobbs modified from original AttitudeUtils
 *         class by hsiddiqu
 * @version $Id: AttitudeUtils.java 206987 2011-11-17 15:51:06Z dhobbs $
 */
public class AttitudeUtils {

    /**
     * In the non-decreasing sequence xa[0:n-1], finds the left index such that
     * xa[left] &lt;= x &lt; xa[left+1]
     *
     * If x &lt; xa[0] the method returns left = -1. If x &gt;= xa[n-1] the
     * method returns left = n-1 (the last valid index to the array). These are
     * the logical extensions of the basic condition when x is outside the
     * interval defined by the array - formally they could be derived by putting
     * xa[-1] = -inf and xa[n] = +inf.
     *
     * This implementation of findLeftIndex uses an estimation method which
     * assumes that the intervals have the same or similar length to speed up
     * the search, this method is faster than the bisection one at least when
     * there aren't many gaps and the time intervals are identical or similar.
     *
     *
     * @param x
     *            value to find the left index for
     * @param xa
     *            array of spline knot times
     * @param splineOrder
     *            order of the spline
     * @return The left index
     **/
    public static int findLeftIndex(final long x, final long[] xa, int splineOrder) {
        // Check if x is in the range of xa.
        if (x >= xa[xa.length - 1]) {
            return xa.length - 1;
        }

        if (x < xa[0]) {
            return -1;
        }

        // skip the first knots to find the increment
        double attitudeTimeIncrement = xa[splineOrder + 1] - xa[splineOrder];

        int estXPos = (int) ((x - xa[0]) / attitudeTimeIncrement) + splineOrder + 1;

        if (estXPos >= xa.length) {
            estXPos = xa.length - 1;
        }

        if (estXPos < 0) {
            estXPos = 0;
        }

        if (xa[estXPos] == x) {
            while (xa[estXPos] == x) {
                estXPos--;
            }
        } else if (xa[estXPos] > x) {
            while (xa[estXPos] > x) {
                estXPos--;
            }
        } else {
            while (((estXPos + 1) < xa.length) && xa[estXPos + 1] < x) {
                estXPos++;
            }
        }
        return estXPos;
    }

    /**
     * Variant of findLeftIndex which returns left = xa.length - 2 if x ==
     * xa[xa.length-1]. This allows linear or cubic Hermite interpolation
     * between xa[left] and xa[left+1] also in the case when x is exactly equal
     * to the last point in the array.
     *
     * @param x
     *            value to find the left index for
     * @param xa
     *            array of spline knot times
     * @param splineOrder
     *            order of the spline
     * @return left index
     **/
    public static int findLeftIndexVar(final long x, final long[] xa, int splineOrder) {

        // handle the special case
        if (x == xa[xa.length - 1]) {
            return xa.length - 2;
        }

        // for all other values of x use the standard method
        return AttitudeUtils.findLeftIndex(x, xa, splineOrder);
    }

    /**
     * Variant of findLeftIndex to be used for BSpline interpolation. In general
     * the value of left is determined such that * xa[left] and xa[left+1] exist
     * * xa[left+1] - xa[left] &gt; 0 * xa[left] &lt;= x &lt; xa[left+1]
     *
     * More specific, this means that
     *
     * if x &gt; xa[xa.length - 1], then the BSpline cannot be evaluated so -1
     * is returned.
     *
     * if x == xa[xa.length-1] then left is determined such that left is the
     * largest value for which xa[left] &lt; xa[xa.length-n] for the largest
     * value of n &gt; 0 for which xa[xa.length-n] == xa[xa.length-1].
     *
     * for all xa[0] &lt; x &lt; xa[xa.length-1] left is determined such that
     * xa[left] &lt;= x &lt; xa[left+1]. If then xa[left] == xa[left-1] &lt;= x
     * then n is determined to be the smallest value for which xa[left-n] &lt;
     * xa[left] and then left - n is returned.
     *
     * if xa[left] &lt;= x and left == 0, then n is determined to be the largest
     * value for which xa[left] == xa[left+n] and then left + n is returned.
     *
     * if x &lt; xa[0], then the BSpline cannot be evaluated so -1 is returned.
     *
     * For more info see the discussion in Mantis 27523.
     *
     * @param x
     *            value to find the left index for
     * @param xa
     *            array of spline knot times
     * @return left index
     **/
    public static int findLeftIndexBSpline(final long x, final long[] xa) {
        if (x > xa[xa.length - 1]) {
            return -1;
        }

        if (x == xa[xa.length - 1]) {
            int left = xa.length - 1;
            while (left > 1 && xa[left] - xa[xa.length - 1] == 0) {
                left--;
            }
            return left;
        }

        if (x < xa[0]) {
            return -1;
        }

        int klo = 0;
        int khi = xa.length - 1;
        while ((khi - klo) > 1) { // Indices must differ by at least 1.
            final int k = (khi + klo) >>> 1;

            if (xa[k] > x) {
                khi = k;
            } else {
                klo = k;
            }
        }

        if (xa[klo] == x) {
            if (xa[0] == x) {
                while (klo < xa.length - 1 && xa[klo] - xa[klo + 1] == 0) {
                    klo++;
                }
                if (xa[klo] == x && xa[klo + 1] == x) {
                    klo++;
                }
            } else {
                while (klo > 1 && xa[klo] - xa[klo - 1] == 0) {
                    klo--;
                }
                if (xa[klo] == x && xa[klo + 1] == x) {
                    klo--;
                }
            }
        }

        return (klo);
    }

    /**
     * In the non-decreasing sequence xa[0:n-1], finds the left index such that
     * xa[left] &lt;= x &lt; xa[left+1]
     *
     * If x &lt; xa[0] the method returns left = -1. If x &gt;= xa[n-1] the
     * method returns left = n-1 (the last valid index to the array). These are
     * the logical extensions of the basic condition when x is outside the
     * interval defined by the array - formally they could be derived by putting
     * xa[-1] = -inf and xa[n] = +inf.
     *
     * This method uses a straight bisection method to locate the left index.
     *
     * @param xa
     *            array with knot times
     * @param x
     *            value to find the left index for
     * @return left index
     */
    public static int findLeftIndexBisection(final long x, final long[] xa) {
        int klo = 0;
        int khi = xa.length - 1;

        // Check if x is in the range of xa.
        if (x > xa[khi] || x == xa[khi]) {
            return khi;
        }

        if (x < xa[klo]) {
            return -1;
        }

        while ((khi - klo) > 1) { // Indices must differ by at least 1.
            final int k = (khi + klo) >>> 1;

            if (xa[k] > x) {
                khi = k;
            } else {
                klo = k;
            }
        }
        return (klo);
    }

    /**
     * Variant of findLeftIndexBisection which returns left = xa.length - 2 if x
     * == xa[xa.length-1]. This allows linear or cubic Hermite interpolation
     * between xa[left] and xa[left+1] also in the case when x is exactly equal
     * to the last point in the array.
     *
     * @param x
     *            value to find the left index for
     * @param xa
     *            array of spline knot times
     * @return left index
     **/
    public static int findLeftIndexBisectionVar(final long x, final long[] xa) {

        // handle the special case
        if (x == xa[xa.length - 1]) {
            return xa.length - 2;
        }

        // for all other values of x use the standard method
        return AttitudeUtils.findLeftIndexBisection(x, xa);
    }

    /**
     * In the non-decreasing sequence xa[0:n-1], finds the left index such that
     * xa[left] &lt;= x &lt; xa[left+1]
     *
     * If x &lt; xa[0] the method returns left = -1. If x &gt;= xa[n-1] the
     * method returns left = n-1 (the last valid index to the array). These are
     * the logical extensions of the basic condition when x is outside the
     * interval defined by the array - formally they could be derived by putting
     * xa[-1] = -inf and xa[n] = +inf.
     *
     * This method uses a straight bisection method to locate the left index.
     *
     * @param xa
     *            array with knot times
     * @param x
     *            value to find the left index for
     * @return left index
     */
    public static int findLeftIndexBisection(final int x, final int[] xa) {
        int klo = 0;
        int khi = xa.length - 1;

        // Check if x is in the range of xa.
        if (x > xa[khi] || x == xa[khi]) {
            return khi;
        }

        if (x < xa[klo]) {
            return -1;
        }

        while ((khi - klo) > 1) { // Indices must differ by at least 1.
            final int k = (khi + klo) >>> 1;

            if (xa[k] > x) {
                khi = k;
            } else {
                klo = k;
            }
        }
        return (klo);
    }

    /**
     * Variant of findLeftIndexBisection which returns left = xa.length - 2 if x
     * == xa[xa.length-1]. This allows linear or cubic Hermite interpolation
     * between xa[left] and xa[left+1] also in the case when x is exactly equal
     * to the last point in the array.
     *
     * @param x
     *            value to find the left index for
     * @param xa
     *            array of spline knot times
     * @return left index
     **/
    public static int findLeftIndexBisectionVar(final int x, final int[] xa) {

        // handle the special case
        if (x == xa[xa.length - 1]) {
            return xa.length - 2;
        }

        // for all other values of x use the standard method
        return AttitudeUtils.findLeftIndexBisection(x, xa);
    }

    /**
     * Returns the values and first derivatives of the four non-zero cubic
     * B-splines in the interval tau(left) &lt;= x &lt; tau(left+1)
     * <p>
     * Based on the subroutine BSPLVB in C. de Boor, A Practical Guide to
     * Splines, Springer 1978
     * <p>
     * Calculation of derivatives was added (perhaps inefficiently) as the
     * analytical derivative of each statement in the original BSPLVB routine,
     * using that (d/dx)deltar(j) = -1 and (d/dx)deltal(j) = +1.
     * <p>
     * The order of the spline is set by the parameter ATT_SPLINE_ORDER (=4 for
     * cubic)
     *
     * @param tau
     *            knot sequence
     * @param x
     *            point at which the B-splines should be evaluated
     * @param splineOrder
     *            order of the spline
     * @param leftIndex
     *            integer chosen (usually) such that tau(leftIndex) &lt;= x &lt;
     *            tau(leftIndex+1) (left can be found by
     *            {@link #findLeftIndex(long, long[], int)}
     * @param b0
     *            values of the cubic B-splines at point {@code x}
     * @param b1
     *            first derivatives (wrt x) of the B-splines at point {@code x}
     * @throws RuntimeException
     *             if input is inconsistent
     */
    public static void calcBsplines(final long x, final long[] tau, int splineOrder, final int leftIndex, final double[] b0, final double[] b1) throws RuntimeException {
        /*
         * check that left index is in correct range to avoid AIOBE below: See
         * B.2 in AGIS A&A paper: The knots used for computing the B-spline
         * values in the interval [tau_l, tau_(l+1)) are tau_(l-M+2) through
         * tau_(l+M-1) hence l-M+2 >= 0 => l>=M-2 l+M-1 <= N+M-1 => l<=N and
         * since L=N+M => N=L-M, so, l<=L-M
         */
        if (leftIndex < splineOrder - 2 || leftIndex > tau.length - splineOrder) {
            throw new RuntimeException("leftIndex (" + leftIndex + ")" + " is not within valid bounds [M-2, N]=[" + (splineOrder - 2) + ", " + (tau.length - splineOrder) + "]");
        }

        long[] deltal = new long[splineOrder];
        long[] deltar = new long[splineOrder];

        long dr;
        long dl;
        double t0;
        double t1;

        b0[0] = 1.;
        b1[0] = 0.;

        for (int i = 0; i < (splineOrder - 1); i++) {
            deltar[i] = tau[leftIndex + i + 1] - x;
            deltal[i] = x - tau[leftIndex - i];

            double s0 = 0.;
            double s1 = 0.;

            for (int j = 0; j <= i; j++) {
                dr = deltar[j];
                dl = deltal[i - j];

                t0 = b0[j] / (double) (dr + dl);
                t1 = b1[j] / (double) (dr + dl);

                b0[j] = s0 + ((double) dr * t0);
                b1[j] = s1 - t0 + ((double) dr * t1);

                s0 = dl * t0;
                s1 = t0 + ((double) dl * t1);
            }
            // b0.length and b1.lentgh must be >= ATT_SPLINE_ORDER
            int j = i + 1;
            b0[j] = s0;
            b1[j] = s1;
        }
    }

    /**
     * Deprecated : Use the Quaternion class instead and this method is not
     * reliable E.g. double[] angleDiff = q1.smallAngularDifference(q2); using
     * quaternions E.g. double[] angleDiff = new
     * Quaternion(q1).smallAngularDifference(new Quaternion(q2)); using double
     * arrays
     *
     * Compute the angular differences about the principal axes of two
     * body-triads represented by two quaternions. This calculation comes from
     * SAG-LL-30 where an inertial rotation vector between to attitudes
     * represented by quaternion q0 and q1 is derived. It is a SMALL ANGLE
     * approximation. If the algorithm finds a difference&gt;EPS between the
     * components of the 2 quaternions, it will flip sign of q1. This is vain if
     * the angular difference is too large and a warning is then logged. Note
     * this is a static utility.
     *
     * @param q0
     *            First quaternion
     * @param q1
     *            Second quaternion
     * @return Array of length 3 with angle between principal axes
     */
    @Deprecated
    public static double[] smallAngularDifferences(final double[] q0, final double[] q1) {
        double[] dq = new double[4];

        final double EPS = 1.e-3;
        int maxk = 1;
        for (int k = 0; k < maxk; ++k) {
            for (int i = 0; i < 4; ++i) {
                dq[i] = q1[i] - q0[i];

                if (Math.abs(dq[i]) > EPS) {
                    for (int j = 0; j < 4; ++j) {
                        q1[j] = -q1[j];
                    }
                    maxk = 2;
                    if (k == 1) {
                        Logger.getLogger(AttitudeUtils.class).warn("out of small angle approximation: insignificant result");
                    }
                    break;
                }
            }
        }
        return new double[] { 2. * ((dq[0] * q0[3]) + (dq[1] * q0[2]) - (dq[2] * q0[1]) - (dq[3] * q0[0])), 2. * ((-dq[0] * q0[2]) + (dq[1] * q0[3]) + (dq[2] * q0[0]) - (dq[3] * q0[1])), 2. * ((dq[0] * q0[1]) - (dq[1] * q0[0]) + (dq[2] * q0[3]) - (dq[3] * q0[2])) };
    }

    /**
     * Insert one or more knots in a knot sequence with a certain multiplicity
     * This will redefine a larger knot sequence of the same duration
     *
     * @param oldKnots
     *            The initial array of knot times
     * @param tInsert
     *            The array of knot insert times
     * @param multiplicity
     *            The multiplicity of the inserted knots
     * @param splineOrder
     *            spline order
     * @return knots The updated knot times, including inserted knots of
     *         multiplicity
     *
     */
    public static long[] insertKnots(long[] oldKnots, long[] tInsert, int multiplicity, int splineOrder) {

        int nDegsFreedomN = oldKnots.length - splineOrder;
        long[] knots = new long[nDegsFreedomN + multiplicity * tInsert.length + splineOrder];

        int count = 0;
        knots[0] = oldKnots[0];
        for (int k = 1; k < oldKnots.length; k++) {
            long knot = oldKnots[k];
            for (int i = 0; i < tInsert.length; i++) {
                long insertTime = tInsert[i];
                // if((insertTime < knot) && (insertTime >= oldKnots[k - 1]))
                if ((insertTime < knot) && (oldKnots[k - 1] < insertTime)) {
                    for (int m = 0; m < multiplicity; m++) {
                        knots[k + count] = insertTime;
                        count++;
                    }
                }
            }
            knots[k + count] = knot;
        }

        return knots;
    }

    /**
     * Insert one or more knots in a knot sequence with a certain multiplicity
     * This will redefine a larger knot sequence of the same duration
     * Additionally, corresponding spline coefficients are inserted with zero as
     * their starting values. These zero values need to be replaced by a fit or
     * updated values.
     *
     * @param old
     *            The initial array of knot times The initial array of spline
     *            coefficients
     * @param tInsert
     *            The array of knot insert times
     * @param multiplicity
     *            The multiplicity of the inserted knots
     * @return knots The updated knot times, including inserted knots of
     *         multiplicity The updated spline coefficients, including inserted
     *         splines at knots of multiplicity
     *
     */
    public static KnotsAndSplines insertKnotsAndSplines(KnotsAndSplines old, long[] tInsert, int multiplicity, int splineOrder) {

        int nDegsFreedomN = old.knots.length - splineOrder;
        KnotsAndSplines out = new KnotsAndSplines(new long[nDegsFreedomN + multiplicity * tInsert.length + splineOrder], new double[4][nDegsFreedomN + multiplicity * tInsert.length]);

        int count = 0;
        out.knots[0] = old.knots[0];
        for (int j = 0; j < 4; j++) {
            out.splines[j][0] = old.splines[j][0];
        }
        for (int k = 1; k < old.knots.length; k++) {
            long knot = old.knots[k];
            for (int i = 0; i < tInsert.length; i++) {
                long insertTime = tInsert[i];
                // if((insertTime < knot) && (insertTime >= old.knots[k - 1]))
                if (insertTime < knot && old.knots[k - 1] < insertTime) {
                    for (int m = 0; m < multiplicity; m++) {
                        out.knots[k + count] = insertTime;
                        for (int j = 0; j < 4; j++) {
                            out.splines[j][k + count] = 0.0;
                        }
                        count++;
                    }
                }
            }
            out.knots[k + count] = knot;
            if (k < old.knots.length - splineOrder) {
                for (int j = 0; j < 4; j++) {
                    out.splines[j][k + count] = old.splines[j][k];
                }
            }
        }
        return out;
    }

    /**
     * Insert one or more elements in an array with a certain multiplicity This
     * will redefine a larger array of the same duration. This is designed to
     * work with a knot sequence.
     *
     * @param oldElements
     *            The initial array at of elements at knot times
     * @param knots
     *            The knot sequence which is not changed
     * @param tInsert
     *            The array of insert times
     * @param multiplicity
     *            The multiplicity of the inserted elements
     * @param splineOrder
     *            spline order
     * @return elements The updated elements including inserted knots of
     *         multiplicity
     *
     */
    public static double[] insertElements(double[] oldElements, long[] knots, long[] tInsert, int multiplicity, int splineOrder) {

        int nDegsFreedomN = knots.length - splineOrder;
        double[] elements = new double[nDegsFreedomN + multiplicity * tInsert.length + splineOrder - 1];

        int count = 0;
        elements[0] = oldElements[0];
        for (int k = 1; k < knots.length; k++) {
            long knot = knots[k];
            for (int i = 0; i < tInsert.length; i++) {
                long insertTime = tInsert[i];
                // if((insertTime < knot) && (insertTime >= old.knots[k - 1]))
                if (insertTime < knot && knots[k - 1] < insertTime) {
                    for (int m = 0; m < multiplicity; m++) {
                        elements[k + count] = 0.0;
                        count++;
                    }
                }
            }
            if (k + count < elements.length) {
                elements[k + count] = oldElements[k];
            }
        }
        return elements;
    }

    /**
     * Utility class for manipulating knots and splines together
     */
    public static class KnotsAndSplines {
        public KnotsAndSplines(long[] knots, double[][] splines) {
            this.knots = knots;
            this.splines = splines;
        }

        public long[] knots;
        public double[][] splines;
    }

}
