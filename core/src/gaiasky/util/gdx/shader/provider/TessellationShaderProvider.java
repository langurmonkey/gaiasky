/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.provider;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.DefaultIntShader;
import gaiasky.util.gdx.shader.IntShader;
import gaiasky.util.gdx.shader.TessellationShader;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;

public class TessellationShaderProvider extends DefaultIntShaderProvider {
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

    public static class Config extends DefaultIntShader.Config {
        public String controlShader = null;
        public String evaluationShader = null;

        public Config() {
        }

        public Config(final String vertexShader, final String controlShader, final String evaluationShader, final String fragmentShader) {
            super(vertexShader, fragmentShader);
            this.controlShader = controlShader;
            this.evaluationShader = evaluationShader;
        }
    }
}
