package gaia.cu9.ari.gaiaorbit.util.coord;

import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.Orbit;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

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
