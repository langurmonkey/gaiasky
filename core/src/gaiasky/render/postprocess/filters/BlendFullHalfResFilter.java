/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import gaiasky.render.MainPostProcessor;
import gaiasky.render.RenderAssets;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.render.util.ShaderLoader;

public final class BlendFullHalfResFilter extends Filter<BlendFullHalfResFilter> {

    private static final int u_texture4 = 4;
    private static final int u_texture5 = 5;
    private static final int u_texture6 = 6;
    private static final int u_texture7 = 7;

    private Texture half;
    private Texture fullDepth;
    private Texture halfDepth;
    private Texture fullAccum, fullWeight, halfAccum, halfWeight;
    private final Vector2 zFarK = new Vector2();

    /**
     * Debug view of the OIT accumulation buffers; see u_debugMode in the shader. 0 = off. Static so
     * it can be flipped from the debugger or a console without touching the effect chain.
     */
    public static int debugMode = 3;

    public BlendFullHalfResFilter() {
        super(ShaderLoader.fromFile("screenspace", "fullhalfresblend", MainPostProcessor.oitEnabled ? RenderAssets.DEFINE_WBOIT : null));

        rebind();
    }

    /**
     * Sets the input buffers. When OIT is enabled the accum and weight attachments are taken from the
     * same frame buffers, so the composite sees both resolutions unresolved.
     */
    public BlendFullHalfResFilter setInput(GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        this.inputTexture = full.getColorBufferTexture();
        this.half = half.getColorBufferTexture();
        this.fullDepth = full.getDepthBufferTexture();
        this.halfDepth = half.getDepthBufferTexture();
        if (MainPostProcessor.oitEnabled) {
            this.fullAccum = full.getAccumBufferTexture();
            this.halfAccum = half.getAccumBufferTexture();
            this.fullWeight = full.getWeightBufferTexture();
            this.halfWeight = half.getWeightBufferTexture();
        }
        return this;
    }

    public void setZFarK(float zFar, float k) {
        this.zFarK.set(zFar, k);
        setParam(Param.ZFarK, zFarK);
    }

    /** Pushes the current static {@link #debugMode}; called every frame so it can be flipped live. */
    public void setDebugMode() {
        setParam(Param.DebugMode, debugMode);
    }


    @Override
    public void rebind() {
        setParams(Param.Full, u_texture0);
        setParams(Param.Half, u_texture1);
        setParams(Param.FullDepth, u_texture2);
        setParams(Param.HalfDepth, u_texture3);
        if (MainPostProcessor.oitEnabled) {
            setParams(Param.FullAccum, u_texture4);
            setParams(Param.HalfAccum, u_texture5);
            setParams(Param.FullWeight, u_texture6);
            setParams(Param.HalfWeight, u_texture7);
        }
        setParams(Param.ZFarK, zFarK);
        setParams(Param.DebugMode, debugMode);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        half.bind(u_texture1);
        fullDepth.bind(u_texture2);
        halfDepth.bind(u_texture3);
        if (MainPostProcessor.oitEnabled) {
            fullAccum.bind(u_texture4);
            halfAccum.bind(u_texture5);
            fullWeight.bind(u_texture6);
            halfWeight.bind(u_texture7);
        }
    }

    public enum Param implements Parameter {
        // @formatter:off
        Full("u_texture0", 0),
        Half("u_texture1", 0),
        FullDepth("u_texture2", 0),
        HalfDepth("u_texture3", 0),
        FullAccum("u_texture4", 0),
        HalfAccum("u_texture5", 0),
        FullWeight("u_texture6", 0),
        HalfWeight("u_texture7", 0),
        DebugMode("u_debugMode", 0),
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
