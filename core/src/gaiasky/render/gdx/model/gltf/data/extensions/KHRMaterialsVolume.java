/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.extensions;

import gaiasky.render.gdx.model.gltf.data.texture.GLTFTextureInfo;

public class KHRMaterialsVolume {
	
	public static final String EXT = "KHR_materials_volume";
	
	public float thicknessFactor;
	public GLTFTextureInfo thicknessTexture;
	public Float attenuationDistance; // default +inf.
	public float [] attenuationColor = {1, 1, 1};
}
