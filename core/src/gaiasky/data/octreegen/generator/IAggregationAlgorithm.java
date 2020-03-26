/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.octreegen.generator;

import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.util.tree.OctreeNode;

/**
 * Interface to be implemented by all algorithms that create a group of virtual
 * particles for an octant.
 * 
 * @author Toni Sagrista
 *
 */
public interface IAggregationAlgorithm {

    /**
     * Creates the sub-sample from the given input stars. All these stars should
     * be in the box defined by the center and the sizes.
     * 
     * @param inputStars
     *            The actual stars that are inside the octant.
     * @param octant
     *            The octant that characterizes the box with its center, size
     *            and depth well set.
     * @param percentage
     *            The percentage of objects to be included in the octant.
     * @return True if we are in a leaf.
     */
    boolean sample(Array<ParticleBean> inputStars, OctreeNode octant, float percentage);

    /**
     * Gets the maximum number of particles in a single node
     * 
     * @return maximum number of particles in a node
     */
    int getMaxPart();

    /**
     * Returns the number of discarded stars by this algorithm so far.
     * 
     * @return The number of stars discarded.
     */
    int getDiscarded();

    /**
     * Returns the max depth setting.
     * @return The max depth
     */
    int getMaxDepth();

}
