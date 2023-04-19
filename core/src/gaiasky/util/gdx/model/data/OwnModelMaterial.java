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

    public Color ambient;
    public Color diffuse;
    public Color specular;
    public Color emissive;
    public Color reflection;
    public Color metallic;
    public Color roughness;

    public float shininess;
    public float opacity = 1.f;

    public Array<OwnModelTexture> textures;
}
