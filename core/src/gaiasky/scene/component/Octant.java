package gaiasky.scene.component;

import com.artemis.Component;
import gaiasky.util.tree.OctreeNode;

public class Octant extends Component {

    /**
     * The id of the octant it belongs to, if any
     **/
    public Long octantId;

    /**
     * Its page
     **/
    public OctreeNode octant;
}
