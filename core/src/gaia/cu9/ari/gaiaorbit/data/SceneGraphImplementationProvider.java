/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data;

import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;

/**
 * Provides the scene graph implementation.
 * 
 * @author tsagrista
 *
 */
public abstract class SceneGraphImplementationProvider {
    public static SceneGraphImplementationProvider provider;

    public static void initialize(SceneGraphImplementationProvider provider) {
        SceneGraphImplementationProvider.provider = provider;
    }

    /**
     * Gets the right scene graph implementation for the given information about
     * it
     * 
     * @param multithreading
     *            Multithreading on?
     * @param hasOctree
     *            Does it have an octree?
     * @param hasStarGroup
     *            Does it contain a star gorup?
     * @param maxThreads
     *            Max number of threads
     * @return The scene graph
     */
    public abstract ISceneGraph getImplementation(boolean multithreading, boolean hasOctree, boolean hasStarGroup, int maxThreads);

}
