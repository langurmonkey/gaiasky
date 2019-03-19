package gaia.cu9.ari.gaiaorbit.util.override;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import gaia.cu9.ari.gaiaorbit.assets.ShaderTemplatingLoader;

public class RelativisticShaderProvider extends DefaultShaderProvider {
    public final RelativisticShader.Config config;

    public RelativisticShaderProvider(final RelativisticShader.Config config) {
        this.config = (config == null) ? new RelativisticShader.Config() : config;
    }

    public RelativisticShaderProvider(final String vertexShader, final String fragmentShader) {
        this(new RelativisticShader.Config(vertexShader, fragmentShader));
    }

    public RelativisticShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    public RelativisticShaderProvider() {
        this(null);
    }

    @Override
    protected Shader createShader(final Renderable renderable) {
        return new RelativisticShader(renderable, config);
    }
}
