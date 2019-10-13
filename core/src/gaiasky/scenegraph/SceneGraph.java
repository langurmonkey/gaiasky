/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.time.ITimeFrameProvider;

/**
 * Implementation of a 3D scene graph.
 *
 * @author Toni Sagrista
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
            EventManager.instance.post(Events.DEBUG_OBJECTS, nobjects, nobjects);
        }
    }

    public void dispose() {
        super.dispose();
    }

}
