/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.Glow;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class LightGlow extends PostProcessorEffect {
    private final Glow glow;
    private Settings settings;
    private boolean blending = false;
    private int sfactor, dfactor;
    public LightGlow(int width, int height) {
        glow = new Glow(width, height);
    }

    @Override
    public void dispose() {
        glow.dispose();
    }

    public void setBackbufferScale(float bbs) {
        glow.setBackbufferScale(bbs);
    }

    public void setLightPositions(int nLights, float[] vec) {
        glow.setLightPositions(nLights, vec);
    }

    public void setLightSolidAngles(float[] vec) {
        glow.setLightViewAngles(vec);
    }

    public void setLightColors(float[] vec) {
        glow.setLightColors(vec);
    }

    public void setNSamples(int nSamples) {
        glow.setNSamples(nSamples);
    }

    public void setTextureScale(float scl) {
        glow.setTextureScale(scl);
    }

    public void setSpiralScale(float scl) {
        glow.setSpiralScale(scl);
    }

    public void setOrientation(float o) {
        glow.setOrientation(o);
    }

    public void enableBlending(int sfactor, int dfactor) {
        this.blending = true;
        this.sfactor = sfactor;
        this.dfactor = dfactor;
    }

    public void disableBlending() {
        this.blending = false;
    }

    public Texture getLightGlowTexture() {
        return glow.getLightGlowTexture();
    }

    public void setLightGlowTexture(Texture tex) {
        glow.setLightGlowTexture(tex);
    }

    public Texture getPrePassTexture() {
        return glow.getPrePassTexture();
    }

    public void setPrePassTexture(Texture tex) {
        glow.setPrePassTexture(tex);
    }

    public boolean isBlendingEnabled() {
        return blending;
    }

    public int getBlendingSourceFactor() {
        return sfactor;
    }

    public int getBlendingDestFactor() {
        return dfactor;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void render(final FrameBuffer src, final FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        glow.setInput(src).setOutput(dest).render();
    }

    public void setViewportSize(int width, int height) {
        this.glow.setViewportSize(width, height);
    }

    @Override
    public void rebind() {
        glow.rebind();
    }

    public static class Settings {
        public final String name;

        public Settings(String name) {
            this.name = name;
        }

        public Settings(Settings other) {
            this.name = other.name;
        }
    }
}
