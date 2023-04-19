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
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.loader.GLBWrapperLoader.GLBLoaderParameters;
import gaiasky.util.gdx.model.IntModel;
import net.mgsx.gltf.loaders.glb.GLBAssetLoader;
import net.mgsx.gltf.loaders.shared.SceneAssetLoaderParameters;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class GLBWrapperLoader extends AsynchronousAssetLoader<IntModel, GLBLoaderParameters> {

    private final GLBAssetLoader glbAssetLoader;

    public GLBWrapperLoader(FileHandleResolver resolver) {
        super(resolver);
        glbAssetLoader = new GLBAssetLoader(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, GLBLoaderParameters parameter) {
        glbAssetLoader.loadAsync(manager, fileName, file, convertParameters(parameter));
    }

    @Override
    public IntModel loadSync(AssetManager manager, String fileName, FileHandle file, GLBLoaderParameters parameter) {
        SceneAsset scene = glbAssetLoader.loadSync(manager, fileName, file, convertParameters(parameter));

        // Convert to IntModel.
        Model model = scene.scene.model;

        return new IntModel(model);
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, GLBLoaderParameters parameter) {
        return glbAssetLoader.getDependencies(fileName, file, convertParameters(parameter));
    }

    public SceneAssetLoaderParameters convertParameters(GLBLoaderParameters parameter) {
        if (parameter == null) {
            return null;
        }
        var result = new SceneAssetLoaderParameters();
        result.withData = parameter.withData;
        return result;
    }

    public static class GLBLoaderParameters extends AssetLoaderParameters<IntModel> {
        public boolean withData = false;
    }
}
