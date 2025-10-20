/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.UnsharpMaskFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class UnsharpMask extends PostProcessorEffect {
    private final UnsharpMaskFilter filter;

    public UnsharpMask() {
        filter = new UnsharpMaskFilter();
        disposables.add(filter);
    }

    /**
     * The sharpen factor. 0 to disable, 1 is default.
     *
     * @param sf The sharpen factor
     */
    public void setSharpenFactor(float sf) {
        filter.setSharpenFactor(sf);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }
}
