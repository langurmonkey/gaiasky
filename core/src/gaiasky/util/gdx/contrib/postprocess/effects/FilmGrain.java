/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.FilmGrainFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

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
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }
}
