/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2012 bmanuel
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

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.CameraBlur;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 *
 * @author bmanuel
 */
public final class CameraMotion extends PostProcessorEffect {
    private CameraBlur camblur;
    private float width, height;
    private Matrix4 ctp;

    public CameraMotion(int width, int height) {
        this.width = width;
        this.height = height;
        camblur = new CameraBlur();
        camblur.setDepthTexture(null);
        ctp = new Matrix4();
    }

    @Override
    public void dispose() {
        camblur.dispose();
    }

    public void setDepthTexture(Texture normalDepthMap) {
        camblur.setDepthTexture(normalDepthMap);
    }

    public void setMatrices(Matrix4 viewProjInv, Matrix4 prevViewProj) {
        camblur.setViewProjectionInverse(viewProjInv);
        camblur.setPreviousViewProjection(prevViewProj);
    }
    public void setMatrices(Matrix4 inv_view, Matrix4 prevViewProj, Matrix4 inv_proj) {
        ctp.set(prevViewProj).mul(inv_view);
        camblur.setCurrentToPrevious(ctp);
        camblur.setInverseProj(inv_proj);
    }

    public void setBlurPasses(int passes) {
        camblur.setBlurPasses(passes);
    }

    public void setBlurScale(float scale) {
        camblur.setBlurScale(scale);
    }

    public void setNearFarK(float near, float far, float k) {
        camblur.setNearFarK(near, far, k);
    }

    @Override
    public void rebind() {
        camblur.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        if (dest != null) {
            camblur.setViewport(dest.getWidth(), dest.getHeight());
        } else {
            camblur.setViewport(width, height);
        }

        restoreViewport(dest);
        camblur.setDepthTexture(main.getOwnDepthBufferTexture());
        camblur.setInput(src).setOutput(dest).render();
    }
}
