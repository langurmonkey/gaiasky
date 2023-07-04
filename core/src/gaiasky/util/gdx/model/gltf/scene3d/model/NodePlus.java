/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.model;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.model.IntNode;

public class NodePlus extends IntNode {
    /**
     * Null if no morph targets.
     */
    public WeightVector weights;

    /**
     * Optional morph target names (e.g. exported from Blender with custom properties enabled).
     * shared with others nodes with same mesh.
     */
    public Array<String> morphTargetNames;

    @Override
    public IntNode copy() {
        return new NodePlus().set(this);
    }

    @Override
    protected IntNode set(IntNode other) {
        if (other instanceof NodePlus) {
            if (((NodePlus) other).weights != null) {
                weights = ((NodePlus) other).weights.cpy();
                morphTargetNames = ((NodePlus) other).morphTargetNames;
            }
        }
        return super.set(other);
    }
}
