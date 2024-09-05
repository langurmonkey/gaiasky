/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.CombineFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

/**
 * Blends the scene render target with the layer render target, which contains lines, labels, grids and
 * other non-scene elements.
 */
public class Blend extends PostProcessorEffect {
    private final CombineFilter combineFilter;

    public Blend() {
        combineFilter = new CombineFilter();
    }


    @Override
    public void rebind() {
        combineFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        combineFilter.setInput(main.getColorBufferTexture(), main.getLayerBufferTexture())
                .setOutput(dest)
                .render();
    }
}
