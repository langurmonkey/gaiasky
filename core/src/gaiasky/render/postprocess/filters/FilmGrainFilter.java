/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.math.MathUtils;
import gaiasky.GaiaSky;
import gaiasky.util.Constants;
import gaiasky.render.util.ShaderLoader;

public final class FilmGrainFilter extends Filter<FilmGrainFilter> {

    private float intensity;
    /**
     * Creates a film grain filter with the given the intensity.
     *
     * @param intensity The intensity.
     */
    public FilmGrainFilter(float intensity) {
        super(ShaderLoader.fromFile("screenspace", "filmgrain"));
        this.intensity = intensity;
        rebind();
    }

    public void setIntensity(float intensity) {
        this.intensity = MathUtils.clamp(intensity, Constants.MIN_FILM_GRAIN_INTENSITY, Constants.MAX_FILM_GRAIN_INTENSITY);
        setParam(Param.Intensity, intensity);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.Intensity, intensity);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        setParam(Param.Time, (float) GaiaSky.instance.getRunTimeSeconds());
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        Intensity("u_intensity", 0),
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
