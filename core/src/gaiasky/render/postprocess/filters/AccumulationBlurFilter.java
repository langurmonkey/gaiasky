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

public class AccumulationBlurFilter extends Filter<AccumulationBlurFilter> {

    private final Vector2 resolution;
    private float blurOpacity = 0.5f;
    private float blurRadius = 0.5f;
    private Texture lastFrameTex;

    public AccumulationBlurFilter() {
        super(ShaderLoader.fromFile("screenspace", "motionblur"));
        resolution = new Vector2();
        rebind();
    }

    public void setBlurOpacity(float blurOpacity) {
        this.blurOpacity = blurOpacity;
        setParam(Param.BlurOpacity, this.blurOpacity);
    }

    public void setBlurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
        setParam(Param.BlurRadius, this.blurRadius);
    }

    public void setResolution(int w, int h) {
        resolution.set(w, h);
        setParam(Param.Resolution, this.resolution);
    }

    public void setLastFrameTexture(Texture tex) {
        this.lastFrameTex = tex;
        if (lastFrameTex != null)
            setParam(Param.LastFrame, u_texture1);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture, u_texture0);
        if (lastFrameTex != null)
            setParams(Param.LastFrame, u_texture1);
        setParams(Param.BlurOpacity, this.blurOpacity);
        setParams(Param.BlurRadius, this.blurRadius);
        setParams(Param.Resolution, this.resolution);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        if (lastFrameTex != null)
            lastFrameTex.bind(u_texture1);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        LastFrame("u_texture1", 0),
        BlurOpacity("u_blurOpacity", 0),
        BlurRadius("u_blurRadius", 0),
        Resolution("u_resolution", 2);
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
