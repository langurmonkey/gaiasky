/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.SSRFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public class SSR extends PostProcessorEffect {
    private SSRFilter filter;

    public SSR() {
        super();
        filter = new SSRFilter();
    }

    public void setFrustumCorners(Matrix4 frustumCorners) {
        filter.setFrustumCorners(frustumCorners);
    }

    public void setCombined(Matrix4 combined) {
        filter.setCombined(combined);
    }

    public void setProjection(Matrix4 proj) {
        filter.setProjection(proj);
    }

    public void setView(Matrix4 view) {
        filter.setView(view);
    }

    public void setZfarK(float zfar, float k) {
        filter.setZfarK(zfar, k);
    }

    public void setTexture1(Texture tex) {
        filter.setTexture1(tex);
    }

    public void setTexture2(Texture tex) {
        filter.setTexture2(tex);
    }

    public void setTexture3(Texture tex) {
        filter.setTexture3(tex);
    }

    @Override
    public void dispose() {
        if (filter != null) {
            filter.dispose();
            filter = null;
        }
    }

    @Override
    public void rebind() {
        if (filter != null) {
            filter.rebind();
        }
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        // Get depth buffer texture from main frame buffer
        filter.setDepthTexture(main.getDepthBufferTexture());
        // Normal buffer
        filter.setNormalTexture(main.getNormalBufferTexture());
        // Reflection mask
        filter.setReflectionTexture(main.getReflectionMaskBufferTexture());
        // Set input, output and render
        filter.setInput(src).setOutput(dest).render();
    }
}
