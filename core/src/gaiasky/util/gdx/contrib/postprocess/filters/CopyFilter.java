/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public class CopyFilter extends Filter<CopyFilter> {
    public CopyFilter() {
        super(ShaderLoader.fromFile("screenspace", "copy"));
    }

    @Override
    public void rebind() {
        setParam(Param.Texture0, u_texture0);
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        ;
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String m, int elementSize) {
            this.mnemonic = m;
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
