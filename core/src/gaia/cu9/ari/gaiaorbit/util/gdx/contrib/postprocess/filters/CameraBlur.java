/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2012 bmanuel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.ShaderLoader;

/**
 * FIXME this effect is INCOMPLETE!
 *
 * @author bmanuel
 */
public final class CameraBlur extends Filter<CameraBlur> {

    private Texture depthtex = null;
    private Vector2 viewport = new Vector2();

    public enum Param implements Parameter {
        // @formatter:off
        InputScene("u_texture0", 0),
        DepthMap("u_texture1", 0),
        ViewProjInv("u_viewProjInv", 0),
        PrevViewProj("u_prevViewProj", 0),
        CurrentToPrevious("u_ctp", 0),
        InverseProj("u_invProj", 0),
        Near("u_near", 0),
        Far("u_far", 0),
        K("u_k", 0),
        BlurPasses("u_blurPasses", 0),
        BlurScale("u_blurScale", 0),
        Viewport("u_viewport", 0);
        // @formatter:on

        private final String mnemonic;
        private int elementSize;

        private Param(String m, int elementSize) {
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

    public CameraBlur() {
        super(ShaderLoader.fromFile("screenspace", "camerablur"));
        rebind();
        // dolut = false;
    }

    public void setDepthTexture(Texture texture) {
        this.depthtex = texture;
    }

    public void setViewProjectionInverse(Matrix4 viewProjInv){
        setParam(Param.ViewProjInv, viewProjInv);
    }
    public void setCurrentToPrevious(Matrix4 ctp) {
        setParam(Param.CurrentToPrevious, ctp);
    }

    public void setPreviousViewProjection(Matrix4 prev){
        setParam(Param.PrevViewProj, prev);
    }

    public void setInverseProj(Matrix4 invProj){
        setParam(Param.InverseProj, invProj);
    }


    public void setBlurPasses(int passes) {
        setParam(Param.BlurPasses, passes);
    }

    public void setBlurScale(float blurScale) {
        setParam(Param.BlurScale, blurScale);
    }

    public void setNearFarK(float near, float far, float k) {
        setParam(Param.Near, near);
        setParam(Param.Far, far);
        setParam(Param.K, k);
    }

    public void setViewport(float width, float height) {
        viewport.set(width, height);
        setParam(Param.Viewport, viewport);
    }

    @Override
    public void rebind() {
        setParams(Param.InputScene, u_texture0);
        setParams(Param.DepthMap, u_texture1);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        rebind();
        inputTexture.bind(u_texture0);
        depthtex.bind(u_texture1);
    }
}
