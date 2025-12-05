/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class BlendFullHalfResFilter extends Filter<BlendFullHalfResFilter> {

    private static final int u_texture4 = 4;
    private static final int u_texture5 = 5;

    private Texture half = null;
    private Texture halfAccum = null;
    private Texture halfReveal = null;
    private Texture fullDepth = null;
    private Texture halfDepth = null;
    private final Vector2 zFarK = new Vector2();

    public BlendFullHalfResFilter() {
        super("screenspace", "fullhalfresblend");

        rebind();
    }

    public BlendFullHalfResFilter setInput(GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        this.inputTexture = full.getColorBufferTexture();
        this.half = half.getColorBufferTexture();
        this.halfAccum = half.getAccumulationBufferTexture();
        this.halfReveal = half.getRevealageBufferTexture();
        this.fullDepth = full.getDepthBufferTexture();
        this.halfDepth = half.getDepthBufferTexture();
        return this;
    }

    public void setZFarK(float zFar, float k) {
        this.zFarK.set(zFar, k);
        setParam(Param.ZFarK, zFarK);
    }


    @Override
    public void rebind() {
        setParams(Param.Full, u_texture0);
        setParams(Param.Half, u_texture1);
        setParams(Param.FullDepth, u_texture2);
        setParams(Param.HalfDepth, u_texture3);
        setParams(Param.HalfAccum, u_texture4);
        setParams(Param.HalfReveal, u_texture5);
        setParams(Param.ZFarK, zFarK);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        half.bind(u_texture1);
        fullDepth.bind(u_texture2);
        halfDepth.bind(u_texture3);
        halfAccum.bind(u_texture4);
        halfReveal.bind(u_texture5);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Full("u_texture0", 0),
        Half("u_texture1", 0),
        FullDepth("u_texture2", 0),
        HalfDepth("u_texture3", 0),
        HalfAccum("u_texture4", 0),
        HalfReveal("u_texture5", 0),
        ZFarK("u_zFarK", 2);
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
