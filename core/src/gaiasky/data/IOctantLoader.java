package gaiasky.data;

import gaiasky.util.tree.OctreeNode;

public interface IOctantLoader {
    void queue(OctreeNode octant);

    void clearQueue();

    void touch(OctreeNode octant);

    int getLoadQueueSize();

    int getNLoadedStars();
}
