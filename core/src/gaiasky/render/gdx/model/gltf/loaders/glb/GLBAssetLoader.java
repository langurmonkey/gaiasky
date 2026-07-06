/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.loaders.glb;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.gdx.model.gltf.loaders.shared.SceneAssetLoaderParameters;
import gaiasky.render.gdx.model.gltf.scene3d.scene.SceneAsset;

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
		boolean withData = parameter != null && parameter.withData;
		return new GLBLoader().load(file, withData);
	}

	@Override
	public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file,
			SceneAssetLoaderParameters parameter) {
		return null;
	}

}
