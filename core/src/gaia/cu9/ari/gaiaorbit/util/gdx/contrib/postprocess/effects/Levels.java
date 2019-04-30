/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2012 tsagrista
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

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.LevelsFilter;

/** Implements brightness, contrast, hue and saturation levels
 * @author tsagrista */
public final class Levels extends PostProcessorEffect {
    private LevelsFilter filter = null;

    /** Creates the effect */
    public Levels() {
        filter = new LevelsFilter();
    }

    /** Set the brightness
    	 * @param value The brightness value in [-1..1] */
    public void setBrightness(float value) {
        filter.setBrightness(value);
    }

    /** Set the saturation
    	 * @param value The saturation value in [0..2] */
    public void setSaturation(float value) {
        filter.setSaturation(value);
    }

    /** Set the hue
    	 * @param value The hue value in [0..2] */
    public void setHue(float value) {
        filter.setHue(value);
    }

    /** Set the contrast
    	 * @param value The contrast value in [0..2] */
    public void setContrast(float value) {
        filter.setContrast(value);
    }

    /** 
     * Sets the gamma correction value
     * @param value The gamma value in [0..3]
     */
    public void setGamma(float value) {
        filter.setGamma(value);
    }

    @Override
    public void dispose() {
        if (filter != null) {
            filter.dispose();
            filter = null;
        }
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }
}
