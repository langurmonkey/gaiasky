/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.model.IntMeshPart;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.IntShader;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.Attribute;
import gaiasky.util.gdx.shader.provider.IntShaderProvider;

public class IntRenderable {
    /**
     * Used to specify the transformations (like translation, scale and rotation) to apply to the shape. In other words: it is used
     * to transform the vertices from model space into world space.
     **/
    public final Matrix4 worldTransform = new Matrix4();
    /** The {@link IntMeshPart} that contains the shape to render **/
    public final IntMeshPart meshPart = new IntMeshPart();
    /**
     * The {@link Material} to be applied to the shape (part of the mesh), must not be null.
     *
     * @see #environment
     **/
    public Material material;
    /**
     * The {@link Environment} to be used to render this Renderable, may be null. When specified it will be combined by the shader
     * with the {@link #material}. When both the material and environment contain an attribute of the same type, the attribute of
     * the material will be used.
     **/
    public Environment environment;
    /**
     * The bone transformations used for skinning, or null if not applicable. When specified and the mesh contains one or more
     * {@link VertexAttributes.Usage#BoneWeight} vertex attributes, then the BoneWeight index is used as
     * index in the array. If the array isn't large enough then the identity matrix is used. Each BoneWeight weight is used to
     * combine multiple bones into a single transformation matrix, which is used to transform the vertex to model space. In other
     * words: the bone transformation is applied prior to the {@link #worldTransform}.
     */
    public Matrix4[] bones;
    /**
     * The {@link IntShader} to be used to render this Renderable using a {@link IntModelBatch}, may be null. It is not guaranteed that
     * the shader will be used, the used {@link IntShaderProvider} is responsible for actually choosing the correct shader to use.
     **/
    public IntShader shader;
    /** User definable value, may be null. */
    public Object userData;

    public IntRenderable set(IntRenderable renderable) {
        worldTransform.set(renderable.worldTransform);
        material = renderable.material;
        meshPart.set(renderable.meshPart);
        bones = renderable.bones;
        environment = renderable.environment;
        shader = renderable.shader;
        userData = renderable.userData;
        return this;
    }
}
