/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.model;

import gaiasky.render.gdx.IntRenderable;
import gaiasky.render.gdx.model.IntNodePart;

public class NodePartPlus extends IntNodePart {

    /**
     * null if no morph targets
     */
    public WeightVector morphTargets;

    public IntRenderable setRenderable(IntRenderable out) {
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
