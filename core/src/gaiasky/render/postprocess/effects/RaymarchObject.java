/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.RaymarchingFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

/**
 * A ray-marching shader to render an object or effect using ray-marching.
 */
public final class RaymarchObject extends PostProcessorEffect {
    private final RaymarchingFilter filter;

    public RaymarchObject(String fragmentShader, float viewportWidth, float viewportHeight) {
        this(fragmentShader, (int) viewportWidth, (int) viewportHeight);
    }

    public RaymarchObject(String fragmentShader, int viewportWidth, int viewportHeight) {
        super();
        filter = new RaymarchingFilter(fragmentShader, viewportWidth, viewportHeight);
        disposables.add(filter);
    }

    public void setViewportSize(int width, int height) {
        filter.setViewportSize(width, height);
    }

    public void setFrustumCorners(Matrix4 frustumCorners) {
        filter.setFrustumCorners(frustumCorners);
    }

    public void setView(Matrix4 view) {
        filter.setView(view);
    }

    public void setCombined(Matrix4 viewProjection) {
        filter.setCombined(viewProjection);
    }

    public void setPos(Vector3 pos) {
        filter.setPos(pos);
    }

    public void setTime(float seconds) {
        filter.setTime(seconds);
    }

    public void setSize(float size) {
        filter.setSize(size);
    }

    public void setZfarK(float zfar, float k) {
        filter.setZfarK(zfar, k);
    }

    public void setAdditionalTexture(Texture tex) {
        filter.setAdditionalTexture(tex);
    }

    public void setAdditional(float[] additional) {
        filter.setAdditional(additional);
    }

    public void setAdditional(float a, float b, float c, float d) {
        filter.setAdditional(a, b, c, d);
    }

    public void setAdditional(int index, float value) {
        filter.setAdditional(index, value);
    }

    @Override
    public void updateShaders() {
        super.updateShaders();
        filter.updateProgram();
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        // Get depth buffer texture from main frame buffer
        filter.setDepthTexture(main.getDepthBufferTexture());
        // Set input, output and render
        filter.setInput(src).setOutput(dest).render();
    }
}
