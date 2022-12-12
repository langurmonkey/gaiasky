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

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.GeometryWarpFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import gaiasky.util.gdx.loader.PFMData;

/**
 * Implements geometry warp and blending from MPCDI
 */
public final class GeometryWarp extends PostProcessorEffect {
    private GeometryWarpFilter warpFilter;

    public GeometryWarp(PFMData data) {
        warpFilter = new GeometryWarpFilter(data);
    }

    public GeometryWarp(PFMData data, Texture blend) {
        warpFilter = new GeometryWarpFilter(data, blend);
    }

    public void setBlendTexture(Texture tex) {
        warpFilter.setBlendTexture(tex);
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
