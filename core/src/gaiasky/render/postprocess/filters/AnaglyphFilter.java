/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import gaiasky.render.util.ShaderLoader;

public final class AnaglyphFilter extends Filter<AnaglyphFilter> {

    private Texture textureLeft, textureRight;
    /**
     * <ol start="0">
     *     <li>red/cyan</li>
     *     <li>red/cyan Dubois</li>
     *     <li>amber/blue</li>
     *     <li>amber/blue Dubois</li>
     *     <li>red/blue</li>
     * </ol>
     */
    private int anaglyphMode;

    public AnaglyphFilter() {
        super("screenspace", "anaglyph");
        rebind();
    }

    @Override
    protected void onBeforeRender() {
        //inputTexture.bind(u_texture0);
        if (textureLeft != null)
            textureLeft.bind(u_texture0);
        if (textureRight != null)
            textureRight.bind(u_texture1);
    }

    public void setTextureLeft(Texture tex) {
        textureLeft = tex;
        setParam(Param.TextureLeft, u_texture0);
    }

    public void setTextureRight(Texture tex) {
        textureRight = tex;
        setParam(Param.TextureRight, u_texture1);
    }

    public void setAnaglyphMode(int anaglyphMode) {
        this.anaglyphMode = anaglyphMode;
        setParam(Param.AnaglyphMode, anaglyphMode);
    }

    @Override
    public void rebind() {
        setParams(Param.TextureLeft, u_texture0);
        setParams(Param.TextureRight, u_texture1);
        setParams(Param.AnaglyphMode, anaglyphMode);

        endParams();
    }

    public enum Param implements Parameter {
        // @formatter:off
        AnaglyphMode("u_anaglyphMode", 0),
        TextureLeft("u_texture0", 0),
        TextureRight("u_texture1", 0);
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
