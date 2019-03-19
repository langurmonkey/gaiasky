package gaia.cu9.ari.gaiaorbit.util.override;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import gaia.cu9.ari.gaiaorbit.assets.ShaderTemplatingLoader;

public class GroundShaderProvider extends DefaultShaderProvider {
    public final GroundShader.Config config;

    public GroundShaderProvider(final GroundShader.Config config) {
        this.config = (config == null) ? new GroundShader.Config() : config;
    }

    public GroundShaderProvider(final String vertexShader, final String fragmentShader) {
        this(new GroundShader.Config(vertexShader, fragmentShader));
    }

    public GroundShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    public GroundShaderProvider() {
        this(null);
    }

    @Override
    protected Shader createShader(final Renderable renderable) {
        return new GroundShader(renderable, config);
    }
}
