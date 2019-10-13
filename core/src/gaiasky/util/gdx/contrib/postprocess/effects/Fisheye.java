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
    private FisheyeDistortion distort;

    public Fisheye() {
        distort = new FisheyeDistortion();
    }

    @Override
    public void dispose() {
        distort.dispose();
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

    ;

}
