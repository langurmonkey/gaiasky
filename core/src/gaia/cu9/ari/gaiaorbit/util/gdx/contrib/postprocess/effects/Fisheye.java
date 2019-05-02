/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/**
 * Fisheye effect
 *
 * @author tsagrista
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.FisheyeDistortion;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

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
