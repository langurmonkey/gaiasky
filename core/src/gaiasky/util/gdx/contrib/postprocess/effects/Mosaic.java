/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.MosaicFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class Mosaic extends PostProcessorEffect {
    private final MosaicFilter filter;

    public Mosaic(float w, float h) {
        filter = new MosaicFilter(w, h);
    }

    @Override
    public void dispose() {
        filter.dispose();
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    public void setTiles(FrameBuffer topLeft, FrameBuffer bottomLeft, FrameBuffer bl, FrameBuffer tl) {
        filter.setTiles(topLeft, bottomLeft, bl, tl);
    }

    public void setViewportSize(float w, float h) {
        filter.setViewportSize(w, h);
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }

}
