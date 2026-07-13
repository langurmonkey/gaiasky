/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import gaiasky.render.postprocess.filters.CloudsFilter.NoiseType;
import gaiasky.render.util.ShaderLoader;

public final class ProceduralSurfaceFilter extends Filter<ProceduralSurfaceFilter> {
    /** LUT texture. **/
    private Texture lut;
    /** LUT hue shift. **/
    float lutHueShift;
    /** LUT saturation. **/
    float lutSaturation = 1;

    /** Viewport size. **/
    private final Vector2 viewport;
    /** Noise scale in x, y and z. **/
    private final Vector3 scale = new Vector3(1, 1, 1);
    /** Color. **/
    private final Vector4 color = new Vector4(1, 1, 1, 1);
    /** Base level. **/
    private float baseLevel = 0.1f;
    /** Remap to [0,1] after base level operation. **/
    private boolean remap = false;
    /** RNG seed. **/
    private float seed = 1.23456f;
    /** Factor by which successive noise octaves decrease in amplitude. This is in (0, 1). **/
    private float persistence = 0.5f;
    /** The initial frequency of the noise function. **/
    private float frequency = 1.0f;
    /** Factor by which successive noise octaves increase in frequency. This is in [1, n). **/
    private float lacunarity = 2f;
    /** Number of octaves. **/
    private int octaves = 4;
    /** Apply smoothing function or not. **/
    private boolean smoothing = false;
    /** Apply absolute value function. **/
    private boolean turbulence = true;
    /** Convert the fBm to ridge noise. **/
    private boolean ridge;
    /** Create different noise patterns in each of the different RGB channels. **/
    private int channels = 1;

    private final boolean genNormalMap, genEmissiveMap;


    private NoiseType type = NoiseType.SIMPLEX;

    public ProceduralSurfaceFilter(int viewportWidth,
                                   int viewportHeight,
                                   boolean normalMap,
                                   boolean emissiveMap) {
        this(new Vector2(viewportWidth, viewportHeight), normalMap, emissiveMap);
    }

    public ProceduralSurfaceFilter(Vector2 viewportSize,
                                   boolean normalMap,
                                   boolean emissiveMap) {
        super(ShaderLoader.fromFile(
                "screenspace",
                "proceduralsurface",
                (normalMap ? "#define normalMapFlag\n" : "") +
                        (emissiveMap ? "#define emissiveMapFlag\n" : "")));
        this.genNormalMap = normalMap;
        this.genEmissiveMap = emissiveMap;
        this.viewport = viewportSize;

        rebind();
    }

    @Override
    public void updateProgram() {
        super.updateProgram(ShaderLoader.fromFile(
                "screenspace",
                "proceduralsurface",
                (genNormalMap ? "#define normalMapFlag\n" : "") +
                        (genEmissiveMap ? "#define emissiveMapFlag\n" : "")));
    }

    public void setViewportSize(float width,
                                float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    public void setColor(float r,
                         float g,
                         float b,
                         float a) {
        this.color.set(r, g, b, a);
        setParam(Param.Color, this.color);
    }

    public void setScale(float scaleX,
                         float scaleY,
                         float scaleZ) {
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

    public void setLutTexture(Texture lut) {
        this.lut = lut;
        setParam(Param.TextureLut, u_texture1);
    }

    public void setLutHueShift(float hs) {
        this.lutHueShift = hs;
        setParam(Param.LutHueShift, lutHueShift);
    }

    public void setLutSaturation(float hs) {
        this.lutSaturation = hs;
        setParam(Param.LutSaturation, lutSaturation);
    }

    @Override
    public void rebind() {
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
        setParams(Param.Channels, this.channels);
        setParams(Param.Type, this.type.ordinal());
        setParams(Param.TextureLut, u_texture1);
        setParams(Param.LutSaturation, lutSaturation);
        setParams(Param.LutHueShift, lutHueShift);

        endParams();
    }

    @Override
    protected void onBeforeRender() {
        if (inputTexture != null)
            inputTexture.bind(u_texture0);
        if (lut != null)
            lut.bind(u_texture1);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Viewport("u_viewport", 2),
        Seed("u_seed", 0),
        Persistence("u_persistence", 0),
        Frequency("u_frequency", 0),
        Lacunarity("u_lacunarity", 0),
        Color("u_color", 4),
        Scale("u_scale", 3),
        BaseLevel("u_baseLevel", 0),
        Remap("u_remap", 0),
        Octaves("u_octaves", 0),
        Smoothing("u_smoothing", 0),
        Turbulence("u_turbulence", 0),
        Ridge("u_ridge", 0),
        Channels("u_channels", 0),
        Type("u_type", 0),
        TextureLut("u_texture1", 0),
        LutSaturation("u_lutSaturation", 0),
        LutHueShift("u_lutHueShift", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic,
              int arrayElementSize) {
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