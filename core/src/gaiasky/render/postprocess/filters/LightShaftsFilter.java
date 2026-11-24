/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.render.util.ShaderLoader;

/**
 * Marches the light mask towards the light and adds the result to the frame, which turns the
 * partially occluded mask into shafts.
 *
 * @see gaiasky.render.postprocess.effects.LightShafts
 */
public final class LightShaftsFilter extends Filter<LightShaftsFilter> {
    private final Vector2 lightPos = new Vector2();
    private final Vector3 lightColor = new Vector3(1f, 1f, 1f);
    private Texture maskTexture;
    private float strength;
    private float density = 1f;
    private float decay = 0.97f;
    private int samples = 32;

    public LightShaftsFilter() {
        super(ShaderLoader.fromFile("screenspace", "lightshafts"));
        rebind();
    }

    /** Sets the mask built by the first pass. */
    public void setMaskTexture(Texture tex) {
        this.maskTexture = tex;
        setParam(Param.Texture1, u_texture1);
    }

    /** Sets the position of the light on screen, in [0,1]. */
    public void setLightPos(float u, float v) {
        this.lightPos.set(u, v);
        setParam(Param.LightPos, this.lightPos);
    }

    /** Sets the color of the light. */
    public void setLightColor(float r, float g, float b) {
        this.lightColor.set(r, g, b);
        setParam(Param.LightColor, this.lightColor);
    }

    /** Sets the strength of the effect. At or below zero the frame is copied through. */
    public void setStrength(float strength) {
        this.strength = strength;
        setParam(Param.Strength, strength);
    }

    public float getStrength() {
        return strength;
    }

    /** Sets the number of samples along each ray. */
    public void setSamples(int samples) {
        this.samples = Math.max(4, samples);
        setParam(Param.Samples, this.samples);
    }

    /** Sets the fraction of the ray to the light that is marched, in [0,1]. */
    public void setDensity(float density) {
        this.density = density;
        setParam(Param.Density, density);
    }

    /** Sets the attenuation of a sample against the previous one. */
    public void setDecay(float decay) {
        this.decay = decay;
        setParam(Param.Decay, decay);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Texture1, u_texture1);
        setParams(Param.LightPos, lightPos);
        setParams(Param.LightColor, lightColor);
        setParams(Param.Strength, strength);
        setParams(Param.Density, density);
        setParams(Param.Decay, decay);
        setParams(Param.Samples, samples);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        if (maskTexture != null) {
            maskTexture.bind(u_texture1);
        }
        inputTexture.bind(u_texture0);
        // Animates the dithering that hides the banding of the accumulation.
        setParam(Param.Time, (float) GaiaSky.instance.getT());
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Texture1("u_texture1", 0),
        LightPos("u_lightPos", 2),
        LightColor("u_lightColor", 3),
        Strength("u_strength", 0),
        Density("u_density", 0),
        Decay("u_decay", 0),
        Samples("u_samples", 0),
        Time("u_time", 0);
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
