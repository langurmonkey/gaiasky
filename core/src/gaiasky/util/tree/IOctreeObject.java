package gaiasky.util.tree;

import gaiasky.util.math.Vector3b;

/**
 * Describes the interface for all objects added to an octant in an octree.
 */
public interface IOctreeObject {

    Vector3b getPosition();

    int getStarCount();

    void dispose();

}
