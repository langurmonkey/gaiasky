/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.CopyFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public class DrawTexture extends PostProcessorEffect {
    private final CopyFilter copyFilter;

    public DrawTexture() {
        copyFilter = new CopyFilter();
        disposables.add(copyFilter);
    }

    @Override
    public void rebind() {
        copyFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        copyFilter.setInput(src).setOutput(dest).render();
    }
}
