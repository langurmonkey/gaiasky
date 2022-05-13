package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.Mapper;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;

public class GraphNode implements Component {

    /**
     * The first name of the parent object.
     */
    public String parentName = null;

    /**
     * The parent entity.
     */
    public Entity parent;

    /**
     * List of children entities.
     */
    public Array<Entity> children;

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

    /**
     * Sets the name of the parent.
     * @param parentName The parent name.
     */
    public void setParent(String parentName) {
        this.parentName = parentName;
    }

    public int getSceneGraphDepth() {
        if (this.parent == null) {
            return 0;
        } else {
            return Mapper.graph.get(this.parent).getSceneGraphDepth() + 1;
        }
    }

}
