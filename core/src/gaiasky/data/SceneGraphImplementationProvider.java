/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import gaiasky.scenegraph.ISceneGraph;

/**
 * Provides the scene graph implementation.
 */
public abstract class SceneGraphImplementationProvider {
    public static SceneGraphImplementationProvider provider;

    public static void initialize(SceneGraphImplementationProvider provider) {
        SceneGraphImplementationProvider.provider = provider;
    }

    /**
     * Gets the right scene graph implementation for the given information about
     * it.
     *
     * @param hasOctree      Does it have an octree?
     * @param hasStarGroup   Does it contain a star group?
     * @param numNodes       Initial number of nodes.
     * @return The scene graph.
     */
    public abstract ISceneGraph getImplementation(boolean hasOctree, boolean hasStarGroup, int numNodes);

}
