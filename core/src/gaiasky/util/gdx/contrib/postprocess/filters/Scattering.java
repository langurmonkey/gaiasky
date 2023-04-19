/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class Scattering extends Filter<Scattering> {
    // Number of light supported
    public static int N = 10;
    private final Vector2 viewport;
    /// NUM_SAMPLES will describe the rays quality, you can play with
    int NUM_SAMPLES = 100;
    private float[] lightPositions;
    private float[] lightViewAngles;
    private int nLights;
    private float decay = 0.96815f;
    private float density = 0.926f;
    private float weight = 0.58767f;
    private int numSamples = 100;

    public Scattering(int width, int height) {
        super(ShaderLoader.fromFile("screenspace", "lightscattering"));
        lightPositions = new float[N * 2];
        lightViewAngles = new float[N];
        viewport = new Vector2(width, height);
        rebind();
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    public void setLightPositions(int nLights, float[] pos) {
        this.nLights = nLights;
        this.lightPositions = pos;
        setParam(Param.NLights, this.nLights);
        setParamv(Param.LightPositions, this.lightPositions, 0, N * 2);
    }

    public void setLightViewAngles(float[] ang) {
        this.lightViewAngles = ang;
        setParamv(Param.LightViewAngles, this.lightViewAngles, 0, N);
    }

    public float getDecay() {
        return decay;
    }

    public void setDecay(float decay) {
        this.decay = decay;
        setParam(Param.Decay, decay);
    }

    public float getDensity() {
        return density;
    }

    public void setDensity(float density) {
        this.density = density;
        setParam(Param.Density, density);
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
        setParam(Param.Weight, weight);
    }

    public int getNumSamples() {
        return numSamples;
    }

    public void setNumSamples(int numSamples) {
        this.numSamples = numSamples;
        setParam(Param.NumSamples, numSamples);
    }

    @Override
    public void rebind() {
        // Re-implement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.NLights, this.nLights);
        setParams(Param.Viewport, viewport);
        setParamsv(Param.LightPositions, lightPositions, 0, N * 2);
        setParamsv(Param.LightViewAngles, lightViewAngles, 0, N);
        setParams(Param.Decay, decay);
        setParams(Param.Density, density);
        setParams(Param.Weight, weight);
        setParams(Param.NumSamples, numSamples);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        LightPositions("u_lightPositions", 2),
        LightViewAngles("u_lightViewAngles", 1),
        Viewport("u_viewport", 2),
        NLights("u_nLights", 0),
        Decay("u_decay", 0),
        Density("u_density", 0),
        Weight("u_weight", 0),
        NumSamples("u_numSamples", 0);
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
