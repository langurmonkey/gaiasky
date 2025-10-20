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
import gaiasky.render.postprocess.filters.DepthBufferFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.util.Constants;

public final class DepthBuffer extends PostProcessorEffect {
    private final DepthBufferFilter filter;

    /** Creates the effect */
    public DepthBuffer() {
        filter = new DepthBufferFilter();
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
        // Get depth buffer texture from main frame buffer
        filter.setDepthTexture(full.getDepthBufferTexture());
        // Set input, output and render
        filter.setInput(src).setOutput(dest).render();
    }
}
