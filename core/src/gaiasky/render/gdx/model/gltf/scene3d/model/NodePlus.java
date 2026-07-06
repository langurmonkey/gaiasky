/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.model;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.gdx.model.IntNode;

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
