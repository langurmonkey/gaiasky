/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class Convolve1D extends Filter<Convolve1D> {
    public int length;
    public float[] weights;
    public float[] offsets;
    public Convolve1D(int length) {
        this(length, new float[length], new float[length * 2]);
    }

    public Convolve1D(int length, float[] weights_data) {
        this(length, weights_data, new float[length * 2]);
    }

    public Convolve1D(int length, float[] weights_data, float[] offsets) {
        super(ShaderLoader.fromFile("screenspace", "convolve-1d", "#define LENGTH " + length));
        setWeights(length, weights_data, offsets);
        rebind();
    }

    public void setWeights(int length, float[] weights, float[] offsets) {
        this.weights = weights;
        this.length = length;
        this.offsets = offsets;
    }

    @Override
    public void dispose() {
        super.dispose();
        weights = null;
        offsets = null;
        length = 0;
    }

    @Override
    public void rebind() {
        setParams(Param.Texture, u_texture0);
        setParamsv(Param.SampleWeights, weights, 0, length);
        setParamsv(Param.SampleOffsets, offsets, 0, length * 2 /* libgdx asks for number of floats, NOT number of elements! */);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        SampleWeights("SampleWeights", 1),
        SampleOffsets("SampleOffsets", 2 /* vec2 */);
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
