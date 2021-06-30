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
 */
public class SceneGraph extends AbstractSceneGraph {

    int nObjects = -1;

    public SceneGraph(int numNodes) {
        super(numNodes);
    }

    public void update(ITimeFrameProvider time, ICamera camera) {
        root.translation.set(camera.getInversePos());
        root.update(time, null, camera);
        objectsPerThread[0] = root.numChildren;

        if (!hasOctree) {
            if (nObjects < 0)
                nObjects = getNObjects();
            EventManager.instance.post(Events.DEBUG_OBJECTS, nObjects, nObjects);
        }
    }

    public void dispose() {
        super.dispose();
    }

}
