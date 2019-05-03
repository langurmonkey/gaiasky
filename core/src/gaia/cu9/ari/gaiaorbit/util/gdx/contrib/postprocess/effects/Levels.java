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
import com.badlogic.gdx.graphics.GL20;
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
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Implements brightness, contrast, hue and saturation levels, plus
 * auto-tone mapping HDR and gamma correction.
 *
 * @author tsagrista
 */
public final class Levels extends PostProcessorEffect {
    private static final int LUMA_SIZE = 400;
    private LevelsFilter levels;
    private Luma luma;
    Copy copy;
    private FrameBuffer lumaBuffer;

    /**
     * Creates the effect
     */
    public Levels() {
        levels = new LevelsFilter();
        luma = new Luma();
        copy = new Copy();

        GLFrameBuffer.FrameBufferBuilder fbb = new GLFrameBuffer.FrameBufferBuilder(LUMA_SIZE, LUMA_SIZE);
        fbb.addColorTextureAttachment(GL30.GL_RGBA16F, GL30.GL_RGBA, GL30.GL_FLOAT);
        lumaBuffer = new GaiaSkyFrameBuffer(fbb);
        lumaBuffer.getColorBufferTexture().setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.MipMapLinearLinear);
        //lumaBuffer = new FloatFrameBuffer(LUMA_SIZE, LUMA_SIZE, false);

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

    //float[] pxls = new float[LUMA_SIZE * LUMA_SIZE * 4];
    //ByteBuffer pixels = ByteBuffer.allocateDirect(LUMA_SIZE * LUMA_SIZE * 4 * 4);
    //FloatBuffer pixelsf = FloatBuffer.allocate(LUMA_SIZE * LUMA_SIZE * 4);

    boolean out = true;
    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        // Luminance
        luma.setInput(src).setOutput(lumaBuffer).render();

        // Actual levels
        levels.setInput(src).setOutput(dest).render();
        //copy.setInput(lumaBuffer).setOutput(dest).render();

        // Read out data
       // if(true) {
       //     Gdx.app.postRunnable(() -> {
       //         if(out) {
       //             lumaBuffer.begin();
       //             //ImageRenderer.renderToImageGl20("/tmp/", "img.jpg", 400, 400);
       //             Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
       //             Gdx.gl.glReadPixels(1, 1, 400, 400, GL30.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
       //             lumaBuffer.end();

       //             System.out.println(pixels.get(100));
       //         }
       //     });
       // }

    }
}
