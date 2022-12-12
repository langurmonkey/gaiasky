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
import gaiasky.util.gdx.contrib.postprocess.filters.FxaaFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Implements the fast approximate anti-aliasing. Very fast and useful for combining with other post-processing effects.
 */
public final class Fxaa extends Antialiasing {
    private FxaaFilter fxaaFilter = null;

    /** Create a FXAA with the viewport size */
    public Fxaa(float viewportWidth, float viewportHeight, int quality) {
        setup(viewportWidth, viewportHeight, quality);
    }

    public Fxaa(int viewportWidth, int viewportHeight, int quality) {
        this((float) viewportWidth, (float) viewportHeight, quality);
    }

    private void setup(float viewportWidth, float viewportHeight, int quality) {
        fxaaFilter = new FxaaFilter(viewportWidth, viewportHeight, quality);
    }

    public void setViewportSize(int width, int height) {
        fxaaFilter.setViewportSize(width, height);
    }

    /**
     * Updates the FXAA quality setting.
     *
     * @param quality The quality in [0,1,2], from worst to best
     */
    public void updateQuality(int quality) {
        fxaaFilter.updateQuality(quality);
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
