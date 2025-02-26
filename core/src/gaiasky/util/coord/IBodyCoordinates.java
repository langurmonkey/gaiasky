/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import gaiasky.util.math.Vector3b;

import java.time.Instant;
import java.util.Map;

public interface IBodyCoordinates {

    /**
     * Initializes the coordinates object
     *
     * @param params The parameter objects.
     */
    void doneLoading(Object... params);

    /**
     * Returns the ecliptic coordinates of the body in the out vector for the
     * given date.
     *
     * @param instant The instant.
     * @param out     The out vector with the ecliptic coordinates in internal
     *                units.
     *
     * @return The out vector for chaining.
     */
    Vector3b getEclipticSphericalCoordinates(Instant instant, Vector3b out);

    /**
     * Gets ecliptic cartesian coordinates for the given date.
     *
     * @param instant The instant.
     * @param out     The out vector where the ecliptic cartesian coordinates will
     *                be.
     *
     * @return The out vector for chaining, or null if the date is out of range,
     * in case of non-elliptical orbits such as Gaia.
     */
    Vector3b getEclipticCartesianCoordinates(Instant instant, Vector3b out);

    /**
     * Gets equatorial cartesian coordinates for the given date.
     *
     * @param instant The instant.
     * @param out     The out vector where the equatorial cartesian coordinates will
     *                be.
     *
     * @return The out vector for chaining, or null if the date is out of range,
     * in case of non-elliptical orbits such as Gaia.
     */
    Vector3b getEquatorialCartesianCoordinates(Instant instant, Vector3b out);

    /**
     * Update the references in this coordinates object with the given index.
     * @param index The index to use.
     */
    void updateReferences(Map<String, Entity> index);

    /**
     * Gets a copy of this coordinates object.
     * @return The copy.
     */
    IBodyCoordinates getCopy();

}
