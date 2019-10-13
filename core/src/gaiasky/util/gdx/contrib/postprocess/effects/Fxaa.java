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

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.filters.FxaaFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Implements the fast approximate anti-aliasing. Very fast and useful for combining with other post-processing effects.
 *
 * @author Toni Sagrista
 */
public final class Fxaa extends Antialiasing {
    private FxaaFilter fxaaFilter = null;

    /** Create a FXAA with the viewport size */
    public Fxaa(int viewportWidth, int viewportHeight) {
        setup(viewportWidth, viewportHeight);
    }

    private void setup(int viewportWidth, int viewportHeight) {
        fxaaFilter = new FxaaFilter(viewportWidth, viewportHeight);
    }

    public void setViewportSize(int width, int height) {
        fxaaFilter.setViewportSize(width, height);
    }

    /**
     * Sets the span max parameter. The default value is 8.
     *
     * @param value
     */
    public void setSpanMax(float value) {
        fxaaFilter.setFxaaSpanMax(value);
    }

    /**
     * Sets the parameter. The default value is 1/128.
     *
     * @param value
     */
    public void setReduceMin(float value) {
        fxaaFilter.setFxaaReduceMin(value);
    }

    /**
     * Sets the parameter. The default value is 1/8.
     *
     * @param value
     */
    public void setReduceMul(float value) {
        fxaaFilter.setFxaaReduceMul(value);
    }

    @Override
    public void dispose() {
        if (fxaaFilter != null) {
            fxaaFilter.dispose();
            fxaaFilter = null;
        }
    }

    @Override
    public void rebind() {
        fxaaFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        fxaaFilter.setInput(src).setOutput(dest).render();
    }
}
