/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.loader.GLTFWrapperLoader.GLTFLoaderParameters;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.gltf.loaders.gltf.GLTFAssetLoader;
import gaiasky.util.gdx.model.gltf.loaders.shared.SceneAssetLoaderParameters;
import gaiasky.util.gdx.model.gltf.scene3d.scene.SceneAsset;

public class GLTFWrapperLoader extends AsynchronousAssetLoader<IntModel, GLTFLoaderParameters> {

    private final GLTFAssetLoader gltfAssetLoader;

    public GLTFWrapperLoader(FileHandleResolver resolver) {
        super(resolver);
        gltfAssetLoader = new GLTFAssetLoader(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, GLTFLoaderParameters parameter) {
        gltfAssetLoader.loadAsync(manager, fileName, file, convertParameters(parameter));
    }

    @Override
    public IntModel loadSync(AssetManager manager, String fileName, FileHandle file, GLTFLoaderParameters parameter) {
        SceneAsset scene = gltfAssetLoader.loadSync(manager, fileName, file, convertParameters(parameter));
        return scene.scene.model;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, GLTFLoaderParameters parameter) {
        return gltfAssetLoader.getDependencies(fileName, file, convertParameters(parameter));
    }

    public SceneAssetLoaderParameters convertParameters(GLTFLoaderParameters parameter) {
        if (parameter == null) {
            return null;
        }
        var result = new SceneAssetLoaderParameters();
        result.withData = parameter.withData;
        return result;
    }

    public static class GLTFLoaderParameters extends AssetLoaderParameters<IntModel> {
        public boolean withData = false;
    }
}
