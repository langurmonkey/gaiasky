package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.tree.OctreeNode;

public class Octant implements Component {

    /**
     * The id of the octant it belongs to, if any
     **/
    public Long octantId;

    /**
     * Its page
     **/
    public OctreeNode octant;
}