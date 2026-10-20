/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.math.Vector2;
import gaiasky.render.util.ShaderLoader;

/**
 * SMAA edge detection pass. Computes, for every pixel, whether there is a luma
 * edge on its left (west) and top (north) sides, and writes that to the red and
 * green channels of the edge buffer. The SMAA algorithm is by Jorge Jimenez et
 * al.; see {@code assets/shader/postprocess/smaa-LICENSE.md}.
 */
public final class SmaaEdgesFilter extends Filter<SmaaEdgesFilter> {
    private final Vector2 texelSize;

    /**
     * @param viewportWidth  The viewport width in pixels.
     * @param viewportHeight The viewport height in pixels.
     * @param defines        Shader defines, used to select the preset and the input color space.
     */
    public SmaaEdgesFilter(float viewportWidth, float viewportHeight, String defines) {
        super(ShaderLoader.fromFile("smaa-edges", "smaa-edges", defines));
        this.texelSize = new Vector2(1f / viewportWidth, 1f / viewportHeight);
        rebind();
    }

    public void setViewportSize(float width, float height) {
        this.texelSize.set(1f / width, 1f / height);
        setParam(Param.TexelSize, this.texelSize);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture, u_texture0);
        setParams(Param.TexelSize, texelSize);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
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
