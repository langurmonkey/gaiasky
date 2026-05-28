/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.utils.Array;
import net.jafama.FastMath;

public class BSplineDouble<T extends VectorDouble<T>> implements PathDouble<T> {
    private final static int DEFAULT_DEGREE = 3;
    private final static double d6 = 1f / 6f;
    public T[] controlPoints;
    public Array<T> knots;
    public int degree;
    public boolean continuous;
    public int spanCount;
    private T tmp;
    private T tmp2;
    private T tmp3;

    public BSplineDouble() {
    }

    public BSplineDouble(T[] controlPoints, boolean continuous) {
        set(controlPoints, DEFAULT_DEGREE, continuous);
    }

    public BSplineDouble(T[] controlPoints, int degree, boolean continuous) {
        set(controlPoints, degree, continuous);
    }

    /**
     * Calculates the cubic b-spline value for the given position (t).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param t          The position ([0,1]) on the spline
     * @param points     The control points
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T cubic(T out, double t, T[] points, boolean continuous,
                                                      T tmp) {
        return cubic_derivative(out, t, points, continuous, tmp);
    }

    /**
     * Calculates the cubic b-spline derivative for the given position (t).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param t          The position ([0,1]) on the spline
     * @param points     The control points
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T cubic_derivative(T out, double t, T[] points,
                                                                 boolean continuous, T tmp) {
        int n = continuous ? points.length : points.length - 3;
        double u = t * n;
        int i = (t >= 1f) ? (n - 1) : (int) u;
        u -= i;
        return cubic(out, i, u, points, continuous, tmp);
    }

    /**
     * Calculates the cubic b-spline value for the given span (i) at the given position (u).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param i          The span ([0,spanCount]) spanCount = continuous ? points.length : points.length - 3 (cubic
     *                   degree)
     * @param u          The position ([0,1]) on the span
     * @param points     The control points
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T cubic(T out, int i, double u, T[] points,
                                                      boolean continuous, T tmp) {
        int n = points.length;
        double dt = 1f - u;
        double t2 = u * u;
        double t3 = t2 * u;
        out.set(points[i]).scl((3f * t3 - 6f * t2 + 4f) * d6);
        if (continuous || i > 0)
            out.add(tmp.set(points[(n + i - 1) % n]).scl(dt * dt * dt * d6));
        if (continuous || i < (n - 1))
            out.add(tmp.set(points[(i + 1) % n]).scl((-3f * t3 + 3f * t2 + 3f * u + 1f) * d6));
        if (continuous || i < (n - 2))
            out.add(tmp.set(points[(i + 2) % n]).scl(t3 * d6));
        return out;
    }

    /**
     * Calculates the cubic b-spline derivative for the given span (i) at the given position (u).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param i          The span ([0,spanCount]) spanCount = continuous ? points.length : points.length - 3 (cubic
     *                   degree)
     * @param u          The position ([0,1]) on the span
     * @param points     The control points
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T cubic_derivative(T out, int i, double u, T[] points,
                                                                 boolean continuous, T tmp) {
        int n = points.length;
        double dt = 1f - u;
        double t2 = u * u;
        double t3 = t2 * u;
        out.set(points[i]).scl(1.5f * t2 - 2 * u);
        if (continuous || i > 0)
            out.add(tmp.set(points[(n + i - 1) % n]).scl(-0.5f * dt * dt));
        if (continuous || i < (n - 1))
            out.add(tmp.set(points[(i + 1) % n]).scl(-1.5f * t2 + u + 0.5f));
        if (continuous || i < (n - 2))
            out.add(tmp.set(points[(i + 2) % n]).scl(0.5f * t2));
        return out;
    }

    /**
     * Calculates the n-degree b-spline value for the given position (t).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param t          The position ([0,1]) on the spline
     * @param points     The control points
     * @param degree     The degree of the b-spline
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T calculate(T out, double t, T[] points, int degree,
                                                          boolean continuous, T tmp) {
        int n = continuous ? points.length : points.length - degree;
        double u = t * n;
        int i = (t >= 1f) ? (n - 1) : (int) u;
        u -= i;
        return calculate(out, i, u, points, degree, continuous, tmp);
    }

    /**
     * Calculates the n-degree b-spline derivative for the given position (t).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param t          The position ([0,1]) on the spline
     * @param points     The control points
     * @param degree     The degree of the b-spline
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T derivative(T out, double t, T[] points, int degree,
                                                           boolean continuous, T tmp) {
        int n = continuous ? points.length : points.length - degree;
        double u = t * n;
        int i = (t >= 1f) ? (n - 1) : (int) u;
        u -= i;
        return derivative(out, i, u, points, degree, continuous, tmp);
    }

    /**
     * Calculates the n-degree b-spline value for the given span (i) at the given position (u).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param i          The span ([0,spanCount]) spanCount = continuous ? points.length : points.length - degree
     * @param u          The position ([0,1]) on the span
     * @param points     The control points
     * @param degree     The degree of the b-spline
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T calculate(T out, int i, double u, T[] points, int degree,
                                                          boolean continuous, T tmp) {
        if (degree == 3) {
            return cubic(out, i, u, points, continuous, tmp);
        }
        return out;
    }

    /**
     * Calculates the n-degree b-spline derivative for the given span (i) at the given position (u).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param i          The span ([0,spanCount]) spanCount = continuous ? points.length : points.length - degree
     * @param u          The position ([0,1]) on the span
     * @param points     The control points
     * @param degree     The degree of the b-spline
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T derivative(T out, int i, double u, T[] points, int degree,
                                                           boolean continuous, T tmp) {
        if (degree == 3) {
            return cubic_derivative(out, i, u, points, continuous, tmp);
        }
        return out;
    }

    public BSplineDouble set(T[] controlPoints, int degree, boolean continuous) {
        if (tmp == null)
            tmp = controlPoints[0].cpy();
        if (tmp2 == null)
            tmp2 = controlPoints[0].cpy();
        if (tmp3 == null)
            tmp3 = controlPoints[0].cpy();
        this.controlPoints = controlPoints;
        this.degree = degree;
        this.continuous = continuous;
        this.spanCount = continuous ? controlPoints.length : controlPoints.length - degree;
        if (knots == null)
            knots = new Array<>(false, spanCount);
        else {
            knots.clear();
            knots.ensureCapacity(spanCount);
        }
        for (int i = 0; i < spanCount; i++)
            knots.add(calculate(controlPoints[0].cpy(), continuous ? i : (int) (i + 0.5 * degree), 0d, controlPoints, degree,
                    continuous, tmp));
        return this;
    }

    @Override
    public T valueAt(T out, double t) {
        int n = spanCount;
        double u = t * n;
        int i = (t >= 1f) ? (n - 1) : (int) u;
        u -= i;
        return valueAt(out, i, u);
    }

    /** @return The value of the spline at position u of the specified span */
    public T valueAt(T out, int span, double u) {
        return calculate(out, continuous ? span : (span + (int) (degree * 0.5)), u, controlPoints, degree, continuous, tmp);
    }

