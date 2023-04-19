/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.utils;

import gaiasky.util.Nature;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Quaterniond;

public class Interpolator {

    protected static double dtMin = 0.02 / Nature.D_TO_S;

    /**
     * Static method for cubic Hermite interpolation between two points, given
     * their values and derivatives.
     *
     * @param x   desired abscissa (normally between x0 and x1)
     * @param x0  abscissa of first point
     * @param y0  function value at x0
     * @param yp0 first derivative at x0
     * @param x1  abscissa of second point
     * @param y1  function value at x1
     * @param yp1 derivative at x1
     *
     * @return array with interpolated function value at x and its derivative
     */
    public static double[] hermite3(final double x, final double x0, final double y0, final double yp0, final double x1, final double y1, final double yp1) {
        double dx = x1 - x0;
        double ddx = (yp0 + yp1 - 2.0 * (y1 - y0) / dx) / dx;
        double c = ((yp1 - yp0) / dx - 3.0 * ddx) / 2.0;
        double d = ddx / dx;
        double t = x - x0;
        double yInt = y0 + t * (yp0 + t * (c + t * d));
        double ypInt = yp0 + t * (2.0 * c + t * 3.0 * d);
        return new double[] { yInt, ypInt };

    }

    public static double[] linear(final double x, final double x0, final double y0, final double x1, final double y1) {
        double y = MathUtilsDouble.lint(x, x0, x1, y0, y1);
        double yprima = (y1 - y0) / (x1 - x0);
        return new double[] { y, yprima };
    }

    /**
     * Static method for computing the average attitude quaternion over a finite
     * time interval ta &lt;= t &lt;= tb, using cubic Hermite interpolation, as
     * well as the average time derivative
     * <p>
     * It is assumed that ta &lt;= tb. If tb-ta is less than dtMin then no
     * average is computed but the instantaneous (interpolated) values at the
     * instant (ta+tb)/2 are returned instead.
     * <p>
     * The times ta, tb, t[] are in [days] from some arbitrary but common
     * origin. Time derivatives are in [1/day].
     * <p>
     * The lengths of the array arguments must be: t.length &gt;= 2, q.length
     * &gt;= t.length, qDot-length &gt;= t.length. No check is made of these
     * conditions.
     * <p>
     * The argument indx is such that t[indx] is not far from ta and tb. It must
     * be in the range 0 &lt;= indx &lt;= t.length-2
     *
     * @param ta   start time of the averaging interval
     * @param tb   end time of the averaging interval
     * @param t    array array of increasing times encompassing the averaging
     *             interval (i.e., t[0] &lt;= ta and tb &lt;= t[t.length-1])
     * @param indx an index in the array t[] that is a suitable starting point
     *             for locating ta and tb in the array
     * @param q    array of attitude quaternions at times t[]
     * @param qDot array of attitude quaternion rates [1/timeUnit] at times t[]
     *
     * @return array containing the average attitude quaternion as the first
     * element and the average attitude quaternion rate [1/timeUnit] as
     * the second element
     */
    public static Quaterniond[] qHermiteAverage(final double ta, final double tb, final double[] t, final int indx, final Quaterniond[] q, final Quaterniond[] qDot) {

        Quaterniond qAve;
        Quaterniond qDotAve;

        if (tb - ta < dtMin) {
            double tm = (ta + tb) / 2;
            int left = getLeftVar(tm, t, indx);
            qAve = Interpolator.qEval(tm, t, q, qDot, left, Kind.VAL);
            qDotAve = Interpolator.qEval(tm, t, q, qDot, left, Kind.DER);

        } else {

            int lefta = getLeftVar(ta, t, indx);
            Quaterniond qa = Interpolator.qEval(ta, t, q, qDot, lefta, Kind.INT);

            int leftb = getLeftVar(tb, t, indx);
            Quaterniond qb = Interpolator.qEval(tb, t, q, qDot, leftb, Kind.INT);

            qAve = qb.mulAdd(qa, -1);
            for (int left = lefta; left < leftb; left++) {
                double dth = (t[left + 1] - t[left]) / 2;
                qAve.mulAdd(q[left], dth).mulAdd(q[left + 1], dth).mulAdd(qDot[left], dth * dth / 3).mulAdd(qDot[left + 1], -dth * dth / 3);
            }
            qAve.mul(1 / (tb - ta));

            qa = Interpolator.qEval(ta, t, q, qDot, lefta, Kind.VAL);
            qb = Interpolator.qEval(tb, t, q, qDot, leftb, Kind.VAL);
            qDotAve = qb.mulAdd(qa, -1).mul(1 / (tb - ta));
        }

        return new Quaterniond[] { qAve, qDotAve };
    }

