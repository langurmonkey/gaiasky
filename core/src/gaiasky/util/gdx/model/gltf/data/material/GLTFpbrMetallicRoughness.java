/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.material;

import gaiasky.util.gdx.model.gltf.data.GLTFObject;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFTextureInfo;

public class GLTFpbrMetallicRoughness extends GLTFObject {
	public float[] baseColorFactor;
	public float metallicFactor = 1;
	public float roughnessFactor = 1;
	public GLTFTextureInfo baseColorTexture, metallicRoughnessTexture;
}
