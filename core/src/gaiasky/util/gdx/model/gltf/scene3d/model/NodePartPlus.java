/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.model;

import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.model.IntNodePart;

public class NodePartPlus extends IntNodePart {

    /**
     * null if no morph targets
     */
    public WeightVector morphTargets;

    public IntRenderable setRenderable(final IntRenderable out) {
        out.material = material;
        out.meshPart.set(meshPart);
        out.bones = bones;
        out.userData = morphTargets;
        return out;
    }

    @Override
    public IntNodePart copy() {
        return new NodePartPlus().set(this);
    }

    @Override
    protected IntNodePart set(IntNodePart other) {
        super.set(other);
        if (other instanceof NodePartPlus) {
            morphTargets = ((NodePartPlus) other).morphTargets;
        }
        return this;
    }
}
