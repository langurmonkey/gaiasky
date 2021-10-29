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
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.CameraBlur;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Camera blur effect.
 * @author bmanuel
 */
public final class CameraMotion extends PostProcessorEffect {
    private final CameraBlur camblur;
    private final float width;
    private final float height;

    public CameraMotion(float width, float height) {
        this.width = width;
        this.height = height;
        camblur = new CameraBlur();
        camblur.setVelocityTexture(null);
    }

    public CameraMotion(int width, int height) {
        this((float) width, (float) height);
    }

    @Override
    public void dispose() {
        camblur.dispose();
    }

    public void setVelocityTexture(Texture velocityTexture) {
        camblur.setVelocityTexture(velocityTexture);
    }

    public void setBlurMaxSamples(int samples) {
        camblur.setBlurMaxSamples(samples);
    }

    public void setBlurScale(float scale) {
        camblur.setBlurScale(scale);
    }

    public void setVelocityScale(float scale) {
        camblur.setVelocityScale(scale);
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
        camblur.setVelocityTexture(main.getVelocityBufferTexture());
        camblur.setInput(src).setOutput(dest).render();
    }
}
