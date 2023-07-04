/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.model.gltf.data.animation.GLTFAnimation;
import gaiasky.util.gdx.model.gltf.data.camera.GLTFCamera;
import gaiasky.util.gdx.model.gltf.data.data.GLTFAccessor;
import gaiasky.util.gdx.model.gltf.data.data.GLTFBuffer;
import gaiasky.util.gdx.model.gltf.data.data.GLTFBufferView;
import gaiasky.util.gdx.model.gltf.data.geometry.GLTFMesh;
import gaiasky.util.gdx.model.gltf.data.material.GLTFMaterial;
import gaiasky.util.gdx.model.gltf.data.scene.GLTFNode;
import gaiasky.util.gdx.model.gltf.data.scene.GLTFScene;
import gaiasky.util.gdx.model.gltf.data.scene.GLTFSkin;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFImage;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFSampler;
import gaiasky.util.gdx.model.gltf.data.texture.GLTFTexture;

public class GLTF extends GLTFObject {
	public GLTFAsset asset;
	public int scene;
	public Array<GLTFScene> scenes;
	public Array<GLTFNode> nodes;
	public Array<GLTFCamera> cameras;
	public Array<GLTFMesh> meshes;
	
	public Array<GLTFImage> images;
	public Array<GLTFSampler> samplers;
	public Array<GLTFTexture> textures;
	
	public Array<GLTFAnimation> animations;
	public Array<GLTFSkin> skins;

	public Array<GLTFAccessor> accessors;
	public Array<GLTFMaterial> materials;
	public Array<GLTFBufferView> bufferViews;
	public Array<GLTFBuffer> buffers;
	
	public Array<String> extensionsUsed;
	public Array<String> extensionsRequired;
}
