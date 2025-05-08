/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import gaiasky.util.math.Vector3Q;

import java.time.Instant;
import java.util.Map;

public class PythonBodyCoordinates implements IBodyCoordinates {

    private final IPythonCoordinatesProvider provider;

    public PythonBodyCoordinates(IPythonCoordinatesProvider provider) {
        this.provider = provider;
    }

    @Override
    public void doneLoading(Object... params) {
    }

    @Override
    public Vector3Q getEclipticSphericalCoordinates(Instant instant, Vector3Q out) {
        return null;
    }

    @Override
    public Vector3Q getEclipticCartesianCoordinates(Instant instant, Vector3Q out) {
        return null;
    }

    @Override
    public Vector3Q getEquatorialCartesianCoordinates(Instant instant, Vector3Q out) {
        provider.getEquatorialCartesianCoordinates(AstroUtils.getJulianDate(instant), out);
        return out;
    }

    @Override
    public void updateReferences(Map<String, Entity> index) {
    }

    @Override
    public IBodyCoordinates getCopy() {
        return this;
    }

}
