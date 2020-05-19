/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.util.Constants;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

/**
 * Coordinates of the Moon given by the algorithm in Jean Meeus' Astronomical
 * Algorithms book.
 * 
 * @author Toni Sagrista
 *
 */
public class MoonAACoordinates extends AbstractOrbitCoordinates {
    public MoonAACoordinates(){
        super();
    }

    @Override
    public void doneLoading(Object... params) {
        super.doneLoading(params);
    }

    @Override
    public Vector3d getEclipticSphericalCoordinates(Instant date, Vector3d out) {
        if (!Constants.withinVSOPTime(date.toEpochMilli()))
            return null;
        AstroUtils.moonEclipticCoordinates(date, out);
        // To internal units
        out.z *= Constants.KM_TO_U * scaling;
        return out;
    }

    @Override
    public Vector3d getEclipticCartesianCoordinates(Instant date, Vector3d out) {
        Vector3d v = getEclipticSphericalCoordinates(date, out);
        if (v == null)
            return null;
        Coordinates.sphericalToCartesian(out, out);
        return out;
    }

    @Override
    public Vector3d getEquatorialCartesianCoordinates(Instant date, Vector3d out) {
        Vector3d v = getEclipticSphericalCoordinates(date, out);
        if (v == null)
            return null;
        Coordinates.sphericalToCartesian(out, out);
        out.mul(Coordinates.eclToEq());
        return out;
    }

}
