/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.model.data;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

public class OwnModelMaterial {
    public enum MaterialType {
        Lambert,
        Phong,
        PBR
    }

    public String id;

    public MaterialType type;

    public Color ambientColor;
    public Color diffuseColor;
    public Color specularColor;
    public Color emissiveColor;
    public Color metallicColor;
    public Color roughnessColor;
    public Color reflectionColor;

    public float metallic;
    public float roughness;
    public float ior;
    public float shininess;
    public float opacity = 1.0f;

    public Array<OwnModelTexture> textures;
}
