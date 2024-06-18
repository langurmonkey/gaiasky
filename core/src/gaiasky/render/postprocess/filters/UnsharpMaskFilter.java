/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import gaiasky.render.util.ShaderLoader;

public final class UnsharpMaskFilter extends Filter<UnsharpMaskFilter> {
    private float u_sharpenFactor = 1f;

    /**
     * Creates an unsharp mask filter.
     */
    public UnsharpMaskFilter() {
        super(ShaderLoader.fromFile("screenspace", "unsharpmask"));
        rebind();
    }

    public void setSharpenFactor(float sf) {
        this.u_sharpenFactor = sf;
        setParam(Param.SharpenFactor, this.u_sharpenFactor);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.SharpenFactor, u_sharpenFactor);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        SharpenFactor("u_sharpenFactor", 0);
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
