package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.GeometryWarpFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import gaiasky.util.gdx.loader.PFMData;
import gaiasky.util.gdx.loader.WarpMeshReader.WarpMesh;

/**
 * Implements geometry warp and blending from MPCDI (using PFM and texture data) or
 * using the warp mesh format by Paul Bourke.
 */
public final class GeometryWarp extends PostProcessorEffect {
    private GeometryWarpFilter warpFilter;

    public GeometryWarp(PFMData data) {
        warpFilter = new GeometryWarpFilter(data);
    }

    public GeometryWarp(PFMData data, Texture blend) {
        warpFilter = new GeometryWarpFilter(data, blend);
    }

    public GeometryWarp(WarpMesh data, int rw, int rh) {
        warpFilter = new GeometryWarpFilter(data, rw, rh);
    }

    public void setViewportSize(int width, int height) {
        warpFilter.setViewportSize(width, height);
    }

    public void setBlendTexture(Texture tex) {
        warpFilter.setBlendTexture(tex);
    }

    @Override
    public void dispose() {
        if (warpFilter != null) {
            warpFilter.dispose();
            warpFilter = null;
        }
    }

    @Override
    public void rebind() {
        warpFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        warpFilter.setInput(src).setOutput(dest).render();
    }
}
