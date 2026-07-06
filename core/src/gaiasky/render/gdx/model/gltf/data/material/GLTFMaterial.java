/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.material;

import gaiasky.render.gdx.model.gltf.data.GLTFEntity;
import gaiasky.render.gdx.model.gltf.data.texture.GLTFNormalTextureInfo;
import gaiasky.render.gdx.model.gltf.data.texture.GLTFOcclusionTextureInfo;
import gaiasky.render.gdx.model.gltf.data.texture.GLTFTextureInfo;

public class GLTFMaterial extends GLTFEntity {
	
	public float [] emissiveFactor;

	public GLTFNormalTextureInfo normalTexture;
	public GLTFOcclusionTextureInfo occlusionTexture;
	public GLTFTextureInfo emissiveTexture;
	
	public String alphaMode;
	public Float alphaCutoff;
	
	public Boolean doubleSided;
	
	public GLTFpbrMetallicRoughness pbrMetallicRoughness;
	
}
