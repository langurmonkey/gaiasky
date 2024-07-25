/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.math.MathUtils;
import gaiasky.render.util.ShaderLoader;
import gaiasky.util.Constants;

public final class ChromaticAberrationFilter extends Filter<ChromaticAberrationFilter> {
    private float aberrationAmount;

    /**
     * Creates a chromatic aberration filter with the given aberration amount.
     *
     * @param amount The aberration amount in [0,0.2].
     */
    public ChromaticAberrationFilter(float amount) {
        super(ShaderLoader.fromFile("screenspace", "chromaticaberration"));
        this.aberrationAmount = amount;
        rebind();
    }

    /**
     * Updates the chromatic aberration amount.
     *
     * @param amount The aberration amount in [0,0.2].
     */
    public void setAberrationAmount(float amount) {
        this.aberrationAmount = MathUtils.clamp(amount, Constants.MIN_CHROMATIC_ABERRATION_AMOUNT, Constants.MAX_CHROMATIC_ABERRATION_AMOUNT);
        setParam(Param.AberrationAmount, aberrationAmount);
    }

    public float getAberrationAmount() {
        return aberrationAmount;
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.AberrationAmount, aberrationAmount);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        AberrationAmount("u_aberrationAmount", 0);
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
