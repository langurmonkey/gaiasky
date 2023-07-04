/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import gaiasky.util.gdx.OwnCubemap;
import gaiasky.util.gdx.shader.attribute.Attribute;
import gaiasky.util.gdx.shader.attribute.CubemapAttribute;

public class PBRCubemapAttribute extends CubemapAttribute {
    public final static String DiffuseEnvAlias = "DiffuseEnvSampler";
    public final static int DiffuseEnv = register(DiffuseEnvAlias);

    public final static String SpecularEnvAlias = "SpecularEnvSampler";
    public final static int SpecularEnv = register(SpecularEnvAlias);

    public PBRCubemapAttribute(int index, TextureDescriptor<OwnCubemap> textureDescription) {
        super(index, textureDescription);
    }

    public PBRCubemapAttribute(int index, OwnCubemap cubemap) {
        super(index, cubemap);
    }

    public static Attribute createDiffuseEnv(OwnCubemap diffuseCubemap) {
        return new PBRCubemapAttribute(DiffuseEnv, diffuseCubemap);
    }

    public static Attribute createSpecularEnv(OwnCubemap specularCubemap) {
        return new PBRCubemapAttribute(SpecularEnv, specularCubemap);
    }

    @Override
    public Attribute copy() {
        return new PBRCubemapAttribute(index, textureDescription);
    }
}
