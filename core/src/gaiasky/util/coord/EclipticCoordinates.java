/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.util.math.Vector3d;

import java.time.Instant;

public class EclipticCoordinates extends OrbitLintCoordinates {
    @Override
    public Vector3d getEclipticSphericalCoordinates(Instant instant, Vector3d out) {
        return null;
    }

    @Override
    public Vector3d getEquatorialCartesianCoordinates(Instant instant, Vector3d out) {
        boolean inRange = data.loadPoint(out, instant);
        out.rotate(AstroUtils.obliquity(AstroUtils.getJulianDate(instant)), 0, 0, 1);//.mul(Coordinates.equatorialToEcliptic());
        return inRange ? out : null;
    }

}
