/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.material;

import gaiasky.util.gdx.model.gltf.data.GLTFEntity;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFNormalTextureInfo;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFOcclusionTextureInfo;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFTextureInfo;

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
