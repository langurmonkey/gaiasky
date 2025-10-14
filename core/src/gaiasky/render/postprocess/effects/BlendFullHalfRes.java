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
import gaiasky.render.postprocess.filters.CopyFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public class BlendFullHalfRes extends PostProcessorEffect {
    private final CombineFilter combine;

    public BlendFullHalfRes() {
        combine = new CombineFilter();
        disposables.add(combine);
    }

    @Override
    public void rebind() {
        combine.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        combine.setInput(full, half).setOutput(dest).render();
    }
}
