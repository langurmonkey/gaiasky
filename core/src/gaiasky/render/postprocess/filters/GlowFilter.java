/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import gaiasky.GaiaSky;
import gaiasky.render.util.ShaderLoader;

/**
 * A filter that implements a glow effect using a shader.
 * <p>
 * This filter takes light sources (positions, intensities, and colors) and the depth buffer of the
 * scene to render a glow effect occluded by the objects in front of each light. It is used as a
 * sub-component of the {@link gaiasky.render.postprocess.effects.LightGlow} effect.
 */
public final class GlowFilter extends Filter<GlowFilter> {
    private final Vector2 viewport;

    private float[] lightPositions;
    private float[] lightViewAngles;
    private float[] lightDepths;
    private float[] lightIntensities;
    private float[] lightColors;
    private int nLights;
    private float textureScale = 1f;
    private float backBufferScale = 1f;
    private final Vector2 zFarK = new Vector2();
    private final Vector2 depthTexel = new Vector2(1f / 1920f, 1f / 1080f);
    private float occlusion;
    private float fovFactor = 1f;

    // Depth buffer of the scene, to drop the glow of the lights hidden behind other objects.
    private Texture depthTexture;
    private Texture lightGlowTexture;

    public GlowFilter(int width, int height) {
        super(ShaderLoader.fromFile("screenspace", "lightglow"));
        viewport = new Vector2(width, height);
        rebind();
    }

    public void setBackBufferScale(float s) {
        this.backBufferScale = s;
        setParam(Param.BackbufferScale, s);
    }

    /** Sets the field of view factor of the camera. */
    public void setFovFactor(float fovFactor) {
        this.fovFactor = fovFactor;
        setParam(Param.FovFactor, fovFactor);
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    public void setLightPositions(int nLights, float[] vec) {
        setNLights(nLights);
        this.lightPositions = vec;
        setParamv(Param.LightPositions, this.lightPositions, 0, this.nLights * 2);
    }

    public void setNLights(int nLights) {
        this.nLights = nLights;
        setParam(Param.NLights, this.nLights);
    }

    public void setLightViewAngles(float[] ang) {
        this.lightViewAngles = ang;
        setParamv(Param.LightViewAngles, this.lightViewAngles, 0, nLights);
    }

    /** Sets the glow intensity of each light, in [0,1], computed from its apparent magnitude. */
    public void setLightIntensities(float[] intensities) {
        this.lightIntensities = intensities;
        if (this.lightIntensities != null && nLights > 0) {
            setParamv(Param.LightIntensities, this.lightIntensities, 0, nLights);
        }
    }

    /** Sets the distance from the camera to each light, in the same space as the depth buffer. */
    public void setLightDepths(float[] depths) {
        this.lightDepths = depths;
        if (this.lightDepths != null && nLights > 0) {
            setParamv(Param.LightDepths, this.lightDepths, 0, nLights);
        }
    }

    /** Sets the z-far and K of the logarithmic depth buffer, to recover a distance from a depth value. */
    public void setZFarK(float zfar, float k) {
        this.zFarK.set(zfar, k);
        setParam(Param.ZFarK, this.zFarK);
    }

    /** Sets the size of one texel of the depth buffer, in uv coordinates. */
    public void setDepthTexelSize(float width, float height) {
        this.depthTexel.set(1f / width, 1f / height);
        setParam(Param.DepthTexel, this.depthTexel);
    }

    /** Enables or disables the occlusion of the lights by the geometry in front of them. */
    public void setOcclusionEnabled(boolean enabled) {
        this.occlusion = enabled ? 1f : 0f;
        setParam(Param.Occlusion, occlusion);
    }

    public void setLightColors(float[] colors) {
        this.lightColors = colors;
        setParamv(Param.LightColors, this.lightColors, 0, nLights * 3);
    }

    /**
     * Sets the maximum halo radius, in pixels at the reference height.
     */
    public void setTextureScale(float scl) {
        this.textureScale = scl;
        setParam(Param.TextureScale, textureScale);
    }

    public Texture getLightGlowTexture() {
        return lightGlowTexture;
    }

    public void setLightGlowTexture(Texture tex) {
        lightGlowTexture = tex;
        setParam(Param.LightGlowTexture, u_texture1);
    }

    public Texture getDepthTexture() {
        return depthTexture;
    }

    public void setDepthTexture(Texture tex) {
        depthTexture = tex;
        setParam(Param.DepthTexture, u_texture2);
    }

    @Override
    public void rebind() {
        // Re-implement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.LightGlowTexture, u_texture1);
        setParams(Param.DepthTexture, u_texture2);
        setParams(Param.TextureScale, textureScale);
        setParams(Param.Viewport, viewport);
        setParams(Param.BackbufferScale, backBufferScale);
        setParams(Param.ZFarK, zFarK);
        setParams(Param.DepthTexel, depthTexel);
        setParams(Param.Occlusion, occlusion);
        setParams(Param.FovFactor, fovFactor);
        setParams(Param.NLights, nLights);
        if (lightPositions != null)
            setParamsv(Param.LightPositions, lightPositions, 0, nLights * 2);
        if (lightViewAngles != null)
            setParamsv(Param.LightViewAngles, lightViewAngles, 0, nLights);
        if (lightDepths != null && nLights > 0)
            setParamsv(Param.LightDepths, lightDepths, 0, nLights);
        if (lightIntensities != null && nLights > 0)
            setParamsv(Param.LightIntensities, lightIntensities, 0, nLights);
        if (lightColors != null)
            setParamsv(Param.LightColors, lightColors, 0, nLights * 3);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        if (lightGlowTexture != null)
            lightGlowTexture.bind(u_texture1);
        if (depthTexture != null)
            depthTexture.bind(u_texture2);
        inputTexture.bind(u_texture0);
        // The angular mask rotates with time. The clock is the frame-time accumulator, so a capture
        // at 30 fps renders the same streak motion as real time at 300 fps.
        setParam(Param.Time, (float) GaiaSky.instance.getT());
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        LightGlowTexture("u_texture1", 0),
        DepthTexture("u_texture2", 0),
        LightPositions("u_lightPositions", 2),
        LightViewAngles("u_lightViewAngles", 1),
        LightDepths("u_lightDepths", 1),
        LightIntensities("u_lightIntensities", 1),
        LightColors("u_lightColors", 3),
        Viewport("u_viewport", 2),
        NLights("u_nLights", 0),
        BackbufferScale("u_backbufferScale", 0),
        Time("u_time", 0),
        ZFarK("u_zFarK", 2),
        DepthTexel("u_depthTexel", 2),
        Occlusion("u_occlusion", 0),
        FovFactor("u_fovFactor", 0),
        TextureScale("u_textureScale", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int arrayElementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = arrayElementSize;
        }

        @Override
        public String mnemonic() {
            return this.mnemonic;
        }

        @Override
        public int arrayElementSize() {
            return this.elementSize;
        }
    }
}
