/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.shader.provider;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.gdx.IntRenderable;
import gaiasky.render.gdx.shader.DefaultIntShader;
import gaiasky.render.gdx.shader.IntShader;

public class DefaultIntShaderProvider extends BaseIntShaderProvider {
    public final DefaultIntShader.Config config;

    public DefaultIntShaderProvider(DefaultIntShader.Config config) {
        this.config = (config == null) ? new DefaultIntShader.Config() : config;
        EventManager.instance.subscribe(this, Event.CLEAR_SHADERS);
    }

    public DefaultIntShaderProvider(String vertexShaderCode, String fragmentShaderCode) {
        this(new DefaultIntShader.Config(vertexShaderCode, fragmentShaderCode));
    }

    public DefaultIntShaderProvider(FileHandle vertexShaderFile, FileHandle fragmentShaderFile) {
        this(vertexShaderFile.readString(), fragmentShaderFile.readString());
    }

    public DefaultIntShaderProvider() {
        this(null);
    }

    @Override
    protected IntShader createShader(IntRenderable renderable) {
        return new DefaultIntShader(renderable, config);
    }
}
