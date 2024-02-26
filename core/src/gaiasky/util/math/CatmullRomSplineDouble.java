/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.MathUtils;

/**
 * Double precision counterpart to the Catmull-Rom spline path.
 *
 * @param <T> Type to interpolate.
 */
public class CatmullRomSplineDouble<T extends VectorDouble<T>> implements PathDouble<T> {
    public T[] controlPoints;
    public boolean continuous;
    public int spanCount;
    private T tmp;
    private T tmp2;
    private T tmp3;

    public CatmullRomSplineDouble() {
    }

    public CatmullRomSplineDouble(final T[] controlPoints, final boolean continuous) {
        set(controlPoints, continuous);
    }

    /**
     * Calculates the Catmull-Rom spline value for the given position (t).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param t          The position (0<=t<=1) on the spline
     * @param points     The control points
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T calculate(final T out, final double t, final T[] points, final boolean continuous,
                                                          final T tmp) {
        final int n = continuous ? points.length : points.length - 3;
        double u = t * n;
        int i = (t >= 1f) ? (n - 1) : (int) u;
        u -= i;
        return calculate(out, i, u, points, continuous, tmp);
    }

    /**
     * Calculates the Catmull-Rom value for the given span (i) at the given position (u).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param i          The span (0<=i<spanCount) spanCount = continuous ? points.length : points.length - degree
     * @param u          The position (0<=u<=1) on the span
     * @param points     The control points
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T calculate(final T out, final int i, final double u, final T[] points,
                                                          final boolean continuous, final T tmp) {
        final int n = points.length;
        final double u2 = u * u;
        final double u3 = u2 * u;
        out.set(points[i]).scl(1.5 * u3 - 2.5 * u2 + 1.0);
        if (continuous || i > 0)
            out.add(tmp.set(points[(n + i - 1) % n]).scl(-0.5 * u3 + u2 - 0.5 * u));
        if (continuous || i < (n - 1))
            out.add(tmp.set(points[(i + 1) % n]).scl(-1.5 * u3 + 2 * u2 + 0.5 * u));
        if (continuous || i < (n - 2))
            out.add(tmp.set(points[(i + 2) % n]).scl(0.5 * u3 - 0.5 * u2));
        return out;
    }

    /**
     * Calculates the derivative of the Catmull-Rom spline for the given position (t).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param t          The position (0<=t<=1) on the spline
     * @param points     The control points
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T derivative(final T out, final double t, final T[] points, final boolean continuous,
                                                           final T tmp) {
        final int n = continuous ? points.length : points.length - 3;
        double u = t * n;
        int i = (t >= 1f) ? (n - 1) : (int) u;
        u -= i;
        return derivative(out, i, u, points, continuous, tmp);
    }

    /**
     * Calculates the derivative of the Catmull-Rom spline for the given span (i) at the given position (u).
     *
     * @param out        The {@link VectorDouble} to set to the result.
     * @param i          The span (0<=i<spanCount) spanCount = continuous ? points.length : points.length - degree
     * @param u          The position (0<=u<=1) on the span
     * @param points     The control points
     * @param continuous If true the b-spline restarts at 0 when reaching 1
     * @param tmp        A temporary vector used for the calculation
     * @return The value of out
     */
    public static <T extends VectorDouble<T>> T derivative(final T out, final int i, final double u, final T[] points,
                                                           final boolean continuous, final T tmp) {
        /*
         * catmull'(u) = 0.5 *((-p0 + p2) + 2 * (2*p0 - 5*p1 + 4*p2 - p3) * u + 3 * (-p0 + 3*p1 - 3*p2 + p3) * u * u)
         */
        final int n = points.length;
        final double u2 = u * u;
        // final double u3 = u2 * u;
        out.set(points[i]).scl(-u * 5 + u2 * 4.5f);
        if (continuous || i > 0)
            out.add(tmp.set(points[(n + i - 1) % n]).scl(-0.5 + u * 2 - u2 * 1.5f));
        if (continuous || i < (n - 1))
            out.add(tmp.set(points[(i + 1) % n]).scl(0.5 + u * 4 - u2 * 4.5f));
        if (continuous || i < (n - 2))
            out.add(tmp.set(points[(i + 2) % n]).scl(-u + u2 * 1.5f));
        return out;
    }

    public CatmullRomSplineDouble set(final T[] controlPoints, final boolean continuous) {
        if (tmp == null)
            tmp = controlPoints[0].cpy();
        if (tmp2 == null)
            tmp2 = controlPoints[0].cpy();
        if (tmp3 == null)
            tmp3 = controlPoints[0].cpy();
        this.controlPoints = controlPoints;
        this.continuous = continuous;
        this.spanCount = continuous ? controlPoints.length : controlPoints.length - 3;
        return this;
    }

    @Override
    public T valueAt(T out, double t) {
        final int n = spanCount;
        double u = t * n;
        int i = (t >= 1f) ? (n - 1) : (int) u;
        u -= i;
        return valueAt(out, i, u);
    }

    /** @return The value of the spline at position u of the specified span */
    public T valueAt(final T out, final int span, final double u) {
        return calculate(out, continuous ? span : (span + 1), u, controlPoints, continuous, tmp);
    }

    @Override
    public T derivativeAt(T out, double t) {
        final int n = spanCount;
        double u = t * n;
        int i = (t >= 1f) ? (n - 1) : (int) u;
        u -= i;
        return derivativeAt(out, i, u);
    }

    /** @return The derivative of the spline at position u of the specified span */
    public T derivativeAt(final T out, final int span, final double u) {
        return derivative(out, continuous ? span : (span + 1), u, controlPoints, continuous, tmp);
    }

    /** @return The span closest to the specified value */
    public int nearest(final T in) {
        return nearest(in, 0, spanCount);
    }

    /** @return The span closest to the specified value, restricting to the specified spans. */
    public int nearest(final T in, int start, final int count) {
        while (start < 0)
            start += spanCount;
        int result = start % spanCount;
        double dst = in.dst2(controlPoints[result]);
        for (int i = 1; i < count; i++) {
            final int idx = (start + i) % spanCount;
            final double d = in.dst2(controlPoints[idx]);
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

    public double approximate(final T in, int start, final int count) {
        return approximate(in, nearest(in, start, count));
    }

    public double approximate(final T in, final int near) {
        int n = near;
        final T nearest = controlPoints[n];
        final T previous = controlPoints[n > 0 ? n - 1 : spanCount - 1];
        final T next = controlPoints[(n + 1) % spanCount];
        final double dstPrev2 = in.dst2(previous);
        final double dstNext2 = in.dst2(next);
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
        double L1 = Math.sqrt(L1Sqr);
        double s = (L2Sqr + L1Sqr - L3Sqr) / (2.0 * L1);
        double u = MathUtils.clamp((L1 - s) / L1, 0f, 1f);
        return (n + u) / spanCount;
    }

    @Override
    public double locate(T v) {
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
