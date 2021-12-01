/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.assets.ShaderTemplatingLoader;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.provider.DefaultIntShaderProvider;

public class TessellationShaderProvider extends DefaultIntShaderProvider {
    public static class Config extends DefaultIntShader.Config {
        String controlShader = null;
        String evaluationShader = null;
        public Config () {
        }

        public Config (final String vertexShader, final String controlShader, final String evaluationShader, final String fragmentShader) {
            super(vertexShader, fragmentShader);
            this.controlShader = controlShader;
            this.evaluationShader = evaluationShader;
        }
    }


    public TessellationShaderProvider(final Config config) {
        super(config);
    }

    public TessellationShaderProvider(final String vertexShader, final String controlShader, final String evaluationShader, final String fragmentShader) {
        this(new Config(vertexShader, controlShader, evaluationShader, fragmentShader));
    }

    public TessellationShaderProvider(final FileHandle vertexShader, final FileHandle controlShader, final FileHandle evaluationShader, final FileHandle fragmentShader) {
        this(ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(controlShader), ShaderTemplatingLoader.load(evaluationShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    public TessellationShaderProvider() {
        this(null);
    }

    @Override
    protected IntShader createShader(final IntRenderable renderable) {
        return new TessellationShader(renderable, (Config) config);
    }
}
