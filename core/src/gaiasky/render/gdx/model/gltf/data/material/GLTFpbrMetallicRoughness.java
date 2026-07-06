/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.material;

import gaiasky.render.gdx.model.gltf.data.GLTFObject;
import gaiasky.render.gdx.model.gltf.data.texture.GLTFTextureInfo;

public class GLTFpbrMetallicRoughness extends GLTFObject {
	public float[] baseColorFactor;
	public float metallicFactor = 1;
	public float roughnessFactor = 1;
	public GLTFTextureInfo baseColorTexture, metallicRoughnessTexture;
}
