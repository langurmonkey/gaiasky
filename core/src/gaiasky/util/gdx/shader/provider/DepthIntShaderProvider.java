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
import gaiasky.util.gdx.shader.DepthIntShader;
import gaiasky.util.gdx.shader.IntShader;

public class DepthIntShaderProvider extends BaseIntShaderProvider {
    public final DepthIntShader.Config config;

    public DepthIntShaderProvider(final DepthIntShader.Config config) {
        this.config = (config == null) ? new DepthIntShader.Config() : config;
        EventManager.instance.subscribe(this, Event.CLEAR_SHADERS);
    }

    public DepthIntShaderProvider(final String vertexShader, final String fragmentShader) {
        this(new DepthIntShader.Config(vertexShader, fragmentShader));
    }

    public DepthIntShaderProvider(final FileHandle vertexShader, final FileHandle fragmentShader) {
        this(vertexShader.readString(), fragmentShader.readString());
    }

    public DepthIntShaderProvider() {
        this(null);
    }

    @Override
    protected IntShader createShader(final IntRenderable renderable) {
        return new DepthIntShader(renderable, config);
    }
}
