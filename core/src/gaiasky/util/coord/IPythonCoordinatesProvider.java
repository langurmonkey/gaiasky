/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

public interface IPythonCoordinatesProvider {

    /**
     * This method takes in a julian date and outputs the coordinates
     * in the internal cartesian system.
     *
     * @param julianDate The julian date to get the coordinates for, as a 64-bit floating point number.
     *
     * @return A 3-vector containing the XYZ values in internal cartesian coordinates, and internal units.
     */
    Object getEquatorialCartesianCoordinates(Object julianDate, Object out);
}
