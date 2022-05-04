/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import gaiasky.data.util.PointCloudData;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.Trajectory;
import gaiasky.scene.component.Verts;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.HeliotropicOrbit;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.SceneGraph;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

public class HeliotropicOrbitCoordinates extends AbstractOrbitCoordinates {

    public HeliotropicOrbitCoordinates() {
        super();
    }

    @Override
    public void doneLoading(Object... params) {
        if (params[0] instanceof SceneGraph) {
            orbit = (HeliotropicOrbit) ((ISceneGraph) params[0]).getNode(orbitname);
        } else if (params[0] instanceof Scene) {
            entity = ((Scene) params[0]).getNode(orbitname);
        }
        if (params[1] instanceof CelestialBody) {
            orbit.setBody((CelestialBody) params[1]);
        } else if (params[1] instanceof Entity) {
            Trajectory trajectory = Mapper.trajectory.get(entity);
            if(trajectory != null) {
                trajectory.body = (Entity) params[1];
            }
        }
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
        // Rotate by solar longitude, and convert to equatorial.
        Vector3d outd = new Vector3d();
        out.put(outd);
        outd.rotate(AstroUtils.getSunLongitude(date) + 180, 0, 1, 0).mul(Coordinates.eclToEq()).scl(scaling);
        return inRange ? out.set(outd) : null;
    }

}
