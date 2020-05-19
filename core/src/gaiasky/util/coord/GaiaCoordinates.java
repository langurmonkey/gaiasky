/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.data.util.PointCloudData;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.HeliotropicOrbit;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

public class GaiaCoordinates extends AbstractOrbitCoordinates {
    PointCloudData data;

    public GaiaCoordinates(){
        super();
    }

    @Override
    public void doneLoading(Object... params) {
        orbitname = "Gaia orbit";
        orbit = (HeliotropicOrbit) ((ISceneGraph) params[0]).getNode(orbitname);
        if (params[1] instanceof CelestialBody)
            orbit.setBody((CelestialBody) params[1]);
        data = orbit.getPointCloud();
    }

    @Override
    public Vector3d getEclipticCartesianCoordinates(Instant date, Vector3d out) {
        return null;
    }

    @Override
    public Vector3d getEclipticSphericalCoordinates(Instant date, Vector3d out) {
        return null;
    }

    @Override
    public Vector3d getEquatorialCartesianCoordinates(Instant date, Vector3d out) {
        boolean inRange = data.loadPoint(out, date);
        // Rotate by solar longitude, and convert to equatorial.
        out.rotate(AstroUtils.getSunLongitude(date) + 180, 0, 1, 0).mul(Coordinates.eclToEq()).scl(scaling);
        return inRange ? out : null;
    }

}
