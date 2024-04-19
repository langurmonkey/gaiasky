/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.util.Constants;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.CameraMotionBlurFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class CameraMotionBlur extends PostProcessorEffect {
    private final CameraMotionBlurFilter cameraMotionBlurFilter;
    private final float width;
    private final float height;

    public CameraMotionBlur(float width,
                            float height) {
        this.width = width;
        this.height = height;
        cameraMotionBlurFilter = new CameraMotionBlurFilter();
        disposables.add(cameraMotionBlurFilter);
    }

    public void setBlurMaxSamples(int samples) {
        cameraMotionBlurFilter.setBlurMaxSamples(samples);
    }

    public void setBlurScale(float scale) {
        cameraMotionBlurFilter.setBlurScale(scale);
    }

    @Override
    public void rebind() {
        cameraMotionBlurFilter.rebind();
    }

    private final Vector3 aux = new Vector3();

    @Override
    public void render(FrameBuffer src,
                       FrameBuffer dest,
                       GaiaSkyFrameBuffer main) {
        // Viewport.
        if (dest != null) {
            cameraMotionBlurFilter.setViewport(dest.getWidth(), dest.getHeight());
        } else {
            cameraMotionBlurFilter.setViewport(width, height);
        }
        // Delta camera pos.
        var cam = GaiaSky.instance.getICamera();
        cam.getDPos().put(aux);
        cameraMotionBlurFilter.setDCam(aux.scl(1f / (float) cam.getSpeedScalingCapped()));
        // Z-far and K.
        cameraMotionBlurFilter.setZFarK((float) cam.getFar(), Constants.getCameraK());
        // Previous projectionView inverse matrix.
        cameraMotionBlurFilter.setProjView(cam.getProjView());
        cameraMotionBlurFilter.setPrevProjView(cam.getPreviousProjView());

        restoreViewport(dest);
        cameraMotionBlurFilter.setDepthTexture(main.getDepthBufferTexture());
        cameraMotionBlurFilter.setInput(src).setOutput(dest).render();
    }
}
