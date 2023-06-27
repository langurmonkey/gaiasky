/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.CameraBlur;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class CameraMotion extends PostProcessorEffect {
    private final CameraBlur cameraBlur;
    private final float width;
    private final float height;

    public CameraMotion(float width, float height) {
        this.width = width;
        this.height = height;
        cameraBlur = new CameraBlur();
        cameraBlur.setVelocityTexture(null);
        disposables.add(cameraBlur);
    }

    public CameraMotion(int width, int height) {
        this((float) width, (float) height);
    }

    public void setVelocityTexture(Texture velocityTexture) {
        cameraBlur.setVelocityTexture(velocityTexture);
    }

    public void setBlurMaxSamples(int samples) {
        cameraBlur.setBlurMaxSamples(samples);
    }

    public void setBlurScale(float scale) {
        cameraBlur.setBlurScale(scale);
    }

    public void setVelocityScale(float scale) {
        cameraBlur.setVelocityScale(scale);
    }

    @Override
    public void rebind() {
        cameraBlur.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        if (dest != null) {
            cameraBlur.setViewport(dest.getWidth(), dest.getHeight());
        } else {
            cameraBlur.setViewport(width, height);
        }

        restoreViewport(dest);
        cameraBlur.setVelocityTexture(main.getVelocityBufferTexture());
        cameraBlur.setInput(src).setOutput(dest).render();
    }
}
