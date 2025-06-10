/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import gaiasky.render.util.ShaderLoader;

public class SSRFilter extends Filter3<SSRFilter> {

    private final Vector2 zFarK;
    private Matrix4 frustumCorners;
    private final Matrix4 combined;
    private final Matrix4 projection;
    private final Matrix4 invProjection;
    private final Matrix4 view;
    private final Matrix4 invView;

    private Texture texture1, texture2, texture3;

    public SSRFilter() {
        super("raymarching/screenspace", "ssr");
        this.zFarK = new Vector2();
        this.frustumCorners = new Matrix4();
        this.projection = new Matrix4();
        this.invProjection = new Matrix4();
        this.view = new Matrix4();
        this.invView = new Matrix4();
        this.combined = new Matrix4();
        rebind();
    }

    public void setNormalTexture(Texture tex) {
        setTexture2(tex);
    }

    public void setReflectionTexture(Texture tex) {
        setTexture3(tex);
    }

    public void setFrustumCorners(Matrix4 fc) {
        this.frustumCorners = fc;
        setParam(Param.FrustumCorners, this.frustumCorners);
    }

    public void setProjection(Matrix4 proj) {
        this.projection.set(proj);
        this.invProjection.set(proj).inv();
        setParam(Param.Projection, this.projection);
        setParam(Param.InvProjection, this.invProjection);
    }

    public void setCombined(Matrix4 mv) {
        this.combined.set(mv);
        setParam(Param.Combined, this.combined);
    }

    public void setView(Matrix4 view) {
        if (view != null) {
            try {
                this.view.set(view);
                setParam(Param.View, this.view);
                this.invView.set(view).inv();
                setParam(Param.InvView, this.invView);
            } catch (RuntimeException e) {
                logger.debug(e);
            }
        }
    }

    public void setZfarK(float zfar, float k) {
        this.zFarK.set(zfar, k);
        setParam(Param.ZfarK, this.zFarK);
    }

    public void setTexture1(Texture tex) {
        this.texture1 = tex;
        setParam(Param.Texture1, u_texture1);
    }

    public void setDepthTexture(Texture tex) {
        setTexture1(tex);
    }

    public void setTexture2(Texture tex) {
        this.texture2 = tex;
        setParam(Param.Texture2, u_texture2);
    }

    public void setTexture3(Texture tex) {
        this.texture3 = tex;
        setParam(Param.Texture3, u_texture3);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Texture1, u_texture1);
        setParams(Param.Texture2, u_texture2);
        setParams(Param.Texture3, u_texture3);
        setParams(Param.ZfarK, zFarK);
        setParams(Param.FrustumCorners, frustumCorners);
        setParams(Param.Combined, combined);
        setParams(Param.Projection, projection);
        setParams(Param.InvProjection, invProjection);
        setParams(Param.View, view);
        setParams(Param.InvView, invView);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        if (inputTexture != null)
            inputTexture.bind(u_texture0);
        if (texture1 != null)
            texture1.bind(u_texture1);
        if (texture2 != null)
            texture2.bind(u_texture2);
        if (texture3 != null)
            texture3.bind(u_texture3);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Texture1("u_texture1", 0),
        Texture2("u_texture2", 0),
        Texture3("u_texture3", 0),
        Viewport("u_viewport", 2),
        ZfarK("u_zfark", 2),
        Projection("u_projection", 16),
        InvProjection("u_invProjection", 16),
        View("u_view", 16),
        InvView("u_invView", 16),
        Combined("u_modelView", 16),
        FrustumCorners("u_frustumCorners", 16);
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
