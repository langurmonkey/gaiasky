/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.assets.ShaderTemplatingLoader;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.provider.DefaultIntShaderProvider;

public class RelativisticShaderProvider extends DefaultIntShaderProvider {
    public final RelativisticShader.Config config;

    public RelativisticShaderProvider(final RelativisticShader.Config config) {
        this.config = (config == null) ? new RelativisticShader.Config() : config;
        EventManager.instance.subscribe(this, Event.CLEAR_SHADERS);
    }

    public RelativisticShaderProvider(final String vertexFile, final String fragmentFile, final String vertexShader, final String fragmentShader) {
        this(new RelativisticShader.Config(vertexFile, fragmentFile, vertexShader, fragmentShader));
    }

    public RelativisticShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(vertexShader.name(), fragmentShader.name(), ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    public RelativisticShaderProvider() {
        this(null);
    }

    @Override
    protected IntShader createShader(final IntRenderable renderable) {
        return new RelativisticShader(renderable, config);
    }
}
