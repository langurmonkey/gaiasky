/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import gaiasky.util.gdx.contrib.postprocess.utils.FullscreenMesh;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;
import gaiasky.util.gdx.loader.PFMData;

/**
 * This filter implements the geometry warp and blending defined in the MPCDI format.
 */
public final class GeometryWarpFilter extends Filter<GeometryWarpFilter> {
    private final FullscreenMesh mesh;
    private Texture blendTexture;
    private int blend;

    public GeometryWarpFilter(PFMData warpData) {
        super(ShaderLoader.fromFile("screenspace", "geometrywarp"));
        this.blend = 0;
        this.mesh = new FullscreenMesh(warpData.data, warpData.width, warpData.height);
        rebind();
    }

    public GeometryWarpFilter(PFMData warpData, Texture blend) {
        super(ShaderLoader.fromFile("screenspace", "geometrywarp"));
        this.mesh = new FullscreenMesh(warpData.data, warpData.width, warpData.height);
        rebind();
        setBlendTexture(blend);
    }

    public void setBlendTexture(Texture tex) {
        this.blendTexture = tex;
        setParam(Param.Blend, u_texture1);

        this.blend = tex == null ? 0 : 1;
        setParam(Param.BlendState, this.blend);
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        if (blendTexture != null)
            blendTexture.bind(u_texture1);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Blend, u_texture1);
        setParams(Param.BlendState, blend);

        endParams();
    }

    @Override
    protected void realRender() {
        // gives a chance to filters to perform needed operations just before the rendering operation take place.
        onBeforeRender();

        program.bind();
        mesh.render(program);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Blend("u_texture1", 0),
        BlendState("u_blend", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

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
}
