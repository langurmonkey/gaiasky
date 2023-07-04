/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.extensions;

public class KHRTextureTransform {
	public static final String EXT = "KHR_texture_transform";
	
	public float [] offset = {0f, 0f};
	public float rotation = 0f;
	public float [] scale = {1f, 1f};
	public Integer texCoord;
}
