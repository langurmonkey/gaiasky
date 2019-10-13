/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.Orbit;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

public abstract class AbstractOrbitCoordinates implements IBodyCoordinates {
    protected static final Log logger = Logger.getLogger(AbstractOrbitCoordinates.class);
    
    protected String orbitname;
    protected Orbit orbit;

    @Override
    public void doneLoading(Object... params) {
        if (params.length == 0) {
            logger.error(new RuntimeException("OrbitLintCoordinates need the scene graph"));
        } else {
            if (orbitname != null && !orbitname.isEmpty()) {
                SceneGraphNode sgn = ((ISceneGraph) params[0]).getNode(orbitname);
                orbit = (Orbit) sgn;
                if (params[1] instanceof CelestialBody)
                    orbit.setBody((CelestialBody) params[1]);
            }
        }
    }

    public void setOrbitname(String orbitname) {
        this.orbitname = orbitname;
    }

    @Override
    public Orbit getOrbitObject() {
        return orbit;
    }

}
