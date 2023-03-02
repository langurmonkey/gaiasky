/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.shader.provider;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.DefaultIntShader;
import gaiasky.util.gdx.shader.IntShader;

public class DefaultIntShaderProvider extends BaseIntShaderProvider {
    public final DefaultIntShader.Config config;

    public DefaultIntShaderProvider(final DefaultIntShader.Config config) {
        this.config = (config == null) ? new DefaultIntShader.Config() : config;
        EventManager.instance.subscribe(this, Event.CLEAR_SHADERS);
    }

    public DefaultIntShaderProvider(final String vertexShaderCode, final String fragmentShaderCode) {
        this(new DefaultIntShader.Config(vertexShaderCode, fragmentShaderCode));
    }

    public DefaultIntShaderProvider(final FileHandle vertexShaderFile, final FileHandle fragmentShaderFile) {
        this(vertexShaderFile.readString(), fragmentShaderFile.readString());
    }

    public DefaultIntShaderProvider() {
        this(null);
    }

    @Override
    protected IntShader createShader(final IntRenderable renderable) {
        return new DefaultIntShader(renderable, config);
    }
}
