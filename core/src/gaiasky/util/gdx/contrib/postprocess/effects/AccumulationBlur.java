/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.AccumulationBlurFilter;
import gaiasky.util.gdx.contrib.postprocess.filters.Copy;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public class AccumulationBlur extends PostProcessorEffect {
    private final Copy copyFilter;
    private AccumulationBlurFilter motionFilter;
    private FrameBuffer fbo;

    public AccumulationBlur(int width, int height) {
        motionFilter = new AccumulationBlurFilter();
        motionFilter.setResolution(width, height);
        copyFilter = new Copy();
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
    public void dispose() {
        if (motionFilter != null) {
            motionFilter.dispose();
            motionFilter = null;
        }
    }

    @Override
    public void rebind() {
        motionFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        if (fbo == null) {
            // Init frame buffer
            fbo = new FrameBuffer(Format.RGBA8888, src.getWidth(), src.getHeight(), false);
        }

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

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        // Dispose fbo
        if (!enabled && fbo != null) {
            fbo.dispose();
            fbo = null;
        }
    }

}
