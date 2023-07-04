/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.extensions;

import gaiasky.util.gdx.model.gltf.data.material.GLTFMaterial;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFTextureInfo;

public class KHRMaterialsVolume {
	
	public static final String EXT = "KHR_materials_volume";
	
	public float thicknessFactor = 0f;
	public GLTFTextureInfo thicknessTexture = null;
	public Float attenuationDistance = null; // default +inf.
	public float [] attenuationColor = {1, 1, 1};
}
