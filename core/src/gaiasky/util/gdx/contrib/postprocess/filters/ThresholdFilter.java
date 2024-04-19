/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class ThresholdFilter extends Filter<ThresholdFilter> {

    private float threshold = 0;

    public ThresholdFilter() {
        super(ShaderLoader.fromFile("screenspace", "threshold"));
        rebind();
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
        setParam(Param.Threshold, threshold);
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture, u_texture0);
        setParam(Param.Threshold, threshold);
        endParams();
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        Threshold("u_threshold", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int elementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = elementSize;
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
