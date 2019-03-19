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

package gaia.cu9.ari.gaiaorbit.util.postprocessing.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.postprocessing.PostProcessorEffect;
import gaia.cu9.ari.gaiaorbit.util.postprocessing.filters.GravitationalDistortionFilter;

/** 
 * This is just a test for now
 * @author Toni Sagrista
 **/
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
    public void render(FrameBuffer src, FrameBuffer dest) {
        restoreViewport(dest);
        gravFilter.setInput(src).setOutput(dest).render();
    }
}
