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
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.CubemapProjectionsFilter;

public final class CubemapProjections extends PostProcessorEffect {
    private CubemapProjectionsFilter filter;

    public enum CubemapProjection {
        EQUIRECTANGULAR, CYLINDRICAL, HAMMER
    }

    public CubemapProjections() {
        filter = new CubemapProjectionsFilter();
    }

    @Override
    public void dispose() {
        filter.dispose();
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    public void setSides(FrameBuffer xpositive, FrameBuffer xnegative, FrameBuffer ypositive, FrameBuffer ynegative, FrameBuffer zpositive, FrameBuffer znegative) {
        filter.setSides(xpositive, xnegative, ypositive, ynegative, zpositive, znegative);
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    };

    public void setProjection(CubemapProjection projection) {
        filter.setProjection(projection);
    }

}
