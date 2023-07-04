/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.gltf;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFLoaderBase;
import gaiasky.util.gdx.model.gltf.scene3d.scene.SceneAsset;

public class GLTFLoader extends GLTFLoaderBase
{
	public SceneAsset load(FileHandle glFile){
		return load(glFile, false);
	}
	public SceneAsset load(FileHandle glFile, boolean withData){
		SeparatedDataFileResolver dataFileResolver = new SeparatedDataFileResolver();
		dataFileResolver.load(glFile);
		return load(dataFileResolver, withData);
	}

}
