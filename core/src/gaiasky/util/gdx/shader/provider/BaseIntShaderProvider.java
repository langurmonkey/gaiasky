/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.provider;

import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.IObserver;
import gaiasky.util.gdx.IntRenderable;
import gaiasky.util.gdx.shader.IntShader;

public abstract class BaseIntShaderProvider implements IntShaderProvider, IObserver {
    protected Array<IntShader> shaders = new Array<>();

    @Override
    public IntShader getShader(IntRenderable renderable) {
        IntShader suggestedShader = renderable.shader;
        if (suggestedShader != null && suggestedShader.canRender(renderable))
            return suggestedShader;
        for (IntShader shader : shaders) {
            if (shader.canRender(renderable))
                return shader;
        }
        final IntShader shader = createShader(renderable);
        shader.init();
        shaders.add(shader);
        return shader;
    }

    protected abstract IntShader createShader(final IntRenderable renderable);

    @Override
    public void dispose() {
        for (IntShader shader : shaders) {
            shader.dispose();
        }
        shaders.clear();
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.CLEAR_SHADERS) {
            dispose();
        }
    }
}
