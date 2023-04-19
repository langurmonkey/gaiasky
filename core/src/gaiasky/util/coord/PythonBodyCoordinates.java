/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.util.math.Vector3b;

import java.time.Instant;

public class PythonBodyCoordinates implements IBodyCoordinates {

    private final IPythonCoordinatesProvider provider;

    public PythonBodyCoordinates(IPythonCoordinatesProvider provider) {
        this.provider = provider;
    }

    @Override
    public void doneLoading(Object... params) {
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant instant, Vector3b out) {
        return null;
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant instant, Vector3b out) {
        return null;
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant instant, Vector3b out) {
        provider.getEquatorialCartesianCoordinates(AstroUtils.getJulianDate(instant), out);
        return out;
    }
}
