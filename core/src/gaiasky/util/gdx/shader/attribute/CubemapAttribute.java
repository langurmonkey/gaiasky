/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import gaiasky.util.gdx.OwnCubemap;

public class CubemapAttribute extends Attribute {
    public final static String ReflectionCubemapAlias = "reflectionCubemap";
    public final static int ReflectionCubemap = register(ReflectionCubemapAlias);

    public final static String DiffuseCubemapAlias = "diffuseCubemap";
    public final static int DiffuseCubemap = register(DiffuseCubemapAlias);

    public final static String NormalCubemapAlias = "normalCubemap";
    public final static int NormalCubemap = register(NormalCubemapAlias);

    public final static String EmissiveCubemapAlias = "emissiveCubemap";
    public final static int EmissiveCubemap = register(EmissiveCubemapAlias);

    public final static String SpecularCubemapAlias = "specularCubemap";
    public final static int SpecularCubemap = register(SpecularCubemapAlias);

    public final static String RoughnessCubemapAlias = "roughnessCubemap";
    public final static int RoughnessCubemap = register(RoughnessCubemapAlias);

    public final static String MetallicCubemapAlias = "metallicCubemap";
    public final static int MetallicCubemap = register(MetallicCubemapAlias);

    public final static String HeightCubemapAlias = "heightCubemap";
    public final static int HeightCubemap = register(HeightCubemapAlias);

    public final TextureDescriptor<OwnCubemap> textureDescription;

    public CubemapAttribute(final int index) {
        super(index);
        textureDescription = new TextureDescriptor<>();
    }

    private static int convertType(int oldType) {
        if (oldType == com.badlogic.gdx.graphics.g3d.attributes.CubemapAttribute.EnvironmentMap) {
            return ReflectionCubemap;
        }
        return -1;
    }

    public <T extends OwnCubemap> CubemapAttribute(final int index, final TextureDescriptor<T> textureDescription) {
        this(index);
        this.textureDescription.set(textureDescription);
    }

    public CubemapAttribute(final int index, final OwnCubemap texture) {
        this(index);
        textureDescription.texture = texture;
    }

    public CubemapAttribute(final CubemapAttribute copyFrom) {
        this(copyFrom.index, copyFrom.textureDescription);
    }

    @Override
    public Attribute copy() {
        return new CubemapAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 967 * result + textureDescription.hashCode();
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index - o.index;
        return textureDescription.compareTo(((CubemapAttribute) o).textureDescription);
    }
}
