/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.util.Constants;
import gaiasky.util.math.Vector3Q;

import java.time.Instant;

public class PlutoCoordinates extends AbstractOrbitCoordinates {
    public PlutoCoordinates() {
        super();
    }

    @Override
    public void doneLoading(Object... params) {
        super.doneLoading(params);
    }

    @Override
    public Vector3Q getEclipticSphericalCoordinates(Instant date, Vector3Q out) {
        AstroUtils.plutoEclipticCoordinates(date, out);
        // To internal units
        out.z = out.z.multiply(Constants.KM_TO_U * scaling);
        return out;
    }

    @Override
    public Vector3Q getEclipticCartesianCoordinates(Instant date, Vector3Q out) {
        getEclipticSphericalCoordinates(date, out);
        Coordinates.sphericalToCartesian(out, out);
        return out;
    }

    @Override
    public Vector3Q getEquatorialCartesianCoordinates(Instant date, Vector3Q out) {
        getEclipticSphericalCoordinates(date, out);
        Coordinates.sphericalToCartesian(out, out);
        out.mul(Coordinates.eclToEq());

        return out;
    }

}