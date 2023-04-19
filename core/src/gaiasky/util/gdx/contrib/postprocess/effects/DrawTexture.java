/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.Copy;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public class DrawTexture extends PostProcessorEffect {
    private final Copy copy;

    public DrawTexture() {
        copy = new Copy();
    }

    @Override
    public void dispose() {
        if (copy != null) {
            copy.dispose();
        }
    }

    @Override
    public void rebind() {
        copy.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        copy.setInput(src).setOutput(dest).render();
    }
}
