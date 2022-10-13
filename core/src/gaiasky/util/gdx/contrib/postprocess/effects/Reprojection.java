/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.ReprojectionFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Fisheye effect
 */
public final class Reprojection extends PostProcessorEffect {
    private final ReprojectionFilter reprojection;

    public Reprojection(float width, float height) {
        reprojection = new ReprojectionFilter(width, height);
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
    public void dispose() {
        reprojection.dispose();
    }

    @Override
    public void rebind() {
        reprojection.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        reprojection.setInput(src).setOutput(dest).render();
    }

}
