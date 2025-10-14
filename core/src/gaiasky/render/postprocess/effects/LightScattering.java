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
import gaiasky.render.postprocess.filters.BiasFilter;
import gaiasky.render.postprocess.filters.BlurFilter;
import gaiasky.render.postprocess.filters.CombineFilter;
import gaiasky.render.postprocess.filters.LightScatteringFilter;
import gaiasky.render.postprocess.util.PingPongBuffer;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class LightScattering extends PostProcessorEffect {
    private final PingPongBuffer pingPongBuffer;
    private final LightScatteringFilter scatteringFilter;
    private final BlurFilter blurFilter;
    private final BiasFilter biasFilter;
    private final CombineFilter combineFilter;
    private Settings settings;
    private boolean blending = false;
    private int sFactor, dFactor;

    public LightScattering(int fboWidth, int fboHeight) {
        // Use RGB888 to force internal format GL_RGB16F, omitting the alpha channel.
        this.pingPongBuffer = PostProcessor.newPingPongBuffer(fboWidth,
                                                              fboHeight,
                                                              Pixmap.Format.RGB888,
                                                              false,
                                                              false,
                                                              false,
                                                              false);

        this.scatteringFilter = new LightScatteringFilter(fboWidth, fboHeight);
        this.blurFilter = new BlurFilter(fboWidth, fboHeight);
        this.biasFilter = new BiasFilter();
        this.combineFilter = new CombineFilter();

        disposables.addAll(pingPongBuffer, scatteringFilter, blurFilter, biasFilter, combineFilter);

        setSettings(new Settings("default", 2, -0.9f, 1f, 1f, 0.7f, 1f));
    }

    /**
     * Sets the positions of the 10 lights in [0..1] in both coordinates
     **/
    public void setLightPositions(int nLights, float[] vec) {
        scatteringFilter.setLightPositions(nLights, vec);
    }

    public void setLightViewAngles(float[] vec) {
        scatteringFilter.setLightViewAngles(vec);
    }

    public void setBaseIntesity(float intensity) {
        combineFilter.setSource1Intensity(intensity);
    }

    public void setScatteringIntesity(float intensity) {
        combineFilter.setSource2Intensity(intensity);
    }

    public void enableBlending(int sfactor, int dfactor) {
        this.blending = true;
        this.sFactor = sfactor;
        this.dFactor = dfactor;
    }

    public void disableBlending() {
        this.blending = false;
    }

    public void setDecay(float decay) {
        scatteringFilter.setDecay(decay);
    }

    public void setDensity(float density) {
        scatteringFilter.setDensity(density);
    }

    public void setWeight(float weight) {
        scatteringFilter.setWeight(weight);
    }

    public void setNumSamples(int numSamples) {
        scatteringFilter.setNumSamples(numSamples);
    }

    public float getBias() {
        return biasFilter.getBias();
    }

    public void setBias(float b) {
        biasFilter.setBias(b);
    }

    public float getBaseIntensity() {
        return combineFilter.getSource1Intensity();
    }

    public float getBaseSaturation() {
        return combineFilter.getSource1Saturation();
    }

    public void setBaseSaturation(float saturation) {
        combineFilter.setSource1Saturation(saturation);
    }

    public float getScatteringIntensity() {
        return combineFilter.getSource2Intensity();
    }

    public float getScatteringSaturation() {
        return combineFilter.getSource2Saturation();
    }

    public void setScatteringSaturation(float saturation) {
        combineFilter.setSource2Saturation(saturation);
    }

    public boolean isBlendingEnabled() {
        return blending;
    }

    public int getBlendingSourceFactor() {
        return sFactor;
    }

    public int getBlendingDestFactor() {
        return dFactor;
    }

    public BlurFilter.BlurType getBlurType() {
        return blurFilter.getType();
    }

    public void setBlurType(BlurFilter.BlurType type) {
        blurFilter.setType(type);
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;

        // setup threshold filter
        setBias(settings.bias);

        // setup combine filter
        setBaseIntesity(settings.baseIntensity);
        setBaseSaturation(settings.baseSaturation);
        setScatteringIntesity(settings.scatteringIntensity);
        setScatteringSaturation(settings.scatteringSaturation);

        // setup blur filter
        setBlurPasses(settings.blurPasses);
        setBlurAmount(settings.blurAmount);
        setBlurType(settings.blurType);

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
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        Texture texsrc = src.getColorBufferTexture();

        boolean blendingWasEnabled = PostProcessor.isStateEnabled(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        pingPongBuffer.begin();
        {
            // apply bias
            biasFilter.setInput(texsrc).setOutput(pingPongBuffer.getSourceBuffer()).render();

            scatteringFilter.setInput(pingPongBuffer.getSourceBuffer()).setOutput(pingPongBuffer.getResultBuffer()).render();

            pingPongBuffer.set(pingPongBuffer.getResultBuffer(), pingPongBuffer.getSourceBuffer());

            // blur pass
            blurFilter.render(pingPongBuffer);
        }
        pingPongBuffer.end();

        if (blending || blendingWasEnabled) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
        }

        if (blending) {
            Gdx.gl.glBlendFunc(sFactor, dFactor);
        }

        restoreViewport(dest);

        // mix original scene and blurred threshold, modulate via
        combineFilter.setOutput(dest).setInput(texsrc, pingPongBuffer.getResultTexture()).render();
    }

    @Override
    public void rebind() {
        blurFilter.rebind();
        biasFilter.rebind();
        combineFilter.rebind();
        pingPongBuffer.rebind();
    }

    public static class Settings {
        public final String name;

        public final BlurFilter.BlurType blurType;
        public final int blurPasses; // simple blur
        public final float blurAmount; // normal blur (1 pass)
        public final float bias;

        public final float scatteringIntensity;
        public final float scatteringSaturation;
        public final float baseIntensity;
        public final float baseSaturation;

        public Settings(String name, BlurFilter.BlurType blurType, int blurPasses, float blurAmount, float bias, float baseIntensity, float baseSaturation, float scatteringIntensity, float scatteringSaturation) {
            this.name = name;
            this.blurType = blurType;
            this.blurPasses = blurPasses;
            this.blurAmount = blurAmount;

            this.bias = bias;
            this.baseIntensity = baseIntensity;
            this.baseSaturation = baseSaturation;
            this.scatteringIntensity = scatteringIntensity;
            this.scatteringSaturation = scatteringSaturation;

        }

        // simple blur
        public Settings(String name, int blurPasses, float bias, float baseIntensity, float baseSaturation, float scatteringIntensity, float scatteringSaturation) {
            this(name, BlurFilter.BlurType.Gaussian5x5b, blurPasses, 0, bias, baseIntensity, baseSaturation, scatteringIntensity, scatteringSaturation);
        }

        public Settings(Settings other) {
            this.name = other.name;
            this.blurType = other.blurType;
            this.blurPasses = other.blurPasses;
            this.blurAmount = other.blurAmount;

            this.bias = other.bias;
            this.baseIntensity = other.baseIntensity;
            this.baseSaturation = other.baseSaturation;
            this.scatteringIntensity = other.scatteringIntensity;
            this.scatteringSaturation = other.scatteringSaturation;

        }
    }
}
