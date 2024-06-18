/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.ChromaticAberrationFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public class ChromaticAberration extends PostProcessorEffect {

    private final ChromaticAberrationFilter filter;

    public ChromaticAberration(float amount) {
        filter = new ChromaticAberrationFilter(amount);
        disposables.add(filter);
    }

    public void setAberrationAmount(float amount) {
        filter.setAberrationAmount(amount);
    }

    public float getAberrationAmount() {
        return filter.getAberrationAmount();
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }
}
