/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

public class LinearDouble<T extends VectorDouble<T>> implements PathDouble<T> {

    public T[] controlPoints;

    public LinearDouble(final T[] controlPoints) {
        set(controlPoints);
    }

    public LinearDouble<T> set(final T[] controlPoints) {
        this.controlPoints = controlPoints;
        return this;
    }

    @Override
    public T derivativeAt(T out,
                          double t) {
        return null;
    }

    @Override
    public T valueAt(T out,
                     double t) {
        if (t == 0) {
            return controlPoints[0];
        }
        if (t == 1) {
            return controlPoints[controlPoints.length - 1];
        }
        int n = controlPoints.length;
        double step = 1d / ((double) n - 1d);
        int i0 = (int) Math.floor(t / step);
        int i1 = i0 + 1;
        double alpha = (t / step) - (double) i0;
        T p0 = controlPoints[i0];
        T p1 = controlPoints[i1];

        out.set(p0);
        return out.interpolate(p1, alpha, InterpolationDouble.linear);
    }

    @Override
    public double approximate(T v) {
        return 0;
    }

    @Override
    public double locate(T v) {
        return 0;
    }

    @Override
    public double approxLength(int samples) {
        return 0;
    }
}
