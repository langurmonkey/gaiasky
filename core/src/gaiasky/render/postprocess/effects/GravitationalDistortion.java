/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.GravitationalDistortionFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

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
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        gravFilter.setInput(src).setOutput(dest).render();
    }
}
