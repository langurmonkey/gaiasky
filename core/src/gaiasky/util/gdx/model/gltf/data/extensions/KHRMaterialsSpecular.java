/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.extensions;

import gaiasky.util.gdx.model.gltf.data.material.GLTFMaterial;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFTextureInfo;

public class KHRMaterialsSpecular {
	
	public static final String EXT = "KHR_materials_specular";
	
	public float specularFactor = 1f;
	public GLTFTextureInfo specularTexture = null;
	public float [] specularColorFactor = {1, 1, 1};
	public GLTFTextureInfo specularColorTexture = null;
}
