/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.GeomModule;

/**
 * API definition for the geometry module, {@link GeomModule}.
 * <p>
 * The geometry module provides calls and methods to carry out geometrical operations directly
 * within the scripting system.
 */
public interface GeomAPI {
    /**
     * Rotates a 3D vector around the given axis by the specified angle in degrees.
     * Vectors are arrays with 3 components. If more components are there, they are ignored.
     *
     * @param vector Vector to rotate, with at least 3 components.
     * @param axis   The axis, with at least 3 components.
     * @param angle  Angle in degrees.
     *
     * @return The new vector, rotated.
     */
    double[] rotate3(double[] vector,
                     double[] axis,
                     double angle);

    /**
     * Rotates a 2D vector by the specified angle in degrees, counter-clockwise assuming that
     * the Y axis points up.
     *
     * @param vector Vector to rotate, with at least 2 components.
     *
     * @return The new vector, rotated.
     */
    double[] rotate2(double[] vector,
                     double angle);

    /**
     * Computes the cross product between the two 3D vectors.
     *
     * @param vec1 First 3D vector.
     * @param vec2 Second 3D vector.
     *
     * @return Cross product 3D vector.
     */
    double[] cross3(double[] vec1,
                    double[] vec2);

    /**
     * Computes the dot product between the two 3D vectors.
     *
     * @param vec1 First 3D vector.
     * @param vec2 Second 3D vector.
     *
     * @return The dot product scalar.
     */
    double dot3(double[] vec1,
                double[] vec2);

}
