/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.coord;

import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.HeliotropicOrbit;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.time.Instant;

public class GaiaCoordinates extends AbstractOrbitCoordinates {
    PointCloudData data;

    @Override
    public void doneLoading(Object... params) {
        orbitname = "Gaia orbit";
        orbit = (HeliotropicOrbit) ((ISceneGraph) params[0]).getNode(orbitname.toLowerCase().trim());
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
        out.rotate(AstroUtils.getSunLongitude(date) + 180, 0, 1, 0).mul(Coordinates.eclToEq());
        return inRange ? out : null;
    }

}
