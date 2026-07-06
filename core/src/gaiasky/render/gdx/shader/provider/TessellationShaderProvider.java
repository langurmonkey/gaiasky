/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.shader.provider;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.render.gdx.IntRenderable;
import gaiasky.render.gdx.shader.DefaultIntShader;
import gaiasky.render.gdx.shader.IntShader;
import gaiasky.render.gdx.shader.TessellationShader;
import gaiasky.render.gdx.shader.loader.ShaderTemplatingLoader;

public class TessellationShaderProvider extends DefaultIntShaderProvider {
    public TessellationShaderProvider(Config config) {
        super(config);
    }

    public TessellationShaderProvider(String vertexShader, String controlShader, String evaluationShader, String fragmentShader) {
        this(new Config(vertexShader, controlShader, evaluationShader, fragmentShader));
    }

    public TessellationShaderProvider(FileHandle vertexShader, FileHandle controlShader, FileHandle evaluationShader, FileHandle fragmentShader) {
        this(ShaderTemplatingLoader.load(vertexShader), ShaderTemplatingLoader.load(controlShader), ShaderTemplatingLoader.load(evaluationShader), ShaderTemplatingLoader.load(fragmentShader));
    }

    public TessellationShaderProvider() {
        this(null);
    }

    @Override
    protected IntShader createShader(IntRenderable renderable) {
        return new TessellationShader(renderable, (Config) config);
    }

    public static class Config extends DefaultIntShader.Config {
        public String controlShader;
        public String evaluationShader;

        public Config() {
        }

        public Config(String vertexShader, String controlShader, String evaluationShader, String fragmentShader) {
            super(vertexShader, fragmentShader);
            this.controlShader = controlShader;
            this.evaluationShader = evaluationShader;
        }
    }
}
