/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2012 tsagrista
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.RaymarchingFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Implements a raymarching effect, usually for SDFs.
 *
 * @author Toni Sagrista
 */
public final class Raymarching extends PostProcessorEffect {
    private RaymarchingFilter filter = null;

    public Raymarching(float viewportWidth, float viewportHeight) {
        this((int) viewportWidth, (int) viewportHeight);
    }

    public Raymarching(int viewportWidth, int viewportHeight) {
        super();
        filter = new RaymarchingFilter(viewportWidth, viewportHeight);
    }

    public void setViewportSize(int width, int height) {
        filter.setViewportSize(width, height);
    }

    public void setFrustumCorners(Matrix4 frustumCorners) {
        filter.setFrustumCorners(frustumCorners);
    }

    public void setCamInvView(Matrix4 civ) {
        filter.setCaminvView(civ);
    }

    public void setCamPos(Vector3 camPos){
        filter.setCamPos(camPos);
    }

    public void setZfarK(float zfar, float k){
        filter.setZfarK(zfar, k);
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
