/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.AnaglyphFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.util.Settings;

public final class AnaglyphEffect extends PostProcessorEffect {
    private final AnaglyphFilter filter;

    public AnaglyphEffect() {
        filter = new AnaglyphFilter();
        disposables.add(filter);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    public void setTextureLeft(Texture tex) {
        filter.setTextureLeft(tex);
    }

    @Override
    public void updateShaders() {
        super.updateShaders();
        filter.updateProgram();
    }

    public void setTextureRight(Texture tex) {
        filter.setTextureRight(tex);
    }

    /**
     * Sets the Anaglyph mode. See {@link Settings.StereoProfile}.
     *
     * @param mode The mode.
     */
    public void setAnaglyphMode(int mode) {
        filter.setAnaglyphMode(mode);
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }

}
