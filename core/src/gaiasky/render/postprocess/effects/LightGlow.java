/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.GaiaSky;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.GlowFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.util.Constants;

/**
 * A post-processor effect that renders a glow around light sources like stars and planets.
 * <p>
 * It uses a {@link GlowFilter} to draw a halo around each light. The intensity of a halo comes from
 * the apparent magnitude of the light, its size from the angular size, and the depth buffer of the
 * scene drops the halos of the lights hidden behind other objects.
 */
public final class LightGlow extends PostProcessorEffect {
    private final GlowFilter glow;
    private Settings settings;
    private boolean blending;
    private int sFactor, dFactor;

    public LightGlow(int width, int height) {
        glow = new GlowFilter(width, height);
        disposables.add(glow);
    }

    public void setBackBufferScale(float bbs) {
        glow.setBackBufferScale(bbs);
    }

    public void setLightPositions(int nLights, float[] vec) {
        glow.setLightPositions(nLights, vec);
    }

    public void setNLights(int nLights) {
        glow.setNLights(nLights);
    }

    public void setLightSolidAngles(float[] vec) {
        glow.setLightViewAngles(vec);
    }

    /** Sets the distance from the camera to each light. */
    public void setLightDepths(float[] vec) {
        glow.setLightDepths(vec);
    }

    /** Sets the glow intensity of each light, in [0,1], computed from its apparent magnitude. */
    public void setLightIntensities(float[] vec) {
        glow.setLightIntensities(vec);
    }

    public void setLightColors(float[] vec) {
        glow.setLightColors(vec);
    }

    /** Sets the field of view factor of the camera. */
    public void setFovFactor(float fovFactor) {
        glow.setFovFactor(fovFactor);
    }

    /** Sets the maximum halo radius, in pixels at the reference height. */
    public void setTextureScale(float scl) {
        glow.setTextureScale(scl);
    }

    public void enableBlending(int sFactor, int dFactor) {
        this.blending = true;
        this.sFactor = sFactor;
        this.dFactor = dFactor;
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

    public Texture getDepthTexture() {
        return glow.getDepthTexture();
    }

    public void setDepthTexture(Texture tex) {
        glow.setDepthTexture(tex);
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

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        // Depth buffer of the scene, so that a light does not glow through the objects in front of
        // it. Only usable outside stereo and cubemap, where the lights are projected once.
        var depth = full != null ? full.getDepthBufferTexture() : null;
        var settings = GaiaSky.settings();
        var occlusion = depth != null && !settings.runtime.openXr && !settings.program.modeStereo.active
                && !settings.program.modeCubemap.active;
        if (occlusion) {
            var cam = GaiaSky.instance.getICamera();
            glow.setZFarK((float) cam.getFar(), Constants.getCameraK());
            glow.setDepthTexelSize(full.getWidth(), full.getHeight());
        }
        glow.setDepthTexture(depth);
        glow.setOcclusionEnabled(occlusion);

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

    public record Settings(String name) {

        public Settings(Settings other) {
                this(other.name);
            }
        }
}
