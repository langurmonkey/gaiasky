/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2012 bmanuel
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
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.RadialDistortion;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class Curvature extends PostProcessorEffect {
    private RadialDistortion distort;

    public Curvature() {
        distort = new RadialDistortion();
    }

    @Override
    public void dispose() {
        distort.dispose();
    }

    public void setDistortion(float distortion) {
        distort.setDistortion(distortion);
    }

    public void setZoom(float zoom) {
        distort.setZoom(zoom);
    }

    public float getDistortion() {
        return distort.getDistortion();
    }

    public float getZoom() {
        return distort.getZoom();
    }

    @Override
    public void rebind() {
        distort.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        distort.setInput(src).setOutput(dest).render();
    }

    ;

}
