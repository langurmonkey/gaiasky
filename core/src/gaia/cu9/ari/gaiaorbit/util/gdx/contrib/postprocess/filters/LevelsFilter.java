/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters;

import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.ShaderLoader;

/**
 * Controls levels of brightness and contrast
 *
 * @author tsagrista
 */
public final class LevelsFilter extends Filter<LevelsFilter> {
    private float brightness = 0.0f;
    private float contrast = 1.0f;
    private float saturation = 1.0f;
    private float hue = 1.0f;
    private float gamma = 1.0f;
    private float exposure = 1.0f;
    private float avgLuma, maxLuma;

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        Brightness("u_brightness", 0),
        Contrast("u_contrast", 0),
        Saturation("u_saturation", 0),
        Hue("u_hue", 0),
        Exposure("u_exposure", 0),
        AvgLuma("u_avgLuma", 0),
        MaxLuma("u_maxLuma", 0),
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

    public LevelsFilter() {
        super(ShaderLoader.fromFile("screenspace", "levels"));
        rebind();
    }

    /**
     * Sets the contrast level
     *
     * @param contrast The contrast value in [0..2]
     */
    public void setContrast(float contrast) {
        this.contrast = contrast;
        setParam(Param.Contrast, this.contrast);
    }

    /**
     * Sets the brightness level
     *
     * @param brightness The brightness value in [-1..1]
     */
    public void setBrightness(float brightness) {
        this.brightness = brightness;
        setParam(Param.Brightness, this.brightness);
    }

    /**
     * Sets the saturation
     *
     * @param saturation The saturation level in [0..2]
     */
    public void setSaturation(float saturation) {
        this.saturation = saturation;
        setParam(Param.Saturation, this.saturation);
    }

    /**
     * Sets the hue
     *
     * @param hue The hue level in [0..2]
     */
    public void setHue(float hue) {
        this.hue = hue;
        setParam(Param.Hue, this.hue);
    }

    /**
     * Sets the gamma correction value
     *
     * @param gamma Gamma value in [0..3]
     */
    public void setGamma(float gamma) {
        this.gamma = gamma;
        setParam(Param.Gamma, this.gamma);
    }

    /**
     * Sets the exposure tone mapping value
     *
     * @param exposure Exposure value in [0..n]
     */
    public void setExposure(float exposure) {
        this.exposure = exposure;
        setParam(Param.Exposure, this.exposure);
    }

    public void setAvgMaxLuma(float avgLuma, float maxLuma){
        this.avgLuma = avgLuma;
        this.maxLuma = maxLuma;
        setParam(Param.AvgLuma, this.avgLuma);
        setParam(Param.MaxLuma, this.maxLuma);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.Brightness, brightness);
        setParams(Param.Contrast, contrast);
        setParams(Param.Saturation, saturation);
        setParams(Param.Hue, hue);
        setParams(Param.Gamma, gamma);
        setParams(Param.Exposure, exposure);
        setParams(Param.AvgLuma, this.avgLuma);
        setParams(Param.MaxLuma, this.maxLuma);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }
}
