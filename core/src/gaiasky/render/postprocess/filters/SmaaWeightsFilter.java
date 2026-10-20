/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import gaiasky.render.util.ShaderLoader;

/**
 * SMAA blending weights pass. Searches the edge buffer for the ends of each
 * detected edge and looks up the area covered by each side of the edge in the
 * precomputed area texture. The SMAA algorithm is by Jorge Jimenez et al.; see
 * {@code assets/shader/postprocess/smaa-LICENSE.md}.
 */
public final class SmaaWeightsFilter extends Filter<SmaaWeightsFilter> {
    private final Vector2 texelSize;
    private Texture areaTexture;
    private Texture searchTexture;

    /**
     * @param viewportWidth  The viewport width in pixels.
     * @param viewportHeight The viewport height in pixels.
     * @param areaTexture    The 160x560 SMAA area lookup table.
     * @param searchTexture  The 64x16 SMAA search lookup table.
     * @param defines        Shader defines, used to select the preset.
     */
    public SmaaWeightsFilter(float viewportWidth, float viewportHeight, Texture areaTexture, Texture searchTexture, String defines) {
        super("smaa-weights", "smaa-weights", defines);
        this.texelSize = new Vector2(1f / viewportWidth, 1f / viewportHeight);
        this.areaTexture = areaTexture;
        this.searchTexture = searchTexture;
        rebind();
    }

    public void setViewportSize(float width, float height) {
        this.texelSize.set(1f / width, 1f / height);
        setParam(Param.TexelSize, this.texelSize);
    }

    public void setLookupTextures(Texture areaTexture, Texture searchTexture) {
        this.areaTexture = areaTexture;
        this.searchTexture = searchTexture;
    }

    @Override
    public void rebind() {
        setParams(Param.EdgesTexture, u_texture0);
        setParams(Param.AreaTexture, u_texture1);
        setParams(Param.SearchTexture, u_texture2);
        setParams(Param.TexelSize, texelSize);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        areaTexture.bind(u_texture1);
        searchTexture.bind(u_texture2);
    }

    public enum Param implements Parameter {
        // @formatter:off
        EdgesTexture("u_texture0", 0),
        AreaTexture("u_texture1", 0),
        SearchTexture("u_texture2", 0),
        TexelSize("u_texelSize", 2);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int arrayElementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = arrayElementSize;
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
