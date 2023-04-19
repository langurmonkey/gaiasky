/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.shader.provider.AtmosphereShaderProvider;

public class AtmosphereShaderProviderLoader<T extends AtmosphereShaderProviderLoader.AtmosphereShaderProviderParameter> extends AsynchronousAssetLoader<AtmosphereShaderProvider, T> {

    AtmosphereShaderProvider shaderProvider;

    public AtmosphereShaderProviderLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, T parameter) {
        shaderProvider = new AtmosphereShaderProvider(Gdx.files.internal(parameter.vertexShader), Gdx.files.internal(parameter.fragmentShader));
    }

    @Override
    public AtmosphereShaderProvider loadSync(AssetManager manager, String fileName, FileHandle file, AtmosphereShaderProviderParameter parameter) {
        return shaderProvider;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, AtmosphereShaderProviderParameter parameter) {
        return null;
    }

    public static class AtmosphereShaderProviderParameter extends AssetLoaderParameters<AtmosphereShaderProvider> {
        String vertexShader;
        String fragmentShader;

        public AtmosphereShaderProviderParameter(String vertexShader, String fragmentShader) {
            super();
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }

    }

}
