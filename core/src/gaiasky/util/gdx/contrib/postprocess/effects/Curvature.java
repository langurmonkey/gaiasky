/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.RadialDistortionFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class Curvature extends PostProcessorEffect {
    private final RadialDistortionFilter distort;

    public Curvature() {
        distort = new RadialDistortionFilter();
        disposables.add(distort);
    }

    public float getDistortion() {
        return distort.getDistortion();
    }

    public void setDistortion(float distortion) {
        distort.setDistortion(distortion);
    }

    public float getZoom() {
        return distort.getZoom();
    }

    public void setZoom(float zoom) {
        distort.setZoom(zoom);
    }

    @Override
    public void rebind() {
        distort.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        distort.setInput(src).setOutput(dest).render();
    }

}
