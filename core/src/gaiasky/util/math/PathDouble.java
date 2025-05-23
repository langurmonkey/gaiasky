/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

public interface PathDouble<T> {
    T derivativeAt(T out, double t);

    /** @return The value of the path at t where 0&le;t&le;1 */
    T valueAt(T out, double t);

    /**
     * @return The approximated value (between 0 and 1) on the path which is closest to the specified value. Note that the
     * implementation of this method might be optimized for speed against precision, see {@link #locate(Object)} for a more
     * precise (but more intensive) method.
     */
    double approximate(T v);

    /**
     * @return The precise location (between 0 and 1) on the path which is closest to the specified value. Note that the
     * implementation of this method might be CPU intensive, see {@link #approximate(Object)} for a faster (but less
     * precise) method.
     */
    double locate(T v);

    /**
     * @param samples The amount of divisions used to approximate length. Higher values will produce more precise results,
     *                but will be more CPU intensive.
     *
     * @return An approximated length of the spline through sampling the curve and accumulating the Euclidean distances between
     * the sample points.
     */
    double approxLength(int samples);

}