    /**
     * Evaluates the quaternion derivative, value or integral at point tx, using
     * Hermite interpolation in t[], q[], qDot[]. left is such that t[left]
     * &lt;= tx &lt; t[left+1]. Kind = DER returns the derivative at tx, VAL
     * returns the value at tx, and INT returns the integral from t[left] to tx.
     *
     * @param tx   time at which the derivative, value or integral is evaluated
     * @param t    array of times (length &gt;= 2)
     * @param q    array of quaternions
     * @param qDot array of quaternion derivatives
     * @param left index in t, q and qDot susch that t[left] &lt;= tx &lt;
     *             t[left+1]
     * @param kind which kind of result is returned (derivative, value or
     *             integral)
     *
     * @return The quaternion
     */
    public static Quaterniond qEval(double tx, double[] t, Quaterniond[] q, Quaterniond[] qDot, int left, Kind kind) {
        double dt = t[left + 1] - t[left];
        double x = (tx - t[left]) / dt;
        double[] p;
        switch (kind) {
        case DER:
            p = Interpolator.interPolDer(x);
            p[0] /= dt;
            p[1] /= dt;
            break;
        case VAL:
            p = Interpolator.interPolVal(x);
            p[2] *= dt;
            p[3] *= dt;
            break;
        case INT:
            p = Interpolator.interPolInt(x);
            p[0] *= dt;
            p[1] *= dt;
            p[2] *= dt * dt;
            p[3] *= dt * dt;
            break;
        default:
            p = null;
            break;
        }
        Quaterniond qx = q[left].cpy().mul(p[0]).mulAdd(q[left + 1], p[1]).mulAdd(qDot[left], p[2]).mulAdd(qDot[left + 1], p[3]);
        return qx;
    }

    /**
     * Find left such that t[left] &lt;= ta &lt; t[left+1]
     *
     * @param ta
     * @param t
     * @param indx starting index
     *
     * @return The left index
     */
    public static int getLeft(double ta, double[] t, int indx) {
        int left = indx;
        while (t[left] > ta)
            left--;
        while (t[left + 1] <= ta)
            left++;
        return left;
    }

    /**
     * Find left such that t[left] &lt;= ta &lt; t[left+1] (but one less if ta
     * == t[left+1])
     *
     * @param ta
     * @param t
     * @param indx starting index
     *
     * @return The left index
     */
    public static int getLeftVar(double ta, double[] t, int indx) {
        if (ta == t[t.length - 1]) {
            return t.length - 2;
        }
        return getLeft(ta, t, indx);
    }

    /**
     * For normalized argument x (between 0 and 1), calculate the four
     * interpolating polynomials a0(x), a1(x), b0(x), b1(x) [DRO-012, Eq. (8)]
     *
     * @param x
     *
     * @return double array containing a0, a1, b0, b1 at x
     */
    protected static double[] interPolVal(double x) {
        double a1 = x * x * (3 - 2 * x);
        double a0 = 1 - a1;
        double b = x * (x - 1);
        double b1 = x * b;
        double b0 = b1 - b;
        return new double[] { a0, a1, b0, b1 };
    }

    /**
     * For normalized argument x (between 0 and 1), calculate the derivatives
     * ap0(x), ap1(x), bp0(x), bp1(x) of the four interpolating polynomials
     *
     * @param x The value
     *
     * @return double array containing ap0, ap1, bp0, bp1 at x
     */
    protected static double[] interPolDer(double x) {
        double ap1 = 6 * x * (1 - x);
        double ap0 = -ap1;
        double bp1 = x * (3 * x - 2);
        double bp0 = bp1 + 1 - 2 * x;
        return new double[] { ap0, ap1, bp0, bp1 };
    }

    /**
     * For normalized argument x (between 0 and 1), calculate the integrals
     * A0(x), A1(x), B0(x), B1(x) of the interpolating polynomials
     * <p>
     * A0(x) = int_0^x a0(y)*dy (etc)
     *
     * @param x The value
     *
     * @return double array containing A0, A1, B0, B1 at x
     */
    protected static double[] interPolInt(double x) {
        double x2 = x * x;
        double x3 = x2 * x;
        double A1 = x3 * (1 - 0.5 * x);
        double A0 = x - A1;
        double B1 = x3 * (0.25 * x - 0.333333333333333333);
        double B0 = B1 + x2 * (0.5 - 0.333333333333333333 * x);
        return new double[] { A0, A1, B0, B1 };
    }

    /**
     * In the non-decreasing sequence xa[0:n-1], finds the left index such that
     * xa[left] &lt;= x &lt; xa[left+1]
     * <p>
     * If x &lt; xa[0] the method returns -1 if x &gt;= xa[n-1], the last index
     * to the array (n-1) is returned
     * <p>
     * Uses a straight bisection method to locate the left index.
     *
     * @param xa       - array of non-decreasing values
     * @param xaLength - ???
     * @param x        - value to locate
     *
     * @return left index
     *
     * @deprecated Mantis 14225, deprecated August 2, 2012. Use
     * {@link AttitudeUtils#findLeftIndex(long, long[], int)}.
     * Remove by GT 18.0.
     */
    @Deprecated
    public static int findLeftIndex(final long[] xa, final int xaLength, final long x) {
        int kLo = 0;
        int kHi = xaLength - 1;

        // Check if x is in the range of xa
        if (x > xa[kHi] || x == xa[kHi]) {
            return kHi;
        }

        if (x < xa[kLo]) {
            return -1;
        }

        // Indices must differ by at least 1
        while ((kHi - kLo) > 1) {
            final int k = (kHi + kLo) >>> 1;
            if (xa[k] > x) {
                kHi = k;
            } else {
                kLo = k;
            }
        }

        return kLo;
    }

    /**
     * Kind of interpolation: for derivative, value or integral
     *
     * @author lennartlindegren
     * @version $Id: Interpolator.java 374850 2014-07-01 16:04:16Z pbalm $
     */
    public enum Kind {
        DER,
        VAL,
        INT
    }

}
