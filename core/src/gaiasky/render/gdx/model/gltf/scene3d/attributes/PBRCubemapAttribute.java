/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import gaiasky.render.gdx.OwnCubemap;
import gaiasky.render.gdx.shader.attribute.Attribute;
import gaiasky.render.gdx.shader.attribute.CubemapAttribute;

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