    @Override
    public T derivativeAt(T out, double t) {
        int n = spanCount;
        double u = t * n;
        int i = (t >= 1f) ? (n - 1) : (int) u;
        u -= i;
        return derivativeAt(out, i, u);
    }

    /** @return The derivative of the spline at position u of the specified span */
    public T derivativeAt(T out, int span, double u) {
        return derivative(out, continuous ? span : (span + (int) (degree * 0.5)), u, controlPoints, degree, continuous, tmp);
    }

    /** @return The span closest to the specified value */
    public int nearest(T in) {
        return nearest(in, 0, spanCount);
    }

    /** @return The span closest to the specified value, restricting to the specified spans. */
    public int nearest(T in, int start, int count) {
        while (start < 0)
            start += spanCount;
        int result = start % spanCount;
        double dst = in.dst2(knots.get(result));
        for (int i = 1; i < count; i++) {
            int idx = (start + i) % spanCount;
            double d = in.dst2(knots.get(idx));
            if (d < dst) {
                dst = d;
                result = idx;
            }
        }
        return result;
    }

    @Override
    public double approximate(T v) {
        return approximate(v, nearest(v));
    }

    public double approximate(T in, int start, int count) {
        return approximate(in, nearest(in, start, count));
    }

    public double approximate(T in, int near) {
        int n = near;
        T nearest = knots.get(n);
        T previous = knots.get(n > 0 ? n - 1 : spanCount - 1);
        T next = knots.get((n + 1) % spanCount);
        double dstPrev2 = in.dst2(previous);
        double dstNext2 = in.dst2(next);
        T P1, P2, P3;
        if (dstNext2 < dstPrev2) {
            P1 = nearest;
            P2 = next;
            P3 = in;
        } else {
            P1 = previous;
            P2 = nearest;
            P3 = in;
            n = n > 0 ? n - 1 : spanCount - 1;
        }
        double L1Sqr = P1.dst2(P2);
        double L2Sqr = P3.dst2(P2);
        double L3Sqr = P3.dst2(P1);
        double L1 = FastMath.sqrt(L1Sqr);
        double s = (L2Sqr + L1Sqr - L3Sqr) / (2 * L1);
        double u = MathUtilsDouble.clamp((L1 - s) / L1, 0d, 1d);
        return (n + u) / spanCount;
    }

    @Override
    public double locate(T v) {
        // TODO Add a precise method
        return approximate(v);
    }

    @Override
    public double approxLength(int samples) {
        double tempLength = 0;
        for (int i = 0; i < samples; ++i) {
            tmp2.set(tmp3);
            valueAt(tmp3, (i) / ((double) samples - 1));
            if (i > 0)
                tempLength += tmp2.dst(tmp3);
        }
        return tempLength;
    }
}
