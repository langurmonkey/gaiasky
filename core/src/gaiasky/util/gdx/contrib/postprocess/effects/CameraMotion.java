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
import gaiasky.util.gdx.contrib.postprocess.filters.CameraBlur;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class CameraMotion extends PostProcessorEffect {
    private final CameraBlur cameraBlur;
    private final float width;
    private final float height;

    public CameraMotion(float width,
                        float height) {
        this.width = width;
        this.height = height;
        cameraBlur = new CameraBlur();
        disposables.add(cameraBlur);
    }

    public void setBlurMaxSamples(int samples) {
        cameraBlur.setBlurMaxSamples(samples);
    }

    public void setBlurScale(float scale) {
        cameraBlur.setBlurScale(scale);
    }

    @Override
    public void rebind() {
        cameraBlur.rebind();
    }

    private final Vector3 aux = new Vector3();

    @Override
    public void render(FrameBuffer src,
                       FrameBuffer dest,
                       GaiaSkyFrameBuffer main) {
        // Viewport.
        if (dest != null) {
            cameraBlur.setViewport(dest.getWidth(), dest.getHeight());
        } else {
            cameraBlur.setViewport(width, height);
        }
        // Delta camera pos.
        var cam = GaiaSky.instance.getICamera();
        cam.getDPos().put(aux);
        cameraBlur.setDCam(aux.scl(1f / (float) cam.getSpeedScalingCapped()));
        // Zfar and K
        cameraBlur.setZfarK((float) cam.getFar(), Constants.getCameraK());
        // Previous projectionView inverse matrix.
        cameraBlur.setProjView(cam.getProjView());
        cameraBlur.setPrevProjView(cam.getPreviousProjView());

        restoreViewport(dest);
        cameraBlur.setDepthTexture(main.getDepthBufferTexture());
        cameraBlur.setInput(src).setOutput(dest).render();
    }
}
