/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/**
 * Fisheye effect
 *
 * @author tsagrista
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.FisheyeDistortion;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class Fisheye extends PostProcessorEffect {
    private final FisheyeDistortion fisheye;

    public Fisheye(float width, float height) {
        fisheye = new FisheyeDistortion(width, height);
    }

    public Fisheye(int width, int height) {
        this((float) width, (float) height);
    }

    public void setViewportSize(int width, int height) {
        this.fisheye.setViewportSize(width, height);
    }

    public void setFov(float fovDegrees) {
        this.fisheye.setFov(fovDegrees);
    }

    public void setMode(int mode) {
        this.fisheye.setMode(mode);
    }

    @Override
    public void dispose() {
        fisheye.dispose();
    }

    @Override
    public void rebind() {
        fisheye.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        fisheye.setInput(src).setOutput(dest).render();
    }

}
