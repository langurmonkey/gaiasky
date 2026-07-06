/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.data.extensions;

public class KHRTextureTransform {
	public static final String EXT = "KHR_texture_transform";
	
	public float [] offset = {0f, 0f};
	public float rotation;
	public float [] scale = {1f, 1f};
	public Integer texCoord;
}
