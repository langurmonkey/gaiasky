/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.model;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ArrayMap;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.Material;

public class IntNodePart {
    /** The MeshPart (shape) to render. Must not be null. */
    public IntMeshPart meshPart;
    /** The Material used to render the {@link #meshPart}. Must not be null. */
    public Material material;
    /**
     * Mapping to each bone (node) and the inverse transform of the bind pose. Will be used to fill the {@link #bones} array. May
     * be null.
     */
    public ArrayMap<IntNode, Matrix4> invBoneBindTransforms;
    /**
     * The current transformation (relative to the bind pose) of each bone, may be null. When the part is skinned, this will be
     * updated by a call to {@link IntModelInstance#calculateTransforms()}. Do not set or change this value manually.
     */
    public Matrix4[] bones;
    /** true by default. If set to false, this part will not participate in rendering and bounding box calculation. */
    public boolean enabled = true;

    /**
     * Construct a new NodePart with null values. At least the {@link #meshPart} and {@link #material} member must be set before
     * the newly created part can be used.
     */
    public IntNodePart() {
    }

    /**
     * Construct a new NodePart referencing the provided {@link IntMeshPart} and {@link Material}.
     *
     * @param meshPart The MeshPart to reference.
     * @param material The Material to reference.
     */
    public IntNodePart(final IntMeshPart meshPart, final Material material) {
        this.meshPart = meshPart;
        this.material = material;
    }

    // FIXME add copy constructor and override #equals.

    /**
     * Convenience method to set the material, mesh, meshPartOffset, meshPartSize, primitiveType and bones members of the specified
     * Renderable. The other member of the provided {@link IntRenderable} remain untouched. Note that the material, mesh and bones
     * members are referenced, not copied. Any changes made to those objects will be reflected in both the NodePart and Renderable
     * object.
     *
     * @param out The Renderable of which to set the members to the values of this NodePart.
     */
    public IntRenderable setRenderable(final IntRenderable out) {
        out.material = material;
        out.meshPart.set(meshPart);
        out.bones = bones;
        return out;
    }

    public IntNodePart copy() {
        return new IntNodePart().set(this);
    }

    protected IntNodePart set(IntNodePart other) {
        meshPart = new IntMeshPart(other.meshPart);
        material = other.material;
        enabled = other.enabled;
        if (other.invBoneBindTransforms == null) {
            invBoneBindTransforms = null;
            bones = null;
        } else {
            if (invBoneBindTransforms == null)
                invBoneBindTransforms = new ArrayMap<>(true, other.invBoneBindTransforms.size, IntNode[]::new, Matrix4[]::new);
            else
                invBoneBindTransforms.clear();
            invBoneBindTransforms.putAll(other.invBoneBindTransforms);

            if (bones == null || bones.length != invBoneBindTransforms.size)
                bones = new Matrix4[invBoneBindTransforms.size];

            for (int i = 0; i < bones.length; i++) {
                if (bones[i] == null)
                    bones[i] = new Matrix4();
            }
        }
        return this;
    }
}
