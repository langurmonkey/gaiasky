/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.GravitationalDistortionFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class GravitationalDistortion extends PostProcessorEffect {
    private final GravitationalDistortionFilter gravFilter;

    public GravitationalDistortion(int viewportWidth, int viewportHeight) {
        gravFilter = new GravitationalDistortionFilter(viewportWidth, viewportHeight);
        disposables.add(gravFilter);
    }

    /**
     * Sets the position of the mass in pixels.
     *
     * @param x The mass X position.
     * @param y The mass Y position.
     */
    public void setMassPosition(float x, float y) {
        gravFilter.setMassPosition(x, y);
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
