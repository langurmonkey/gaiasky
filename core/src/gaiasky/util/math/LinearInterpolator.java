/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

/**
 * Implements a linear function for interpolation of real univariate functions.
 */
public class LinearInterpolator {

    private final double[] x, y;
    private final int length;

    /**
     * Creates a linear interpolator with the given interpolation points.
     *
     * @param x the arguments for the interpolation points
     * @param y the values for the interpolation points
     */
    public LinearInterpolator(double[] x, double[] y) {
        if (x == null || y == null) {
            throw new RuntimeException("Interpolation points can't be null");
        }
        if (x.length == 0 || y.length == 0) {
            throw new RuntimeException("Interpolation points can't be empty");
        }
        if (x.length != y.length) {
            throw new RuntimeException("Lengths of x and y must be the same");
        }
        this.x = x;
        this.y = y;
        this.length = x.length;
    }

    public double value(double v) {
        if (x[0] > v || x[length - 1] < v) {
            throw new RuntimeException("Value out of range: " + v + " not in [" + x[0] + "," + x[length - 1] + "]");
        }

        // Find index using a binary search.
        int i0 = -1;
        int i1 = -1;
        int first = 1;
        int last = length - 1;
        int mid = (first + last) / 2;
        while (first <= last) {
            if (v >= x[mid - 1] && v <= x[mid]) {
                // Found.
                i0 = mid - 1;
                i1 = mid;
                break;
            } else if (v < x[mid - 1]) {
                last = mid - 1;
            } else {
                first = mid + 1;
            }
            mid = (first + last) / 2;
        }

        return MathUtilsDouble.lint(v, x[i0], x[i1], y[i0], y[i1]);
    }
}
