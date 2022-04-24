package gaiasky.util.tree;

import gaiasky.util.math.Vector3b;

public interface IOctreeObject {

    Vector3b getPosition();
    int getStarCount();
    void dispose();

}
