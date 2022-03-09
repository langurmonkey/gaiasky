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

public class GroundShaderProvider extends DefaultIntShaderProvider {
    public final GroundShader.Config config;

    public GroundShaderProvider(final GroundShader.Config config) {
        this.config = (config == null) ? new GroundShader.Config() : config;
        EventManager.instance.subscribe(this, Event.CLEAR_SHADERS);
    }

    public GroundShaderProvider(final String vertexFile, final String fragmentFile, final String vertexShader, final String fragmentShader) {
        this(new GroundShader.Config(vertexFile, fragmentFile, vertexShader, fragmentShader));
    }

    public GroundShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(vertexShader.name(), fragmentShader.name(), ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    public GroundShaderProvider() {
        this(null);
    }

    @Override
    protected IntShader createShader(final IntRenderable renderable) {
        return new GroundShader(renderable, config);
    }
}
