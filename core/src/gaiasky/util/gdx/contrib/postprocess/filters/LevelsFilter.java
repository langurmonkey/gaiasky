/*******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

/**
 * Controls levels of brightness and contrast
 */
public final class LevelsFilter extends Filter<LevelsFilter> {
    private final ShaderProgram programRegular;
    private final ShaderProgram programToneMappingExposure;
    private final ShaderProgram programToneMappingAuto;
    private final ShaderProgram programToneMappingACES;
    private final ShaderProgram programToneMappingUncharted;
    private final ShaderProgram programToneMappingFilmic;
    private float brightness = 0.0f;
    private float contrast = 1.0f;
    private float saturation = 1.0f;
    private float hue = 1.0f;
    private float gamma = 1.0f;
    private float exposure = 1.0f;
    private float avgLuma, maxLuma;

    public LevelsFilter() {
        super(ShaderLoader.fromFile("screenspace", "levels"));
        programRegular = program;
        programToneMappingExposure = ShaderLoader.fromFile("screenspace", "levels", "#define toneMappingExposure");
        programToneMappingAuto = ShaderLoader.fromFile("screenspace", "levels", "#define toneMappingAuto");
        programToneMappingACES = ShaderLoader.fromFile("screenspace", "levels", "#define toneMappingACES");
        programToneMappingUncharted = ShaderLoader.fromFile("screenspace", "levels", "#define toneMappingUncharted");
        programToneMappingFilmic = ShaderLoader.fromFile("screenspace", "levels", "#define toneMappingFilmic");
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

    public void setAvgMaxLuma(float avgLuma, float maxLuma) {
        this.avgLuma = avgLuma;
        this.maxLuma = maxLuma;
        setParam(Param.AvgLuma, this.avgLuma);
        setParam(Param.MaxLuma, this.maxLuma);
    }

    public void enableToneMappingExposure() {
        this.program = programToneMappingExposure;
        rebind();
    }

    public void enableToneMappingAuto() {
        this.program = programToneMappingAuto;
        rebind();
    }

    public void enableToneMappingACES() {
        this.program = programToneMappingACES;
        rebind();
    }

    public void enableToneMappingUncharted() {
        this.program = programToneMappingUncharted;
        rebind();
    }

    public void enableToneMappingFilmic() {
        this.program = programToneMappingFilmic;
        rebind();
    }

    public boolean isToneMappingAuto() {
        return this.program == programToneMappingAuto;
    }

    public void disableToneMapping() {
        this.program = programRegular;
        rebind();
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
        if (programToneMappingExposure != null && program == programToneMappingExposure) {
            setParams(Param.Exposure, exposure);
        }
        if (programToneMappingAuto != null && program == programToneMappingAuto) {
            setParams(Param.AvgLuma, this.avgLuma);
            setParams(Param.MaxLuma, this.maxLuma);
        }
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

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
