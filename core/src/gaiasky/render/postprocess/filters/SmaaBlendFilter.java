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
 * SMAA neighbourhood blending pass. Blends each pixel with its neighbour along
 * the edge, using the weights computed by {@link SmaaWeightsFilter}. The SMAA
 * algorithm is by Jorge Jimenez et al.; see
 * {@code assets/shader/postprocess/smaa-LICENSE.md}.
 */
public final class SmaaBlendFilter extends Filter<SmaaBlendFilter> {
    private final Vector2 texelSize;
    private Texture blendTexture;

    /**
     * @param viewportWidth  The viewport width in pixels.
     * @param viewportHeight The viewport height in pixels.
     */
    public SmaaBlendFilter(float viewportWidth, float viewportHeight) {
        super("smaa-blend", "smaa-blend");
        this.texelSize = new Vector2(1f / viewportWidth, 1f / viewportHeight);
        rebind();
    }

    public void setViewportSize(float width, float height) {
        this.texelSize.set(1f / width, 1f / height);
        setParam(Param.TexelSize, this.texelSize);
    }

    /**
     * Sets the buffer with the blending weights computed in the previous pass.
     *
     * @param blendTexture The blending weights texture.
     */
    public void setBlendTexture(Texture blendTexture) {
        this.blendTexture = blendTexture;
    }

    @Override
    public void rebind() {
        setParams(Param.ColorTexture, u_texture0);
        setParams(Param.BlendTexture, u_texture1);
        setParams(Param.TexelSize, texelSize);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        blendTexture.bind(u_texture1);
    }

    public enum Param implements Parameter {
        // @formatter:off
        ColorTexture("u_texture0", 0),
        BlendTexture("u_texture1", 0),
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
