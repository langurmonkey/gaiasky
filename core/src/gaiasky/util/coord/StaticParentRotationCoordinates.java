/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.GraphNode;
import gaiasky.scenegraph.ModelBody;
import gaiasky.scenegraph.Orbit;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.Constants;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

public class StaticParentRotationCoordinates implements IBodyCoordinates {

    // TODO remove when ready
    ModelBody parent;
    Entity parentEntity;
    Vector3d position;
    Matrix4d trf;

    public StaticParentRotationCoordinates() {
        super();
        trf = new Matrix4d();
    }

    @Override
    public void doneLoading(Object... params) {
        if(params[1] instanceof SceneGraphNode) {
            SceneGraphNode me = (SceneGraphNode) params[1];
            if (me.parent != null && me.parent instanceof ModelBody) {
                parent = (ModelBody) me.parent;
            }
        } else if(params[1] instanceof GraphNode) {
            GraphNode gn = (GraphNode) params[1];
            if (gn.parent != null && Mapper.model.has(gn.parent)) {
                parentEntity = gn.parent;
            }
        }
    }

    @Override
    public Vector3b getEclipticSphericalCoordinates(Instant date, Vector3b out) {
        return getEquatorialCartesianCoordinates(date, out);
    }

    @Override
    public Vector3b getEclipticCartesianCoordinates(Instant date, Vector3b out) {
        return getEquatorialCartesianCoordinates(date, out);
    }

    @Override
    public Vector3b getEquatorialCartesianCoordinates(Instant date, Vector3b out) {
        out.set(position);
        RotationComponent rc;
        if(parent != null) {
            rc = parent.rc;
        } else {
            rc = Mapper.rotation.get(parentEntity).rc;
        }
        if (rc != null) {
            out.rotate((float) rc.ascendingNode, 0, 1, 0).rotate((float) (rc.inclination + rc.axialTilt), 0, 0, 1).rotate((float) rc.angle, 0, 1, 0);
        }

        return out;
    }

    public void setPosition(double[] position) {
        this.position = new Vector3d(position[0] * Constants.KM_TO_U, position[1] * Constants.KM_TO_U, position[2] * Constants.KM_TO_U);
    }

    @Override
    public Orbit getOrbitObject() {
        return null;
    }

}
