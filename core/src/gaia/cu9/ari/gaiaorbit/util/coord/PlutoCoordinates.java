/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.coord;

import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.time.Instant;

public class PlutoCoordinates extends AbstractOrbitCoordinates {
    @Override
    public void doneLoading(Object... params) {
        super.doneLoading(params);
    }

    @Override
    public Vector3d getEclipticSphericalCoordinates(Instant date, Vector3d out) {
        AstroUtils.plutoEclipticCoordinates(date, out);
        // To internal units
        out.z *= Constants.KM_TO_U;
        return out;
    }

    @Override
    public Vector3d getEclipticCartesianCoordinates(Instant date, Vector3d out) {
        getEclipticSphericalCoordinates(date, out);
        Coordinates.sphericalToCartesian(out, out);
        return out;
    }

    @Override
    public Vector3d getEquatorialCartesianCoordinates(Instant date, Vector3d out) {
        getEclipticSphericalCoordinates(date, out);
        Coordinates.sphericalToCartesian(out, out);
        out.mul(Coordinates.eclToEq());

        return out;
    }

}