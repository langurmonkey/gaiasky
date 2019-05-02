/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters;

import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.ShaderLoader;

/**
 * HDR filter.
 *
 * @author Toni Sagrista
 */
public final class HDRFilter extends Filter<HDRFilter> {
    private float exposure;
    private float gamma;

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        Exposure("u_exposure", 0),
        Gamma("u_gamma", 0);
        // @formatter:on

        private String mnemonic;
        private int elementSize;

        private Param(String mnemonic, int arrayElementSize) {
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

    public HDRFilter() {
        this(3.0f, 2.2f);
    }

    public HDRFilter(float exposure, float gamma) {
        super(ShaderLoader.fromFile("screenspace", "hdr"));
        this.exposure = exposure;
        this.gamma = gamma;
    }

    public void setExposure(float exposure) {
        this.exposure = exposure;
        setParam(Param.Exposure, this.exposure);
    }

    public void setGamma(float gamma) {
        this.gamma = gamma;
        setParam(Param.Gamma, this.gamma);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.Exposure, exposure);
        setParams(Param.Gamma, gamma);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }
}
