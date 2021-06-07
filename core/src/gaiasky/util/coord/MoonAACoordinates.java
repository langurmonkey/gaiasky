/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.util.Constants;
import gaiasky.util.math.Vector3b;
import org.apfloat.Apfloat;

import java.time.Instant;

/**
 * Coordinates of the Moon given by the algorithm in Jean Meeus' Astronomical
 * Algorithms book.
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
    public Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out) {
        if (!Constants.withinVSOPTime(date.toEpochMilli()))
            return null;
        AstroUtils.moonEclipticCoordinates(date, out);
        // To internal units
        out.z = out.z.multiply(new Apfloat(Constants.KM_TO_U * scaling, Constants.PREC));
        return out;
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant date, Vector3b out) {
        Vector3b v = getEclipticSphericalCoordinates(date, out);
        if (v == null)
            return null;
        Coordinates.sphericalToCartesian(out, out);
        return out;
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant date, Vector3b out) {
        Vector3b v = getEclipticSphericalCoordinates(date, out);
        if (v == null)
            return null;
        Coordinates.sphericalToCartesian(out, out);
        out.mul(Coordinates.eclToEq());
        return out;
    }

}
