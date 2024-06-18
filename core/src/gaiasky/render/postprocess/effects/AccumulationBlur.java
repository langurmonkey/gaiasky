/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.AccumulationBlurFilter;
import gaiasky.render.postprocess.filters.CopyFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public class AccumulationBlur extends PostProcessorEffect {
    private final CopyFilter copyFilter;
    private final AccumulationBlurFilter motionFilter;
    private final FrameBuffer fbo;

    public AccumulationBlur(int width, int height) {
        motionFilter = new AccumulationBlurFilter();
        motionFilter.setResolution(width, height);
        copyFilter = new CopyFilter();
        fbo = new FrameBuffer(Format.RGBA8888, width, height, false);

        disposables.addAll(copyFilter, motionFilter, fbo);
    }

    public void setBlurOpacity(float blurOpacity) {
        motionFilter.setBlurOpacity(blurOpacity);
    }

    public void setBlurRadius(float blurRadius) {
        motionFilter.setBlurRadius(blurRadius);
    }

    public void setResolution(int w, int h) {
        motionFilter.setResolution(w, h);
    }

    @Override
    public void rebind() {
        motionFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        if (dest != null) {
            motionFilter.setInput(src).setOutput(dest).render();
        } else {

            motionFilter.setInput(src).setOutput(fbo).render();

            // Copy fbo to screen
            copyFilter.setInput(fbo).setOutput(dest).render();
        }

        // Set last frame
        motionFilter.setLastFrameTexture(fbo.getColorBufferTexture());

    }

}
