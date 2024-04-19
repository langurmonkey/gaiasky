/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class CameraMotionBlurFilter extends Filter<CameraMotionBlurFilter> {

    private final Vector2 viewport = new Vector2();
    private final Vector3 dCam = new Vector3();
    private final Vector2 zFarK = new Vector2();
    private final Matrix4 projViewInverse = new Matrix4();
    private final Matrix4 prevProjView = new Matrix4();
    private Texture depthTexture = null;
    private float blurScale;
    private int samples;

    public CameraMotionBlurFilter() {
        super(ShaderLoader.fromFile("screenspace", "camerablur"));
        rebind();
    }

    public void setDepthTexture(Texture texture) {
        this.depthTexture = texture;
    }

    public void setProjViewInverse(Matrix4 m) {
        this.projViewInverse.set(m);
        setParam(Param.ProjViewInverse, m);
    }

    public void setProjView(Matrix4 m) {
        this.projViewInverse.set(m).inv();
        setParam(Param.ProjViewInverse, this.projViewInverse);
    }

    public void setPrevProjView(Matrix4 m) {
        this.prevProjView.set(m);
        setParam(Param.PrevProjView, this.prevProjView);
    }

    public void setBlurMaxSamples(int samples) {
        this.samples = samples;
        setParam(Param.BlurMaxSamples, samples);
    }

    public void setBlurScale(float blurScale) {
        this.blurScale = blurScale;
        setParam(Param.BlurScale, blurScale);
    }

    public void setDCam(Vector3 d) {
        dCam.set(d);
        setParam(Param.DCam, dCam);
    }

    public void setZFarK(float zfar, float k) {
        this.zFarK.set(zfar, k);
        setParam(Param.ZFarK, this.zFarK);
    }

    public void setViewport(float width, float height) {
        viewport.set(width, height);
        setParam(Param.Viewport, viewport);
    }

    @Override
    public void rebind() {
        setParams(Param.InputScene, u_texture0);
        setParams(Param.DepthMap, u_texture2);
        setParams(Param.PrevProjView, prevProjView);
        setParams(Param.ProjViewInverse, projViewInverse);
        setParams(Param.Viewport, viewport);
        setParams(Param.DCam, dCam);
        setParams(Param.ZFarK, zFarK);
        setParams(Param.BlurScale, blurScale);
        setParams(Param.BlurMaxSamples, samples);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        rebind();
        inputTexture.bind(u_texture0);
        depthTexture.bind(u_texture2);
    }

    public enum Param implements Parameter {
        // @formatter:off
        InputScene("u_texture0", 0),
        DepthMap("u_texture1", 0),
        PrevProjView("u_prevProjView", 16),
        ProjViewInverse("u_projViewInverse", 16),
        DCam("u_dCam", 3),
        ZFarK("u_zFarK", 2),
        BlurMaxSamples("u_blurSamplesMax", 0),
        BlurScale("u_blurScale", 0),
        Viewport("u_viewport", 2);
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
