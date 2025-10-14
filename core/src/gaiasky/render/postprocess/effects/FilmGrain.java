/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.FilmGrainFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class FilmGrain extends PostProcessorEffect {
    private final FilmGrainFilter filter;

    public FilmGrain(float intensity) {
        filter = new FilmGrainFilter(intensity);
        disposables.add(filter);
    }

    public void setIntensity(float intensity) {
        filter.setIntensity(intensity);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }
}
