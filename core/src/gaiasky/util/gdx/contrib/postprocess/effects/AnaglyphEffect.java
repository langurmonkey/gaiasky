/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.AnaglyphFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class AnaglyphEffect extends PostProcessorEffect {
    private final AnaglyphFilter anaglyphFilter;

    public AnaglyphEffect() {
        anaglyphFilter = new AnaglyphFilter();
        disposables.add(anaglyphFilter);
    }

    @Override
    public void rebind() {
        anaglyphFilter.rebind();
    }

    public void setTextureLeft(Texture tex) {
        anaglyphFilter.setTextureLeft(tex);
    }

    public void setTextureRight(Texture tex) {
        anaglyphFilter.setTextureRight(tex);
    }

    /**
     * Sets the mode:
     * <ul>
     *     <li>0 - red/blue</li>
     *     <li>1 - red/cyan</li>
     * </ul>
     *
     * @param mode The mode.
     */
    public void setAnaglyphMode(int mode) {
        anaglyphFilter.setAnaglyphMode(mode);
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        anaglyphFilter.setInput(src).setOutput(dest).render();
    }

}
