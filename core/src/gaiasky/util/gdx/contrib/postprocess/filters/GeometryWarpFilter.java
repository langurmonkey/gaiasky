/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/**
 * Fisheye distortion filter
 *
 * @author tsagrista
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class GeometryWarpFilter extends Filter<GeometryWarpFilter> {
    private Vector2 viewport;
    private Texture warpTexture;

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Warp("u_texture1", 0),
        Viewport("u_viewport", 2);
        // @formatter:on

        private final String mnemonic;
        private int elementSize;

        Param(String m, int elementSize) {
            this.mnemonic = m;
            this.elementSize = elementSize;
        }

        @Override
        public String mnemonic() {
            return this.mnemonic;
        }

        @Override
        public int arrayElementSize() {
            return this.elementSize;
        }
    }


    public GeometryWarpFilter(float width, float height) {
        super(ShaderLoader.fromFile("screenspace", "geometrywarp"));
        viewport = new Vector2(width, height);
        rebind();
    }

    public GeometryWarpFilter(int width, int height) {
        this((float) width, (float) height);
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }


    public void setWarpTexture(Texture tex){
        this.warpTexture = tex;
        setParam(Param.Warp, u_texture1);
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        warpTexture.bind(u_texture1);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Warp, u_texture1);
        setParams(Param.Viewport, viewport);

        endParams();
    }
}
