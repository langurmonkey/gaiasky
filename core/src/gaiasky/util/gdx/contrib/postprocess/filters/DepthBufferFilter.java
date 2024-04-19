/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class DepthBufferFilter extends Filter<DepthBufferFilter> {

    /**
     * Default depth buffer texture. In our case, it contains the logarithmic
     * depth buffer data.
     */
    private Texture depthTexture;
    private final Vector2 zFarK = new Vector2();

    public DepthBufferFilter() {
        super(ShaderLoader.fromFile("screenspace", "depthbuffer"));
        rebind();
    }

    public void setDepthTexture(Texture tex) {
        this.depthTexture = tex;
        setParam(Param.TextureDepth, u_texture1);
    }

    public void setZFarK(float zFar, float k) {
        this.zFarK.set(zFar, k);
        setParam(Param.ZFarK, zFarK);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.TextureDepth, u_texture1);
        setParams(Param.ZFarK, zFarK);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        depthTexture.bind(u_texture1);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        TextureDepth("u_texture1", 0),
        ZFarK("u_zFarK", 2);
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
