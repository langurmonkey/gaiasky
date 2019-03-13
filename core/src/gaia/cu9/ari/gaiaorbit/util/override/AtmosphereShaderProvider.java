package gaia.cu9.ari.gaiaorbit.util.override;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;

import gaia.cu9.ari.gaiaorbit.assets.ShaderTemplatingLoader;

public class AtmosphereShaderProvider extends DefaultShaderProvider {
    public final AtmosphereShader.Config config;

    public AtmosphereShaderProvider(final AtmosphereShader.Config config) {
        this.config = (config == null) ? new AtmosphereShader.Config() : config;
    }

    public AtmosphereShaderProvider(final String vertexShader, final String fragmentShader) {
        this(new AtmosphereShader.Config(vertexShader, fragmentShader));
    }

    public AtmosphereShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    @Override
    protected Shader createShader(final Renderable renderable) {
        return new AtmosphereShader(renderable, config);
    }

}
