/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.CubemapProjectionsFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Fisheye effect
 */
public final class CubemapProjections extends PostProcessorEffect {
    private final CubemapProjectionsFilter filter;

    public enum CubemapProjection {
        EQUIRECTANGULAR,
        CYLINDRICAL,
        HAMMER,
        ORTHOGRAPHIC,
        ORTHOSPHERE,
        ORTHOSPHERE_CROSSEYE,
        AZIMUTHAL_EQUIDISTANT;

        public boolean isPlanetarium() {
            return this.equals(AZIMUTHAL_EQUIDISTANT);
        }

        public boolean isPanorama() {
            return !isPlanetarium();
        }
    }

    public CubemapProjections(float w, float h) {
        filter = new CubemapProjectionsFilter(w, h);
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

    public void setViewportSize(float w, float h) {
        filter.setViewportSize(w, h);
    }

    public void setPlanetariumAperture(float ap) {
        filter.setPlanetariumAperture(ap);
    }

    public void setPlanetariumAngle(float angle) {
        filter.setPlanetariumAngle(angle);
    }

    public float getPlanetariumAngle() {
        return filter.getPlanetariumAngle();
    }

    public float getPlanetariumAperture() {
        return filter.getPlanetariumAperture();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }

    public void setProjection(CubemapProjection projection) {
        filter.setProjection(projection);
    }

}
