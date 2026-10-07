/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.CameraMotionBlurFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.util.Constants;

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
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        // Viewport.
        if (dest != null) {
            cameraMotionBlurFilter.setViewport(dest.getWidth(), dest.getHeight());
        } else {
            cameraMotionBlurFilter.setViewport(width, height);
        }
        // Delta camera pos.
        var cam = GaiaSky.instance.getICamera();
        cam.getDPos().put(aux);
        // When the camera is position-locked to the focus, the camera delta
        // contains the focus's own motion (e.g. its orbital motion). Focus-
        // locked geometry moves with the camera, so it must not be attributed
        // the full camera delta. Subtract the focus delta, so that the blur
        // for focus-locked pixels only reflects the user's motion relative
        // to the focus. Static background objects will be under-blurred, but
        // that is preferable to the focus being over-blurred.
        // Sign: for a focus-locked point, W_prev = W_cur - dPos - dx, while
        // the shader computes W_cur - u_dCam. So u_dCam = dPos + dx.
        var focusDx = cam.getFocusDx();
        if (focusDx != null && cam.getFocus() != null && GaiaSky.settings().scene.camera.focusLock.position) {
            aux.x += focusDx.x.floatValue();
            aux.y += focusDx.y.floatValue();
            aux.z += focusDx.z.floatValue();
        }
        cameraMotionBlurFilter.setDCam(aux);
        // Z-far and K.
        cameraMotionBlurFilter.setZFarK((float) cam.getFar(), Constants.getCameraK());
        // Projection matrix coefficients for the clip-space Z: A = -(f+n)/(f-n), B = -2fn/(f-n).
        float near = (float) cam.getNear();
        float far = (float) cam.getFar();
        cameraMotionBlurFilter.setProjAB(-(far + near) / (far - near), -2f * far * near / (far - near));
        // Previous projectionView inverse matrix.
        cameraMotionBlurFilter.setProjView(cam.getProjView());
        cameraMotionBlurFilter.setPrevProjView(cam.getPreviousProjView());

        restoreViewport(dest);
        cameraMotionBlurFilter.setDepthTexture(full.getDepthBufferTexture());
        cameraMotionBlurFilter.setInput(src).setOutput(dest).render();
    }
}
