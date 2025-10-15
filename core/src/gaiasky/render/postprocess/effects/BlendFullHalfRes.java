/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.GaiaSky;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.BlendFullHalfResFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.util.Constants;

public class BlendFullHalfRes extends PostProcessorEffect {
    private final BlendFullHalfResFilter filter;

    public BlendFullHalfRes() {
        filter = new BlendFullHalfResFilter();
        disposables.add(filter);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        // Z-far and K.
        var cam = GaiaSky.instance.getICamera();
        filter.setZFarK((float) cam.getFar(), Constants.getCameraK());

        restoreViewport(dest);
        filter.setInput(full, half).setOutput(dest).render();
    }
}
