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
import gaiasky.util.gdx.shader.DefaultIntShader.Config;
import gaiasky.util.gdx.shader.IntShader;
import gaiasky.util.gdx.shader.RelativisticShader;
import gaiasky.util.gdx.shader.loader.ShaderTemplatingLoader;

public class RelativisticShaderProvider extends DefaultIntShaderProvider {
    public final Config config;

    public RelativisticShaderProvider(final Config config) {
        this.config = (config == null) ? new Config() : config;
        EventManager.instance.subscribe(this, Event.CLEAR_SHADERS);
    }

    public RelativisticShaderProvider(final String vertexFile, final String fragmentFile, final String vertexShader, final String fragmentShader) {
        this(new Config(vertexFile, fragmentFile, vertexShader, fragmentShader));
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
