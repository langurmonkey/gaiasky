/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;

public class TextureAttribute extends Attribute {
    public final static String DiffuseAlias = "diffuseTexture";
    public final static int Diffuse = register(DiffuseAlias);
    public final static String SpecularAlias = "specularTexture";
    public final static int Specular = register(SpecularAlias);
    public final static String BumpAlias = "bumpTexture";
    public final static int Bump = register(BumpAlias);
    public final static String NormalAlias = "normalTexture";
    public final static int Normal = register(NormalAlias);
    public final static String AmbientAlias = "ambientTexture";
    public final static int Ambient = register(AmbientAlias);
    public final static String EmissiveAlias = "emissiveTexture";
    public final static int Emissive = register(EmissiveAlias);
    public final static String MetallicAlias = "metallicTexture";
    public final static int Metallic = register(MetallicAlias);
    public final static String AOAlias = "AOTexture";
    public final static int AO = register(AOAlias);
    public final static String RoughnessAlias = "roughnessTexture";
    public final static int Roughness = register(RoughnessAlias);
    public final static String OcclusionMetallicRoughnessAlias = "occlusionMetallicRoughnessTexture";
    public final static int OcclusionMetallicRoughness = register(OcclusionMetallicRoughnessAlias);
    public final static String HeightAlias = "heightTexture";
    public final static int Height = register(HeightAlias);
    public final static String SvtCacheAlias = "svtCacheTexture";
    public final static int SvtCache = register(SvtCacheAlias);
    public final static String SvtIndirectionDiffuseAlias = "svtIndirectionDiffuseTexture";
    public final static int SvtIndirectionDiffuse = register(SvtIndirectionDiffuseAlias);
    public final static String SvtIndirectionHeightAlias = "svtIndirectionHeightTexture";
    public final static int SvtIndirectionHeight = register(SvtIndirectionHeightAlias);
    public final static String SvtIndirectionNormalAlias = "svtIndirectionNormalTexture";
    public final static int SvtIndirectionNormal = register(SvtIndirectionNormalAlias);
    public final static String SvtIndirectionSpecularAlias = "svtIndirectionSpecularTexture";
    public final static int SvtIndirectionSpecular = register(SvtIndirectionSpecularAlias);
    public final static String SvtIndirectionRoughnessAlias = "svtIndirectionRoughnessTexture";
    public final static int SvtIndirectionRoughness = register(SvtIndirectionRoughnessAlias);
    public final static String SvtIndirectionEmissiveAlias = "svtIndirectionEmissiveTexture";
    public final static int SvtIndirectionEmissive = register(SvtIndirectionEmissiveAlias);
    public final static String SvtIndirectionMetallicAlias = "svtIndirectionMetallicTexture";
    public final static int SvtIndirectionMetallic = register(SvtIndirectionMetallicAlias);
    public final TextureDescriptor<Texture> textureDescription;
    public float offsetU = 0;
    public float offsetV = 0;
    public float scaleU = 1;
    public float scaleV = 1;
    /**
     * The index of the texture coordinate vertex attribute to use for this TextureAttribute. Whether this value is used, depends
     * on the shader and {@link Attribute#type} value. For basic (model specific) types (e.g. {@link #Diffuse}, {@link #Normal},
     * etc.), this value is usually ignored and the first texture coordinate vertex attribute is used.
     */
    public int uvIndex = 0;

    public TextureAttribute(final int index) {
        super(index);
        textureDescription = new TextureDescriptor<>();
    }

    public TextureAttribute(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute other) {
        super(convertType(other.type));
        textureDescription = other.textureDescription;
        offsetU = other.offsetU;
        offsetV = other.offsetV;
        scaleU = other.scaleU;
        scaleV = other.scaleV;
        uvIndex = other.uvIndex;
    }

    protected static int convertType(long oldType) {
        if (oldType == PBRTextureAttribute.BaseColorTexture) {
            return Diffuse;
        } else if (oldType == PBRTextureAttribute.EmissiveTexture) {
            return Emissive;
        } else if (oldType == PBRTextureAttribute.NormalTexture) {
            return Normal;
        } else if (oldType == PBRTextureAttribute.SpecularFactorTexture) {
            return Specular;
        } else if (oldType == PBRTextureAttribute.SpecularColorTexture) {
            return Specular;
        } else if (oldType == PBRTextureAttribute.MetallicRoughnessTexture) {
            return OcclusionMetallicRoughness;
        } else if (oldType == PBRTextureAttribute.OcclusionTexture) {
            return AO;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Bump) {
            return Bump;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Ambient) {
            return Ambient;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse) {
            return Diffuse;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Normal) {
            return Normal;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Emissive) {
            return Emissive;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Reflection) {
            return Metallic;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Specular) {
            return Specular;
        }
        return -1;
    }

    public <T extends Texture> TextureAttribute(final int index, final TextureDescriptor<T> textureDescription) {
        this(index);
        this.textureDescription.set(textureDescription);
    }

