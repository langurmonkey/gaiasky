/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.ReprojectionFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class Reprojection extends PostProcessorEffect {
    private final ReprojectionFilter reprojection;

    public Reprojection(float width, float height) {
        reprojection = new ReprojectionFilter(width, height);
        disposables.add(reprojection);
    }

    public Reprojection(int width, int height) {
        this((float) width, (float) height);
    }

    public void setViewportSize(int width, int height) {
        this.reprojection.setViewportSize(width, height);
    }

    public void setFov(float fovDegrees) {
        this.reprojection.setFov(fovDegrees);
    }

    public void setMode(int mode) {
        this.reprojection.setMode(mode);
    }

    @Override
    public void rebind() {
        reprojection.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        reprojection.setInput(src).setOutput(dest).render();
    }

}
