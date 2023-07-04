/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.glb;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFLoaderBase;
import gaiasky.util.gdx.model.gltf.scene3d.scene.SceneAsset;

public class GLBLoader extends GLTFLoaderBase {

	public SceneAsset load(FileHandle file){
		return load(file, false);
	}
	public SceneAsset load(FileHandle file, boolean withData){
		BinaryDataFileResolver dataFileResolver = new BinaryDataFileResolver();
		dataFileResolver.load(file);
		return load(dataFileResolver, withData);
	}
	
	public SceneAsset load(byte[] bytes) {
		return load(bytes, false);
	}
	public SceneAsset load(byte[] bytes, boolean withData) {
		BinaryDataFileResolver dataFileResolver = new BinaryDataFileResolver();
		dataFileResolver.load(bytes);
		return load(dataFileResolver, withData);
	}
}
