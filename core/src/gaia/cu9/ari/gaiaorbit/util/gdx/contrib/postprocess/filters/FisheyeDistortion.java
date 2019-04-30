/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/**
 * Fisheye distortion filter
 * 
 * @author tsagrista
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters;

import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.ShaderLoader;

public final class FisheyeDistortion extends Filter<FisheyeDistortion> {

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0);
        // @formatter:on

        private final String mnemonic;
        private int elementSize;

        private Param(String m, int elementSize) {
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

    public FisheyeDistortion() {
        super(ShaderLoader.fromFile("screenspace", "fisheye"));
        rebind();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);

        endParams();
    }
}
