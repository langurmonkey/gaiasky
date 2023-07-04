/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.glb;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.model.gltf.loaders.shared.SceneAssetLoaderParameters;
import gaiasky.util.gdx.model.gltf.scene3d.scene.SceneAsset;

public class GLBAssetLoader  extends AsynchronousAssetLoader<SceneAsset, SceneAssetLoaderParameters>{

	public GLBAssetLoader() {
		this(new InternalFileHandleResolver());
	}
	public GLBAssetLoader(FileHandleResolver resolver) {
		super(resolver);
	}

	@Override
	public void loadAsync(AssetManager manager, String fileName, FileHandle file,
			SceneAssetLoaderParameters parameter) {
	}

	@Override
	public SceneAsset loadSync(AssetManager manager, String fileName, FileHandle file,
			SceneAssetLoaderParameters parameter) {
		final boolean withData = parameter != null && parameter.withData;
		return new GLBLoader().load(file, withData);
	}

	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file,
			SceneAssetLoaderParameters parameter) {
		return null;
	}

}
