package gaiasky.scene.component;

import com.artemis.Component;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;

public class GraphNode extends Component {
    /**
     * The parent entity.
     */
    public SceneGraphNode parent;

    /**
     * List of children entities.
     */
    public Array<SceneGraphNode> children;

    /**
     * Cumulative translation object. In contrast with the position, which contains
     * the position relative to the parent, this contains the absolute position in the
     * internal reference system.
     */
    public Vector3b translation;

    /**
     * Local transform matrix. Contains the transform matrix and the
     * transformations that will be applied to this object and not to its
     * children.
     */
    public Matrix4 localTransform;

    /**
     * This transform stores only the orientation of the object. For example in
     * planets, it stores their orientation with respect to their equatorial
     * plane, but not other transformations applied to the object such as the
     * size or the rotation angle at the time.
     */
    public Matrix4d orientation;

    /**
     * The total number of descendants under this node.
     */
    public int numChildren;
}
