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
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.GravitationalDistortionFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * This is just a test for now
 */
public final class GravitationalDistortion extends PostProcessorEffect {
    private GravitationalDistortionFilter gravFilter = null;

    public GravitationalDistortion(int viewportWidth, int viewportHeight) {
        setup(viewportWidth, viewportHeight);
    }

    private void setup(int viewportWidth, int viewportHeight) {
        gravFilter = new GravitationalDistortionFilter(viewportWidth, viewportHeight);
    }

    /**
     * Sets the position of the mass in pixels.
     *
     * @param x
     * @param y
     */
    public void setMassPosition(float x, float y) {
        gravFilter.setMassPosition(x, y);
    }

    @Override
    public void dispose() {
        if (gravFilter != null) {
            gravFilter.dispose();
            gravFilter = null;
        }
    }

    @Override
    public void rebind() {
        gravFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        gravFilter.setInput(src).setOutput(dest).render();
    }
}
