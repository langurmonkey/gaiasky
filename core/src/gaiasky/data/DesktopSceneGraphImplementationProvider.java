/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.SceneGraph;

public class DesktopSceneGraphImplementationProvider extends SceneGraphImplementationProvider {

    @Override
    public ISceneGraph getImplementation(boolean hasOctree, boolean hasStarGroup, int numNodes) {
        // Scene graph concurrent has been deprecated, now all stars are in GPU
        return new SceneGraph(numNodes);
    }

}
