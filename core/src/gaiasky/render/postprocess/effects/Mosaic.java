/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.MosaicFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class Mosaic extends PostProcessorEffect {
    private final MosaicFilter filter;

    public Mosaic(float w, float h) {
        filter = new MosaicFilter(w, h);
        disposables.add(filter);
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
