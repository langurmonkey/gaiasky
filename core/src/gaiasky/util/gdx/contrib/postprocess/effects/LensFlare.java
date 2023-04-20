/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.LensFlareFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public class LensFlare extends PostProcessorEffect {
    private final LensFlareFilter filter;

    /**
     * Creates a new lens flare effect with the given parameters.
     *
     * @param width The viewport width.
     * @param height The viewport height.
     * @param intensity The intensity of the effect.
     * @param type The type, 0 for simple, 1 for complex.
     */
    public LensFlare(int width, int height, float intensity, int type) {
        filter = new LensFlareFilter(width, height, intensity, type);
    }

    public void setViewport(int width, int height) {
        filter.setViewportSize(width, height);
    }


    public void setLightPositions(int nLights, float[] vec) {
        filter.setLightPositions(nLights, vec);
    }

    public void setIntensity(float intensity) {
        filter.setIntensity(intensity);
    }

    public void setColor(float[] color) {
        filter.setColor(color);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }

    @Override
    public void dispose() {
        filter.dispose();
    }
}