    public <T extends Texture> TextureAttribute(final int index, final TextureDescriptor<T> textureDescription, float offsetU,
            float offsetV, float scaleU, float scaleV, int uvIndex) {
        this(index, textureDescription);
        this.offsetU = offsetU;
        this.offsetV = offsetV;
        this.scaleU = scaleU;
        this.scaleV = scaleV;
        this.uvIndex = uvIndex;
    }

    public <T extends Texture> TextureAttribute(final int index, final TextureDescriptor<T> textureDescription, float offsetU,
            float offsetV, float scaleU, float scaleV) {
        this(index, textureDescription, offsetU, offsetV, scaleU, scaleV, 0);
    }

    public TextureAttribute(final int index, final Texture texture) {
        this(index);
        textureDescription.texture = texture;
    }

    public TextureAttribute(final int index, final TextureRegion region) {
        this(index);
        set(region);
    }

    public TextureAttribute(final TextureAttribute copyFrom) {
        this(copyFrom.index, copyFrom.textureDescription, copyFrom.offsetU, copyFrom.offsetV, copyFrom.scaleU, copyFrom.scaleV,
                copyFrom.uvIndex);
    }

    public static TextureAttribute createDiffuse(final Texture texture) {
        return new TextureAttribute(Diffuse, texture);
    }

    public static TextureAttribute createDiffuse(final TextureRegion region) {
        return new TextureAttribute(Diffuse, region);
    }

    public static TextureAttribute createSpecular(final Texture texture) {
        return new TextureAttribute(Specular, texture);
    }

    public static TextureAttribute createSpecular(final TextureRegion region) {
        return new TextureAttribute(Specular, region);
    }

    public static TextureAttribute createNormal(final Texture texture) {
        return new TextureAttribute(Normal, texture);
    }

    public static TextureAttribute createNormal(final TextureRegion region) {
        return new TextureAttribute(Normal, region);
    }

    public static TextureAttribute createBump(final Texture texture) {
        return new TextureAttribute(Bump, texture);
    }

    public static TextureAttribute createBump(final TextureRegion region) {
        return new TextureAttribute(Bump, region);
    }

    public static TextureAttribute createAmbient(final Texture texture) {
        return new TextureAttribute(Ambient, texture);
    }

    public static TextureAttribute createAmbient(final TextureRegion region) {
        return new TextureAttribute(Ambient, region);
    }

    public static TextureAttribute createEmissive(final Texture texture) {
        return new TextureAttribute(Emissive, texture);
    }

    public static TextureAttribute createEmissive(final TextureRegion region) {
        return new TextureAttribute(Emissive, region);
    }

    public static TextureAttribute createMetallic(final Texture texture) {
        return new TextureAttribute(Metallic, texture);
    }

    public static TextureAttribute createReflection(final Texture texture) {
        return createMetallic(texture);
    }

    public static TextureAttribute createMetallic(final TextureRegion region) {
        return new TextureAttribute(Metallic, region);
    }

    public static TextureAttribute createReflection(final TextureRegion region) {
        return createMetallic(region);
    }

    public static TextureAttribute createHeight(final Texture texture) {
        return new TextureAttribute(Height, texture);
    }

    public static TextureAttribute createHeight(final TextureRegion region) {
        return new TextureAttribute(Height, region);
    }

    public void set(final TextureRegion region) {
        textureDescription.texture = region.getTexture();
        offsetU = region.getU();
        offsetV = region.getV();
        scaleU = region.getU2() - offsetU;
        scaleV = region.getV2() - offsetV;
    }

    @Override
    public Attribute copy() {
        return new TextureAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 991 * result + textureDescription.hashCode();
        result = 991 * result + NumberUtils.floatToRawIntBits(offsetU);
        result = 991 * result + NumberUtils.floatToRawIntBits(offsetV);
        result = 991 * result + NumberUtils.floatToRawIntBits(scaleU);
        result = 991 * result + NumberUtils.floatToRawIntBits(scaleV);
        result = 991 * result + uvIndex;
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index < o.index ? -1 : 1;
        TextureAttribute other = (TextureAttribute) o;
        final int c = textureDescription.compareTo(other.textureDescription);
        if (c != 0)
            return c;
        if (uvIndex != other.uvIndex)
            return uvIndex - other.uvIndex;
        if (!MathUtils.isEqual(scaleU, other.scaleU))
            return scaleU > other.scaleU ? 1 : -1;
        if (!MathUtils.isEqual(scaleV, other.scaleV))
            return scaleV > other.scaleV ? 1 : -1;
        if (!MathUtils.isEqual(offsetU, other.offsetU))
            return offsetU > other.offsetU ? 1 : -1;
        if (!MathUtils.isEqual(offsetV, other.offsetV))
            return offsetV > other.offsetV ? 1 : -1;
        return 0;
    }
}
