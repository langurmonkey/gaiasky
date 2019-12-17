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
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

/**
 * This filter implements the geometry warp and blending defined in the MPCDI format.
 * The warp texture is a reverse mapping from destination pixel to source pixel (note that
 * the original file contains the forward mapping, so the inverse must be computed beforehand).
 * The blend texture contains a blend map in the alpha channel.
 */
public final class GeometryWarpFilter extends Filter<GeometryWarpFilter> {
    private Texture warpTexture, blendTexture;
    private int warp, blend;

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Warp("u_texture1", 0),
        Blend("u_texture2", 0),
        WarpState("u_warp", 0),
        BlendState("u_blend", 0);
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


    public GeometryWarpFilter() {
        super(ShaderLoader.fromFile("screenspace", "geometrywarp"));
        this.warp = 0;
        this.blend = 0;
        rebind();
    }

    public void setWarpTexture(Texture tex){
        this.warpTexture = tex;
        setParam(Param.Warp, u_texture1);

        this.warp = tex == null ? 0 : 1;
        setParam(Param.WarpState, this.warp);
    }

    public void setBlendTexture(Texture tex){
        this.blendTexture = tex;
        setParam(Param.Blend, u_texture2);

        this.blend = tex == null ? 0 : 1;
        setParam(Param.BlendState, this.blend);
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
        setParams(Param.WarpState, warp);
        setParams(Param.Blend, u_texture2);
        setParams(Param.BlendState, blend);

        endParams();
    }
}
