/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.loader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.utils.Array;

public class DefaultShaderProviderLoader<T extends DefaultShaderProviderLoader.DefaultShaderProviderParameter> extends AsynchronousAssetLoader<DefaultShaderProvider, T> {

    DefaultShaderProvider shaderProvider;

    public DefaultShaderProviderLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, T parameter) {
        shaderProvider = new DefaultShaderProvider(Gdx.files.internal(parameter.vertexShader), Gdx.files.internal(parameter.fragmentShader));
    }

    @Override
    public DefaultShaderProvider loadSync(AssetManager manager, String fileName, FileHandle file, DefaultShaderProviderParameter parameter) {
        return shaderProvider;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, DefaultShaderProviderParameter parameter) {
        return null;
    }

    public static class DefaultShaderProviderParameter extends AssetLoaderParameters<DefaultShaderProvider> {
        String vertexShader;
        String fragmentShader;

        public DefaultShaderProviderParameter(String vertexShader, String fragmentShader) {
            super();
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }

    }

}
