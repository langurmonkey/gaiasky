package gaia.cu9.ari.gaiaorbit.scenegraph;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * Implementation of a 3D scene graph.
 * 
 * @author Toni Sagrista
 *
 */
public class SceneGraph extends AbstractSceneGraph {

    int nobjects = -1;

    public SceneGraph() {
        super();
    }

    public void update(ITimeFrameProvider time, ICamera camera) {
        super.update(time, camera);

        root.translation.set(camera.getInversePos());
        root.update(time, null, camera);
        objectsPerThread[0] = root.numChildren;

        if (!hasOctree) {
            if (nobjects < 0)
                nobjects = getNObjects();
            EventManager.instance.post(Events.DEBUG3, "Objects: " + nobjects);
        }
    }

    public void dispose() {
        super.dispose();
    }

}
