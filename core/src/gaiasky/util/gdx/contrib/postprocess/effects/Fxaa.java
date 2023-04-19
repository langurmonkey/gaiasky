/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.filters.FxaaFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

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
