package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.tree.OctreeNode;

/** Component that contains a reference to the octree node this object belongs to. **/
public class Octant implements Component {

    /**
     * Its page
     **/
    public OctreeNode octant;
}
