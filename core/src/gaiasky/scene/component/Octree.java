package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.util.tree.IOctreeObject;
import gaiasky.util.tree.OctreeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A component that defines an octree structure.
 * The root of the octree is in the component {@link Octant}.
 */
public class Octree implements Component {

    /** The list with the currently observed objects. **/
    public List<IOctreeObject> roulette;

    /** Map with the parent for each node. **/
    public Map<Entity, OctreeNode> parenthood;

    /** Is this just a copy? */
    public boolean copy = false;

    /** Creates an empty octree. **/
    public Octree() {
        this.parenthood = new HashMap<>();
    }

    public void removeParenthood(SceneGraphNode child) {
        parenthood.remove(child);
    }
}
