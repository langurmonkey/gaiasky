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

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FloatFrameBuffer;
import com.badlogic.gdx.graphics.glutils.FloatTextureData;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import gaia.cu9.ari.gaiaorbit.screenshot.ImageRenderer;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.Copy;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.LevelsFilter;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.Luma;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Implements brightness, contrast, hue and saturation levels, plus
 * auto-tone mapping HDR and gamma correction.
 *
 * @author tsagrista
 */
public final class Levels extends PostProcessorEffect {
    private static final int LUMA_SIZE = 300;
    private LevelsFilter levels;
    private Luma luma;
    private float lumaMax = 0.9f, lumaAvg = 0.5f;
    private float currLumaMax = -1f, currLumaAvg = -1f;
    private FrameBuffer lumaBuffer;

    /**
     * Creates the effect
     */
    public Levels() {
        levels = new LevelsFilter();
        luma = new Luma();

        GLFrameBuffer.FrameBufferBuilder fbb = new GLFrameBuffer.FrameBufferBuilder(LUMA_SIZE, LUMA_SIZE);
        fbb.addColorTextureAttachment(GL30.GL_RGBA32F, GL30.GL_RGBA, GL30.GL_FLOAT);
        lumaBuffer = new GaiaSkyFrameBuffer(fbb);
        lumaBuffer.getColorBufferTexture().setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.MipMapLinearLinear);

        luma.setImageSize(LUMA_SIZE, LUMA_SIZE);
        luma.setTexelSize(1f / LUMA_SIZE, 1f / LUMA_SIZE);
    }

    public Luma getLuma() {
        return luma;
    }

    public FrameBuffer getLumaBuffer() {
        return lumaBuffer;
    }

    /**
     * Set the brightness
     *
     * @param value The brightness value in [-1..1]
     */
    public void setBrightness(float value) {
        levels.setBrightness(value);
    }

    /**
     * Set the saturation
     *
     * @param value The saturation value in [0..2]
     */
    public void setSaturation(float value) {
        levels.setSaturation(value);
    }

    /**
     * Set the hue
     *
     * @param value The hue value in [0..2]
     */
    public void setHue(float value) {
        levels.setHue(value);
    }

    /**
     * Set the contrast
     *
     * @param value The contrast value in [0..2]
     */
    public void setContrast(float value) {
        levels.setContrast(value);
    }

    /**
     * Sets the gamma correction value
     *
     * @param value The gamma value in [0..3]
     */
    public void setGamma(float value) {
        levels.setGamma(value);
    }

    /**
     * Sets the exposure tone mapping value
     *
     * @param value The exposure value in [0..n]
     */
    public void setExposure(float value) {
        levels.setExposure(value);
    }

    @Override
    public void dispose() {
        if (levels != null) {
            levels.dispose();
            levels = null;
        }
    }

    @Override
    public void rebind() {
        levels.rebind();
    }

    FloatBuffer pixels = BufferUtils.createFloatBuffer(LUMA_SIZE * LUMA_SIZE * 4);

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        // Luminance
        luma.setInput(src).setOutput(lumaBuffer).render();

        // Actual levels
        levels.setInput(src).setOutput(dest).render();

        // Read out data
        Gdx.app.postRunnable(() -> {
            lumaBuffer.begin();
            GL30.glReadPixels(0, 0, LUMA_SIZE, LUMA_SIZE, GL30.GL_RGBA, GL30.GL_FLOAT, pixels);
            lumaBuffer.end();

            computeMaxAvg(pixels);

            // Slowly move towards target luma values
            if (currLumaAvg < 0) {
                currLumaAvg = lumaAvg;
                currLumaMax = lumaMax;
            } else {
                float dt = Gdx.graphics.getDeltaTime();
                // Low pass filter
                float smoothing = 0.5f;
                currLumaAvg += dt * (lumaAvg - currLumaAvg) / smoothing;
                currLumaMax += dt * (lumaMax - currLumaMax) / smoothing;
                levels.setAvgMaxLuma(currLumaAvg, currLumaMax);
            }
        });
    }

    private void computeMaxAvg(FloatBuffer buff) {
        buff.rewind();
        double avg = 0;
        double max = -Double.MIN_VALUE;
        int i = 1;
        while (buff.hasRemaining()) {
            double v = (double) buff.get();

            // Skip g, b, a
            buff.get();
            buff.get();
            buff.get();

            if (!Double.isNaN(v)) {
                avg = avg + (v - avg) / (i + 1);
                max = v > max ? v : max;
                i++;
            }
        }

        lumaMax = (float) max;
        lumaAvg = (float) avg;
        buff.clear();
    }
}
