/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.extensions;

import gaiasky.render.gdx.model.gltf.data.texture.GLTFTextureInfo;

public class KHRMaterialsIridescence {
	
	public static final String EXT = "KHR_materials_iridescence";
	
	public float iridescenceFactor;
	public GLTFTextureInfo iridescenceTexture;
	public float iridescenceIor = 1.3f;
	public float iridescenceThicknessMinimum = 100;
	public float iridescenceThicknessMaximum = 400;
	public GLTFTextureInfo iridescenceThicknessTexture;
}
