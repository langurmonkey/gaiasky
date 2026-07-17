/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import gaiasky.render.util.NoiseType;
import gaiasky.render.util.ShaderLoader;

public class NoiseFilter extends Filter<NoiseFilter> {
    /** Viewport size. **/
    protected final Vector2 viewport;
    /** Noise scale in x, y and z. **/
    protected final Vector3 scale = new Vector3(1, 1, 1);
    /** Color. **/
    protected final Vector4 color = new Vector4(1, 1, 1, 1);
    /** Base level. **/
    protected float baseLevel = 0.1f;
    /** Remap to [0,1] after base level operation. **/
    protected boolean remap = false;
    /** RNG seed. **/
    protected float seed = 1.23456f;
    /** Factor by which successive noise octaves decrease in amplitude. This is in (0, 1). **/
    protected float persistence = 0.5f;
    /** The initial frequency of the noise function. **/
    protected float frequency = 1.0f;
    /** Factor by which successive noise octaves increase in frequency. This is in [1, n). **/
    protected float lacunarity = 2f;
    /** Number of octaves. **/
    protected int octaves = 4;
    /** Apply smoothing function or not. **/
    protected boolean smoothing = false;
    /** Apply absolute value function. **/
    protected boolean turbulence = true;
    /** Convert the fBm to ridge noise. **/
    protected boolean ridge;
    /** Create different noise patterns in each of the different RGB channels. **/
    protected int channels = 1;
    /**
     * Plains parameters:
     * <ul>
     *     <li>
     *        Height - flatten the areas between base level and this level, in [0,1].
     *     </li>
     *     <li>
     *        Slope - the slope of the plains.
     *     </li>
     * </ul>
     **/
    protected final Vector2 plains = new Vector2(0.0f, 0.1f);

    /** The noise type. **/
    protected NoiseType type = NoiseType.SIMPLEX;

    public NoiseFilter(int viewportWidth, int viewportHeight, int targets, String shader) {
        this(new Vector2(viewportWidth, viewportHeight), targets, shader);
    }

    public NoiseFilter(Vector2 viewportSize, int targets) {
       this(viewportSize, targets, "biome");
    }

    public NoiseFilter(Vector2 viewportSize, int targets, String shader) {
        super(ShaderLoader.fromFile(
                "screenspace",
                shader ));
        this.viewport = viewportSize;

        rebind();
    }

    public NoiseFilter(ShaderProgram program, Vector2 viewportSize){
        super(program);
        this.viewport = viewportSize;
    }

    @Override
    public void updateProgram() {
        super.updateProgram(ShaderLoader.fromFile(
                "screenspace",
                "biome"));
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    public void setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
        setParam(Param.Color, this.color);
    }

    public void setScale(float scaleX, float scaleY, float scaleZ) {
        this.scale.set(scaleX, scaleY, scaleZ);
        setParam(Param.Scale, this.scale);
    }

    public void setBaseLevel(float baseLevel) {
        this.baseLevel = baseLevel;
        setParam(Param.BaseLevel, this.baseLevel);
    }

    public void setRemap(boolean remap) {
        this.remap = remap;
        setParam(Param.Remap, this.remap);
    }

    public void setScale(float scale) {
        setScale(scale, scale, scale);
    }

    public void setSeed(float seed) {
        this.seed = seed;
        setParam(Param.Seed, this.seed);
    }

    public void setPersistence(float p) {
        this.persistence = p;
        setParam(Param.Persistence, this.persistence);
    }

    public void setFrequency(float f) {
        this.frequency = f;
        setParam(Param.Frequency, this.frequency);
    }

    public void setLacunarity(float lacunarity) {
        this.lacunarity = lacunarity;
        setParam(Param.Lacunarity, this.lacunarity);
    }

    public void setOctaves(int octaves) {
        this.octaves = octaves;
        setParam(Param.Octaves, this.octaves);
    }

    public void setSmoothing(boolean smoothing) {
        this.smoothing = smoothing;
        setParam(Param.Smoothing, this.smoothing);
    }

    public void setTurbulence(boolean turbulence) {
        this.turbulence = turbulence;
        setParam(Param.Turbulence, this.turbulence);
    }

    public void setRidge(boolean ridge) {
        this.ridge = ridge;
        setParam(Param.Ridge, this.ridge);
    }

    public void setChannels(int channels) {
        this.channels = channels;
        setParam(Param.Channels, this.channels);
    }

    public void setType(NoiseType type) {
        this.type = type;
        setParam(Param.Type, this.type.ordinal());
    }

    public void setPlainsHeight(float ph) {
        this.plains.x = ph;
        setParam(Param.Plains, plains);
    }

    public void setPlainsSlope(float ps) {
        this.plains.y = ps;
        setParam(Param.Plains, plains);
    }

    protected void rebindNoEnd(){
        // Re-implement super to batch every parameter
        setParams(Param.Viewport, this.viewport);
        setParams(Param.Color, this.color);
        setParams(Param.Scale, this.scale);
        setParams(Param.Seed, this.seed);
        setParams(Param.BaseLevel, this.baseLevel);
        setParams(Param.Remap, this.remap);
        setParams(Param.Persistence, this.persistence);
        setParams(Param.Frequency, this.frequency);
        setParams(Param.Lacunarity, this.lacunarity);
        setParams(Param.Octaves, this.octaves);
        setParams(Param.Smoothing, this.smoothing);
        setParams(Param.Turbulence, this.turbulence);
        setParams(Param.Ridge, this.ridge);
        setParams(Param.Plains, this.plains);
        setParams(Param.Channels, this.channels);
        setParams(Param.Type, this.type.ordinal());
    }

    @Override
    public void rebind() {
        rebindNoEnd();

        endParams();
    }

    @Override
    protected void onBeforeRender() {
        if (inputTexture != null)
            inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),

        Viewport("u_viewport", 2),
        Seed("u_seed", 0),
        Amplitude("u_amplitude", 0),
        Persistence("u_persistence", 0),
        Frequency("u_frequency", 0),
        Lacunarity("u_lacunarity", 0),
        Color("u_color", 4),
        Scale("u_scale", 3),
        BaseLevel("u_baseLevel", 0),
        Remap("u_remap", 0),
        Power("u_power", 0),
        Octaves("u_octaves", 0),
        Smoothing("u_smoothing", 0),
        Turbulence("u_turbulence", 0),
        Ridge("u_ridge", 0),
        Plains("u_plains", 2),
        Channels("u_channels", 0),
        Type("u_type", 0);
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
