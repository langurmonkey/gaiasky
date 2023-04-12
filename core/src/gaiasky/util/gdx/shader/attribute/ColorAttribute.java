/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.graphics.Color;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;

public class ColorAttribute extends Attribute {
    public final static String DiffuseAlias = "diffuseColor";
    public final static int Diffuse = register(DiffuseAlias);
    public final static String SpecularAlias = "specularColor";
    public final static int Specular = register(SpecularAlias);
    public final static String AmbientAlias = "ambientColor";
    public static final int Ambient = register(AmbientAlias);
    public final static String EmissiveAlias = "emissiveColor";
    public static final int Emissive = register(EmissiveAlias);
    public final static String MetallicAlias = "metallicColor";
    public static final int Metallic = register(MetallicAlias);
    public final static String RoughnessAlias = "roughnessColor";
    public static final int Roughness = register(RoughnessAlias);
    public final static String AmbientLightAlias = "ambientLightColor";
    public static final int AmbientLight = register(AmbientLightAlias);
    public final static String FogAlias = "fogColor";
    public static final int Fog = register(FogAlias);
    public final Color color = new Color();

    public ColorAttribute(final int index) {
        super(index);
    }

    public ColorAttribute(com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute other) {
        super(convertType(other.type));
        color.set(other.color);
    }

    private static int convertType(long oldType) {
        if (oldType == com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Ambient) {
            return Ambient;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Fog) {
            return Fog;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Diffuse) {
            return Diffuse;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Emissive) {
            return Emissive;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.AmbientLight) {
            return AmbientLight;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Reflection) {
            return Metallic;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute.Specular) {
            return Specular;
        } else if (oldType == PBRColorAttribute.BaseColorFactor) {
            return Diffuse;
        }
        return -1;
    }

    public ColorAttribute(final int index, final Color color) {
        this(index);
        if (color != null)
            this.color.set(color);
    }

    public ColorAttribute(final int index, float r, float g, float b, float a) {
        this(index);
        this.color.set(r, g, b, a);
    }

    public ColorAttribute(final int index, float l) {
        this(index);
        this.color.set(l, l, l, 1.0f);
    }

    public ColorAttribute(final ColorAttribute copyFrom) {
        this(copyFrom.index, copyFrom.color);
    }

    public final static ColorAttribute createAmbient(final Color color) {
        return new ColorAttribute(Ambient, color);
    }

    public final static ColorAttribute createAmbient(float r, float g, float b, float a) {
        return new ColorAttribute(Ambient, r, g, b, a);
    }

    public final static ColorAttribute createDiffuse(final Color color) {
        return new ColorAttribute(Diffuse, color);
    }

    public final static ColorAttribute createDiffuse(float r, float g, float b, float a) {
        return new ColorAttribute(Diffuse, r, g, b, a);
    }

    public final static ColorAttribute createSpecular(final Color color) {
        return new ColorAttribute(Specular, color);
    }

    public final static ColorAttribute createSpecular(float r, float g, float b, float a) {
        return new ColorAttribute(Specular, r, g, b, a);
    }

    public final static ColorAttribute createMetallic(final Color color) {
        return new ColorAttribute(Metallic, color);
    }

    public final static ColorAttribute createMetallic(float r, float g, float b, float a) {
        return new ColorAttribute(Metallic, r, g, b, a);
    }

    public final static ColorAttribute createRoughness(final Color color) {
        return new ColorAttribute(Roughness, color);
    }

    public final static ColorAttribute createRoughness(float r, float g, float b, float a) {
        return new ColorAttribute(Roughness, r, g, b, a);
    }


    public final static ColorAttribute createEmissive(final Color color) {
        return new ColorAttribute(Emissive, color);
    }

    public final static ColorAttribute createEmissive(float r, float g, float b, float a) {
        return new ColorAttribute(Emissive, r, g, b, a);
    }

    public final static ColorAttribute createAmbientLight(final Color color) {
        return new ColorAttribute(AmbientLight, color);
    }

    public final static ColorAttribute createAmbientLight(float r, float g, float b, float a) {
        return new ColorAttribute(AmbientLight, r, g, b, a);
    }

    public final static ColorAttribute createFog(final Color color) {
        return new ColorAttribute(Fog, color);
    }

    public final static ColorAttribute createFog(float r, float g, float b, float a) {
        return new ColorAttribute(Fog, r, g, b, a);
    }

    @Override
    public Attribute copy() {
        return new ColorAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 953 * result + color.toIntBits();
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index - o.index;
        return ((ColorAttribute) o).color.toIntBits() - color.toIntBits();
    }
}
