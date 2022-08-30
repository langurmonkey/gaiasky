/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.util.math.Vector3b;

import java.time.Instant;

public class HeliotropicOrbitCoordinates extends AbstractOrbitCoordinates {

    public HeliotropicOrbitCoordinates() {
        super();
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant date, Vector3b out) {
        return null;
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out) {
        return null;
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant date, Vector3b out) {
        boolean inRange = getData().loadPoint(out, date);
        if(!inRange) {
            return null;
        }
        // Rotate by solar longitude, and convert to equatorial.
        return out.rotate(AstroUtils.getSunLongitude(date) + 180, 0, 1, 0).mul(Coordinates.eclToEq()).scl(scaling);
    }

}
