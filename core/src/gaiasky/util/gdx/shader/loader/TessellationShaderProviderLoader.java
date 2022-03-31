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
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.shader.provider.TessellationShaderProvider;

public class TessellationShaderProviderLoader<T extends TessellationShaderProviderLoader.TessellationShaderProviderParameter> extends AsynchronousAssetLoader<TessellationShaderProvider, T> {

    TessellationShaderProvider shaderProvider;

    public TessellationShaderProviderLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, T parameter) {
        shaderProvider = new TessellationShaderProvider(Gdx.files.internal(parameter.vertexShader), Gdx.files.internal(parameter.controlShader), Gdx.files.internal(parameter.evaluationShader), Gdx.files.internal(parameter.fragmentShader));
    }

    @Override
    public TessellationShaderProvider loadSync(AssetManager manager, String fileName, FileHandle file, TessellationShaderProviderParameter parameter) {
        return shaderProvider;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, TessellationShaderProviderParameter parameter) {
        return null;
    }

    public static class TessellationShaderProviderParameter extends AssetLoaderParameters<TessellationShaderProvider> {
        String vertexShader;
        String controlShader;
        String evaluationShader;
        String fragmentShader;

        public TessellationShaderProviderParameter(String vertexShader, String controlShader, String evaluationShader, String fragmentShader) {
            super();
            this.vertexShader = vertexShader;
            this.controlShader = controlShader;
            this.evaluationShader = evaluationShader;
            this.fragmentShader = fragmentShader;
        }

    }

}
