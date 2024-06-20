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
import gaiasky.render.postprocess.filters.SurfaceGenFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class SurfaceGen extends PostProcessorEffect {
    private final SurfaceGenFilter filter;

    public SurfaceGen() {
        filter = new SurfaceGenFilter();
        disposables.add(filter);
    }

    public void setLutTexture(Texture lut) {
        filter.setLutTexture(lut);
    }

    public void setMoistureTexture(Texture moisture) {
        filter.setMoistureTexture(moisture);
    }

    public void setLutHueShift(float lutHueShift) {
        filter.setLutHueShift(lutHueShift);
    }


    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }
}
