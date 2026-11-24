/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import gaiasky.GaiaSky;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.LightShaftsFilter;
import gaiasky.render.postprocess.filters.LightShaftsMaskFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.util.Constants;

/**
 * Light shafts ("god rays") around a nearby star, when the star is in view and further away than
 * the objects in front of it.
 * <p>
 * The first pass builds a mask with the lit halo of the star, dropped wherever the depth buffer
 * shows geometry in front of it. The second pass marches that mask towards the star and adds the
 * result to the frame, so a body crossing the line of sight casts shafts across the sky.
 * <p>
 * The strength is gated on the apparent magnitude of the star (the same figure the light glow uses)
 * and on its distance, so the effect fades away once the camera leaves the neighbourhood of a star.
 */
public final class LightShafts extends PostProcessorEffect {
    /** Distance, in AU, where the effect starts to fade out, and where it is gone. */
    private static final double FADE_START_AU = 500.0, FADE_END_AU = 5000.0;
    /** Intensity below which a light does not produce shafts. */
    private static final float MIN_INTENSITY = 0.05f;
    /** Margin, in uv, around the screen where the strength fades to zero. */
    private static final float EDGE_MARGIN = 0.1f;

    private final LightShaftsMaskFilter maskFilter;
    private final LightShaftsFilter shaftsFilter;
    private final int maskWidth, maskHeight;
    private FrameBuffer maskBuffer;

    private float[] lightPositions, lightDepths, lightColors, lightIntensities;
    private int nLights;
    private float baseStrength = 0.6f;
    private float radius = 0.06f;

    public LightShafts(int maskWidth, int maskHeight) {
        maskFilter = new LightShaftsMaskFilter();
        shaftsFilter = new LightShaftsFilter();
        this.maskWidth = Math.max(1, maskWidth);
        this.maskHeight = Math.max(1, maskHeight);
        maskBuffer = createMaskBuffer();
        disposables.addAll(maskFilter, shaftsFilter);
    }

    private FrameBuffer createMaskBuffer() {
        return new FrameBuffer(Format.RGBA8888, maskWidth, maskHeight, false);
    }

    /** Maximum strength of the effect, before the gating on the light. */
    public void setStrength(float strength) {
        this.baseStrength = strength;
    }

    /** Radius of the lit halo around the light, in uv. */
    public void setRadius(float radius) {
        this.radius = radius;
    }

    /** Number of samples along each ray. */
    public void setSamples(int samples) {
        shaftsFilter.setSamples(samples);
    }

    /**
     * Sets the data of the lights of the frame. The arrays are the ones of the light position
     * updater, so they are only valid during the frame in which they are published.
     */
    public void setLights(int nLights, float[] positions, float[] depths, float[] colors, float[] intensities) {
        this.nLights = nLights;
        this.lightPositions = positions;
        this.lightDepths = depths;
        this.lightColors = colors;
        this.lightIntensities = intensities;
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        // Both passes write the whole frame, so any blending left over from a previous effect is off.
        Gdx.gl.glDisable(GL20.GL_BLEND);

        var depth = full != null ? full.getDepthBufferTexture() : null;
        float strength = depth != null ? computeStrength() : 0f;
        // With no light to scatter the frame is just copied through, which also keeps the effect
        // free when nothing triggers it.
        shaftsFilter.setStrength(strength);
        if (strength > 0f) {
            maskFilter.setInput(depth).setOutput(maskBuffer).render();
            shaftsFilter.setMaskTexture(maskBuffer.getColorBufferTexture());
        }
        // The mask pass leaves the viewport at the size of its buffer, so it is restored here.
        restoreViewport(dest);
        shaftsFilter.setInput(src).setOutput(dest).render();
    }

    /**
     * Picks the light with the strongest shafts in view and uploads its data, returning the strength
     * of the effect for the frame, which is zero when no light qualifies.
     */
    private float computeStrength() {
        var settings = GaiaSky.settings();
        // The light is projected once for the whole screen, and the depth buffer is only usable then.
        if (settings.runtime.openXr || settings.program.modeCubemap.active || settings.program.modeStereo.active) {
            return 0f;
        }

        int best = -1;
        float bestStrength = 0f;
        for (int i = 0; i < nLights; i++) {
            float u = lightPositions[i * 2], v = lightPositions[i * 2 + 1];
            // Behind the camera, or far enough outside the screen that there is nothing to draw.
            if (lightDepths[i] <= 0f || lightIntensities[i] < MIN_INTENSITY
                    || u < -EDGE_MARGIN || u > 1f + EDGE_MARGIN || v < -EDGE_MARGIN || v > 1f + EDGE_MARGIN) {
                continue;
            }
            // The shafts hold their strength with the apparent magnitude, and the distance fades them
            // out: only the neighbourhood of a star scatters enough light.
            double distanceAu = lightDepths[i] * Constants.U_TO_AU;
            double distanceFade = MathUtils.clamp((FADE_END_AU - distanceAu) / (FADE_END_AU - FADE_START_AU), 0.0, 1.0);
            // Fade the light out as it leaves the screen.
            float edgeFade = MathUtils.clamp(Math.min(Math.min(u, 1f - u), Math.min(v, 1f - v)) / EDGE_MARGIN, 0f, 1f);

            float strength = baseStrength * lightIntensities[i] * (float) distanceFade * edgeFade;
            if (strength > bestStrength) {
                bestStrength = strength;
                best = i;
            }
        }
        if (best < 0) {
            return 0f;
        }

        float u = lightPositions[best * 2], v = lightPositions[best * 2 + 1];
        var cam = GaiaSky.instance.getICamera();
        maskFilter.setZFarK((float) cam.getFar(), Constants.getCameraK());
        maskFilter.setLightPos(u, v);
        maskFilter.setLightDepth(lightDepths[best]);
        maskFilter.setLightRadius(radius);
        shaftsFilter.setLightPos(u, v);
        shaftsFilter.setLightColor(lightColors[best * 3], lightColors[best * 3 + 1], lightColors[best * 3 + 2]);
        return bestStrength;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (maskBuffer != null) {
            maskBuffer.dispose();
            maskBuffer = null;
        }
    }

    @Override
    public void rebind() {
        maskFilter.rebind();
        shaftsFilter.rebind();
        // The buffer is a plain GL object, so it does not survive a context loss by itself.
        if (maskBuffer != null) {
            maskBuffer.dispose();
        }
        maskBuffer = createMaskBuffer();
    }
}
