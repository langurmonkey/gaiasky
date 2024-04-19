/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class LensDirtFilter extends Filter<LensDirtFilter> {
    private Texture lensDirtTexture;
    private Texture lensStarburstTexture;
    private float starburstOffset;

    public LensDirtFilter() {
        this(false);
    }

    public LensDirtFilter(boolean addToBase) {
        super(ShaderLoader.fromFile("screenspace", "lensdirt", addToBase? "#define addToBase" : ""));
        rebind();
    }

    public void setLensDirtTexture(Texture tex) {
        this.lensDirtTexture = tex;
        setParam(Param.LensDirt, u_texture1);
    }

    public void setLensStarburstTexture(Texture tex) {
        this.lensStarburstTexture = tex;
        setParam(Param.LensStarburst, u_texture2);
    }

    public void setStarburstOffset(float offset) {
        this.starburstOffset = offset;
        setParam(Param.StarburstOffset, offset);
    }

    @Override
    public void rebind() {
        // Re-implement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.LensDirt, u_texture1);
        setParams(Param.LensStarburst, u_texture2);
        setParams(Param.StarburstOffset, starburstOffset);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        lensDirtTexture.bind(u_texture1);
        if (lensStarburstTexture != null)
            lensStarburstTexture.bind(u_texture2);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        LensDirt("u_texture1", 0),
        LensStarburst("u_texture2", 0),
        StarburstOffset("u_starburstOffset", 0);
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
