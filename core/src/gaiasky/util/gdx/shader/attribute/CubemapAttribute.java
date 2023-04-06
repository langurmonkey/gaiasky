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

import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import gaiasky.util.gdx.OwnCubemap;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;

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

    public CubemapAttribute(com.badlogic.gdx.graphics.g3d.attributes.CubemapAttribute other) {
        super(convertType(other.type));
        textureDescription = new TextureDescriptor<>();
        if (other.textureDescription.texture != null) {
            textureDescription.texture = new OwnCubemap(other.textureDescription.texture.getCubemapData(),
                    other.textureDescription.texture.getMinFilter(),
                    other.textureDescription.texture.getMagFilter());
        }
    }

    private static int convertType(long oldType) {
        if (oldType == com.badlogic.gdx.graphics.g3d.attributes.CubemapAttribute.EnvironmentMap) {
            return ReflectionCubemap;
        } else if (oldType == PBRCubemapAttribute.DiffuseEnv) {
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
