/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.extensions;

public class KHRMaterialsEmissiveStrength {
	
	public static final String EXT = "KHR_materials_emissive_strength";
	
	public float emissiveStrength = 1f;
}
