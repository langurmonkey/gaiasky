/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.provider;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.AtmosphereShader;
import gaiasky.util.gdx.shader.AtmosphereShader.Config;
import gaiasky.util.gdx.shader.IntShader;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;

public class AtmosphereShaderProvider extends DefaultIntShaderProvider {
    public final Config config;

    public AtmosphereShaderProvider(final Config config) {
        this.config = (config == null) ? new Config() : config;
        EventManager.instance.subscribe(this, Event.CLEAR_SHADERS);
    }

    public AtmosphereShaderProvider(final String vertexShader, final String fragmentShader) {
        this(new Config(vertexShader, fragmentShader));
    }

    public AtmosphereShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    @Override
    protected IntShader createShader(final IntRenderable renderable) {
        return new AtmosphereShader(renderable, config);
    }

}
