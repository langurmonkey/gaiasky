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

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.filters.GeometryWarpFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Implements the fast approximate anti-aliasing. Very fast and useful for combining with other post-processing effects.
 *
 * @author Toni Sagrista
 */
public final class GeometryWarp extends Antialiasing {
    private GeometryWarpFilter warpFilter = null;

    /** Create a FXAA with the viewport size */
    public GeometryWarp(float viewportWidth, float viewportHeight) {
        setup(viewportWidth, viewportHeight);
    }

    public GeometryWarp(int viewportWidth, int viewportHeight) {
        this((float) viewportWidth, (float) viewportHeight);
    }

    private void setup(float viewportWidth, float viewportHeight) {
        warpFilter = new GeometryWarpFilter(viewportWidth, viewportHeight);
    }

    public void setViewportSize(int width, int height) {
        warpFilter.setViewportSize(width, height);
    }

    public void setWarpTexture(Texture tex){
        warpFilter.setWarpTexture(tex);
    }


    @Override
    public void dispose() {
        if (warpFilter != null) {
            warpFilter.dispose();
            warpFilter = null;
        }
    }

    @Override
    public void rebind() {
        warpFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        warpFilter.setInput(src).setOutput(dest).render();
    }
}
