/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessor;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.BlurFilter;
import gaiasky.render.postprocess.filters.CombineFilter;
import gaiasky.render.postprocess.filters.ThresholdFilter;
import gaiasky.render.postprocess.util.PingPongBuffer;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class Bloom extends PostProcessorEffect {
    private final PingPongBuffer pingPongBuffer;
    private final BlurFilter blurFilter;
    private final ThresholdFilter thresholdFilter;
    private final CombineFilter combineFilter;
    private boolean blending = false;
    private int sFactor, dFactor;

    public Bloom(int fboWidth, int fboHeight) {
        // Use RGB888 to force internal format GL_RGB16F, omitting the alpha channel.
        pingPongBuffer = PostProcessor.newPingPongBuffer(fboWidth,
                                                         fboHeight,
                                                         Pixmap.Format.RGB888,
                                                         false,
                                                         false,
                                                         false,
                                                         false);

        blurFilter = new BlurFilter(fboWidth, fboHeight);
        thresholdFilter = new ThresholdFilter();
        combineFilter = new CombineFilter();
        disposables.addAll(blurFilter, thresholdFilter, combineFilter, pingPongBuffer);

        blurFilter.setAmount(0);
        blurFilter.setPasses(3);
        blurFilter.setType(BlurFilter.BlurType.Gaussian5x5b);
        setThreshold(0.3f);
        setBaseIntensity(1f);
        setBaseSaturation(0.85f);
        setBloomIntesnity(1.1f);
        setBloomSaturation(0.85f);

    }

    public void setBaseIntensity(float intensity) {
        combineFilter.setSource1Intensity(intensity);
    }

    public void setBaseSaturation(float saturation) {
        combineFilter.setSource1Saturation(saturation);
    }

    public void setBloomIntesnity(float intensity) {
        combineFilter.setSource2Intensity(intensity);
    }

    public void setBloomSaturation(float saturation) {
        combineFilter.setSource2Saturation(saturation);
    }

    public void enableBlending(int sfactor, int dfactor) {
        this.blending = true;
        this.sFactor = sfactor;
        this.dFactor = dfactor;
    }

    public void disableBlending() {
        this.blending = false;
    }

    public float getThreshold() {
        return thresholdFilter.getThreshold();
    }

    public void setThreshold(float threshold) {
        this.thresholdFilter.setThreshold(threshold);
    }

    public BlurFilter.BlurType getBlurType() {
        return blurFilter.getType();
    }

    public void setBlurType(BlurFilter.BlurType type) {
        blurFilter.setType(type);
    }


    public int getBlurPasses() {
        return blurFilter.getPasses();
    }

    public void setBlurPasses(int passes) {
        blurFilter.setPasses(passes);
    }

    public float getBlurAmount() {
        return blurFilter.getAmount();
    }

    public void setBlurAmount(float amount) {
        blurFilter.setAmount(amount);
    }

    @Override
    public void render(final FrameBuffer src, final FrameBuffer dest, GaiaSkyFrameBuffer main) {
        Texture texsrc = src.getColorBufferTexture();

        boolean blendingWasEnabled = PostProcessor.isStateEnabled(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        pingPongBuffer.begin();
        {
            // threshold / high-pass filterp
            // only areas with pixels >= threshold are blit to smaller fbo
            thresholdFilter.setInput(texsrc).setOutput(pingPongBuffer.getSourceBuffer()).render();

            // blur pass
            blurFilter.render(pingPongBuffer);
        }
        pingPongBuffer.end();

        if (blending || blendingWasEnabled) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
        }

        if (blending) {
            //Gdx.gl.glBlendFuncSeparate(sfactor, dfactor, GL30.GL_ONE, GL30.GL_ONE);
            Gdx.gl.glBlendFunc(sFactor, dFactor);
        }

        restoreViewport(dest);

        // mix original scene and blurred threshold, modulate via
        // set(Base|Bloom)(Saturation|Intensity)
        combineFilter.setOutput(dest).setInput(texsrc, pingPongBuffer.getResultTexture()).render();
        //copy.setInput(pingPongBuffer.getResultTexture()).setOutput(dest).render();

    }

    @Override
    public void rebind() {
        blurFilter.rebind();
        thresholdFilter.rebind();
        combineFilter.rebind();
        pingPongBuffer.rebind();
    }

    public static class Settings {
        public final String name;

        public final BlurFilter.BlurType blurType;
        public final int blurPasses; // simple blur
        public final float blurAmount; // normal blur (1 pass)
        public final float bloomThreshold;

        public final float bloomIntensity;
        public final float bloomSaturation;
        public final float baseIntensity;
        public final float baseSaturation;

        public Settings(String name, BlurFilter.BlurType blurType, int blurPasses, float blurAmount, float bloomThreshold, float baseIntensity, float baseSaturation, float bloomIntensity, float bloomSaturation) {
            this.name = name;
            this.blurType = blurType;
            this.blurPasses = blurPasses;
            this.blurAmount = blurAmount;

            this.bloomThreshold = bloomThreshold;
            this.baseIntensity = baseIntensity;
            this.baseSaturation = baseSaturation;
            this.bloomIntensity = bloomIntensity;
            this.bloomSaturation = bloomSaturation;
        }

        // simple blur
        public Settings(String name, int blurPasses, float bloomThreshold, float baseIntensity, float baseSaturation, float bloomIntensity, float bloomSaturation) {
            this(name, BlurFilter.BlurType.Gaussian5x5, blurPasses, 0, bloomThreshold, baseIntensity, baseSaturation, bloomIntensity, bloomSaturation);
        }

        public Settings(Settings other) {
            this.name = other.name;
            this.blurType = other.blurType;
            this.blurPasses = other.blurPasses;
            this.blurAmount = other.blurAmount;

            this.bloomThreshold = other.bloomThreshold;
            this.baseIntensity = other.baseIntensity;
            this.baseSaturation = other.baseSaturation;
            this.bloomIntensity = other.bloomIntensity;
            this.bloomSaturation = other.bloomSaturation;
        }
    }
}
