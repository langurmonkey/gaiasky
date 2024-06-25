/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import gaiasky.render.util.ShaderLoader;

public final class NoiseFilter extends Filter<NoiseFilter> {
    /** Viewport size. **/
    private final Vector2 viewport;
    /** Noise scale in x, y and z. **/
    private final Vector3 scale = new Vector3(1, 1, 1);
    /** Final range of the noise values for the elevation channel. The other channels default to [0, 1]. **/
    private final Vector2 range = new Vector2(0, 1);
    /** Color. **/
    private final Vector4 color = new Vector4(1, 1, 1, 1);
    /** RNG seed. **/
    private float seed = 1.23456f;
    /** The initial frequency. **/
    private float frequency = 1.0f;
    /** Factor by which successive noise octaves decrease in amplitude. This is in (0, 1). **/
    private float persistence = 0.5f;
    /** Factor by which successive noise octaves increase in frequency. This is in [1, n). **/
    private float lacunarity = 2f;
    /** Exponent to apply to the generated noise in a power function. **/
    private float power = 1.3f;
    /** Number of octaves. **/
    private int octaves = 4;
    /** Apply absolute value function. **/
    private boolean turbulence = true;
    /** Convert the fBm to ridge noise. **/
    private boolean ridge = false;
    /** Number of terraces to use in the height profile. Set to 0 to disable. **/
    private int numTerraces = 0;
    /** Exponent of terraces. Must be odd. The lower it is, the smoother the terrace transitions. **/
    private float terraceExp = 17.0f;
    /** Create different noise patterns in each of the different RGB channels. **/
    private int channels = 1;

    /**
     * <p>The type of noise:</p>
     * <ol>
     *   <li>Perlin</li>
     *   <li>Simplex</li>
     *   <li>Voronoi</li>
     *   <li>Curl</li>
     *   <li>White</li>
     * </ol>
     */
    public enum NoiseType {
        PERLIN, SIMPLEX, VORONOI, CURL, WHITE
    }

    private NoiseType type = NoiseType.SIMPLEX;

    public NoiseFilter(float viewportWidth, float viewportHeight) {
        this(new Vector2(viewportWidth, viewportHeight));
    }

    public NoiseFilter(Vector2 viewportSize) {
        super(ShaderLoader.fromFile("screenspace", "noise"));
        this.viewport = viewportSize;

        rebind();
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    public void setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
        setParam(Param.Color, this.color);
    }

    public void setRange(float a, float b) {
        this.range.set(a, b);
        setParam(Param.Range, this.range);
    }

    public void setScale(float scaleX, float scaleY, float scaleZ) {
        this.scale.set(scaleX, scaleY, scaleZ);
        setParam(Param.Scale, this.scale);
    }

    public void setScale(float scale) {
        setScale(scale, scale, scale);
    }

    public void setSeed(float seed) {
        this.seed = seed;
        setParam(Param.Seed, this.seed);
    }

    public void setFrequency(float f) {
        this.frequency = f;
        setParam(Param.Frequency, this.frequency);
    }

    public void setPersistence(float p) {
        this.persistence = p;
        setParam(Param.Persistence, this.persistence);
    }

    public void setLacunarity(float lacunarity) {
        this.lacunarity = lacunarity;
        setParam(Param.Lacunarity, this.lacunarity);
    }

    public void setPower(float power) {
        this.power = power;
        setParam(Param.Power, this.power);
    }

    public void setOctaves(int octaves) {
        this.octaves = octaves;
        setParam(Param.Octaves, this.octaves);
    }

    public void setTurbulence(boolean turbulence) {
        this.turbulence = turbulence;
        setParam(Param.Turbulence, this.turbulence);
    }

    public void setRidge(boolean ridge) {
        this.ridge = ridge;
        setParam(Param.Ridge, this.ridge);
    }

    public void setNumTerraces(int nt) {
        this.numTerraces = nt;
        setParam(Param.NumTerraces, this.numTerraces);
    }

    public void setTerraceExp(float te) {
        this.terraceExp = te;
        setParam(Param.TerraceExp, this.terraceExp);
    }

    public void setChannels(int channels) {
        this.channels = channels;
        setParam(Param.Channels, this.channels);
    }

    public void setType(NoiseType type) {
        this.type = type;
        setParam(Param.Type, this.type.ordinal());
    }

    @Override
    public void rebind() {
        // Re-implement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.Viewport, viewport);
        setParams(Param.Range, this.range);
        setParams(Param.Color, this.color);
        setParams(Param.Scale, this.scale);
        setParams(Param.Seed, this.seed);
        setParams(Param.Frequency, this.frequency);
        setParams(Param.Persistence, this.persistence);
        setParams(Param.Lacunarity, this.lacunarity);
        setParams(Param.Power, this.power);
        setParams(Param.Octaves, this.octaves);
        setParams(Param.Turbulence, this.turbulence);
        setParams(Param.Ridge, this.ridge);
        setParams(Param.NumTerraces, this.numTerraces);
        setParams(Param.TerraceExp, this.terraceExp);
        setParams(Param.Channels, this.channels);
        setParams(Param.Type, this.type.ordinal());

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
        Frequency("u_frequency", 0),
        Persistence("u_persistence", 0),
        Lacunarity("u_lacunarity", 0),
        Range("u_range", 2),
        Color("u_color", 4),
        Scale("u_scale", 3),
        Power("u_power", 0),
        Octaves("u_octaves", 0),
        Turbulence("u_turbulence", 0),
        Ridge("u_ridge", 0),
        NumTerraces("u_numTerraces", 0),
        TerraceExp("u_terraceExp", 0),
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
