/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.filters.FxaaFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class Fxaa extends Antialiasing {
    private final FxaaFilter fxaaFilter;

    /**
     * Create a FXAA with the viewport size
     */
    public Fxaa(float viewportWidth, float viewportHeight, int quality) {
        this((int) viewportWidth, (int) viewportHeight, quality);
    }

    public Fxaa(int viewportWidth, int viewportHeight, int quality) {
        fxaaFilter = new FxaaFilter(viewportWidth, viewportHeight, quality);
        disposables.add(fxaaFilter);
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
    public void rebind() {
        fxaaFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        fxaaFilter.setInput(src).setOutput(dest).render();
    }

    @Override
    public void updateShaders() {
        super.updateShaders();
        fxaaFilter.updateProgram();
    }

}
