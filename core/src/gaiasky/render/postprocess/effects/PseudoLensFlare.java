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
import gaiasky.render.postprocess.filters.*;
import gaiasky.render.postprocess.util.PingPongBuffer;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class PseudoLensFlare extends PostProcessorEffect {
    private final PingPongBuffer pingPongBuffer;
    private final PseudoLensFlareFilter flare;
    private final LensDirtFilter dirt;
    private final BlurFilter blurFilter;
    private final BiasFilter biasFilter;
    private final CombineFilter combineFilter;
    private Settings settings;
    private boolean blending = false;
    private int sFactor, dFactor;

    public PseudoLensFlare(int fboWidth, int fboHeight) {
        // Use RGB888 to force internal format GL_RGB16F, omitting the alpha channel.
        this.pingPongBuffer = PostProcessor.newPingPongBuffer(fboWidth,
                                                              fboHeight,
                                                              Pixmap.Format.RGB888,
                                                              false,
                                                              false,
                                                              false,
                                                              false);

        this.flare = new PseudoLensFlareFilter(fboWidth, fboHeight);
        this.dirt = new LensDirtFilter();
        this.blurFilter = new BlurFilter(fboWidth, fboHeight);
        this.biasFilter = new BiasFilter();
        this.combineFilter = new CombineFilter();

        disposables.addAll(pingPongBuffer, flare, dirt, blurFilter, biasFilter, combineFilter);

        setSettings(new Settings("default", 2, -0.9f, 1f, 1f, 0.7f, 1f, 8, 0.5f));
    }

    public void setBaseIntensity(float intensity) {
        combineFilter.setSource1Intensity(intensity);
    }

    public void setFlareIntensity(float intensity) {
        combineFilter.setSource2Intensity(intensity);
    }

    public void setHaloWidth(float haloWidth) {
        flare.setHaloWidth(haloWidth);
    }

    public void setLensColorTexture(Texture tex) {
        flare.setLensColorTexture(tex);
    }

    public void setLensDirtTexture(Texture tex) {
        dirt.setLensDirtTexture(tex);
    }

    public void setLensStarburstTexture(Texture tex) {
        dirt.setLensStarburstTexture(tex);
    }

    public void setStarburstOffset(float offset) {
        dirt.setStarburstOffset(offset);
    }

    public void enableBlending(int sfactor, int dfactor) {
        this.blending = true;
        this.sFactor = sfactor;
        this.dFactor = dfactor;
    }

    public void disableBlending() {
        this.blending = false;
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

    public float getFlareIntensity() {
        return combineFilter.getSource2Intensity();
    }

    public float getFlareSaturation() {
        return combineFilter.getSource2Saturation();
    }

    public void setFlareSaturation(float saturation) {
        combineFilter.setSource2Saturation(saturation);
    }

    public int getGhosts() {
        return flare.getGhosts();
    }

    public void setGhosts(int ghosts) {
        flare.setGhosts(ghosts);
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
        setBias(settings.flareBias);

        // setup combine filter
        setBaseIntensity(settings.baseIntensity);
        setBaseSaturation(settings.baseSaturation);
        setFlareIntensity(settings.flareIntensity);
        setFlareSaturation(settings.flareSaturation);

        // setup blur filter
        setBlurPasses(settings.blurPasses);
        setBlurAmount(settings.blurAmount);
        setBlurType(settings.blurType);

        setGhosts(settings.ghosts);
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
        boolean blendingWasEnabled = PostProcessor.isStateEnabled(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        pingPongBuffer.begin();
        {
            // Apply bias.
            biasFilter.setInput(src.getColorBufferTexture()).setOutput(pingPongBuffer.getSourceBuffer()).render();

            // Apply flare.
            flare.setInput(pingPongBuffer.getSourceBuffer()).setOutput(pingPongBuffer.getResultBuffer()).render();

            pingPongBuffer.set(pingPongBuffer.getResultBuffer(), pingPongBuffer.getSourceBuffer());

            // Blur pass.
            blurFilter.render(pingPongBuffer);

            // Apply lens dirt.
            dirt.setInput(pingPongBuffer.getSourceBuffer()).setOutput(pingPongBuffer.getResultBuffer()).render();
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
        combineFilter.setOutput(dest).setInput(src.getColorBufferTexture(), pingPongBuffer.getResultTexture()).render();
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
        public final float flareBias;

        public final float flareIntensity;
        public final float flareSaturation;
        public final float baseIntensity;
        public final float baseSaturation;

        public final int ghosts;
        public final float haloWidth;

        public Settings(String name, BlurFilter.BlurType blurType, int blurPasses, float blurAmount, float flareBias, float baseIntensity, float baseSaturation, float flareIntensity, float flareSaturation, int ghosts, float haloWidth) {
            this.name = name;
            this.blurType = blurType;
            this.blurPasses = blurPasses;
            this.blurAmount = blurAmount;

            this.flareBias = flareBias;
            this.baseIntensity = baseIntensity;
            this.baseSaturation = baseSaturation;
            this.flareIntensity = flareIntensity;
            this.flareSaturation = flareSaturation;

            this.ghosts = ghosts;
            this.haloWidth = haloWidth;
        }

        // simple blur
        public Settings(String name, int blurPasses, float flareBias, float baseIntensity, float baseSaturation, float flareIntensity, float flareSaturation, int ghosts, float haloWidth) {
            this(name, BlurFilter.BlurType.Gaussian5x5b, blurPasses, 0, flareBias, baseIntensity, baseSaturation, flareIntensity, flareSaturation, ghosts, haloWidth);
        }

        public Settings(Settings other) {
            this.name = other.name;
            this.blurType = other.blurType;
            this.blurPasses = other.blurPasses;
            this.blurAmount = other.blurAmount;

            this.flareBias = other.flareBias;
            this.baseIntensity = other.baseIntensity;
            this.baseSaturation = other.baseSaturation;
            this.flareIntensity = other.flareIntensity;
            this.flareSaturation = other.flareSaturation;

            this.ghosts = other.ghosts;
            this.haloWidth = other.haloWidth;

        }
    }
}
