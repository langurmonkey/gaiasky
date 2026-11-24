/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.math.Vector2;
import gaiasky.render.util.ShaderLoader;

/**
 * Builds the light mask of the frame for the light shafts effect: the lit halo around a light,
 * dropped where other geometry stands in front of it.
 *
 * @see gaiasky.render.postprocess.effects.LightShafts
 */
public final class LightShaftsMaskFilter extends Filter<LightShaftsMaskFilter> {
    private final Vector2 lightPos = new Vector2();
    private final Vector2 zFarK = new Vector2();
    private float lightDepth;
    private float lightRadius = 0.06f;

    public LightShaftsMaskFilter() {
        super(ShaderLoader.fromFile("screenspace", "lightshaftsmask"));
        rebind();
    }

    /** Sets the position of the light on screen, in [0,1]. */
    public void setLightPos(float u, float v) {
        this.lightPos.set(u, v);
        setParam(Param.LightPos, this.lightPos);
    }

    /** Sets the distance from the camera to the light, along the view direction. */
    public void setLightDepth(float depth) {
        this.lightDepth = depth;
        setParam(Param.LightDepth, depth);
    }

    /** Sets the radius of the lit halo, in uv. */
    public void setLightRadius(float radius) {
        this.lightRadius = radius;
        setParam(Param.LightRadius, radius);
    }

    /** Sets the z-far and K of the logarithmic depth buffer. */
    public void setZFarK(float zfar, float k) {
        this.zFarK.set(zfar, k);
        setParam(Param.ZFarK, this.zFarK);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.LightPos, lightPos);
        setParams(Param.LightDepth, lightDepth);
        setParams(Param.LightRadius, lightRadius);
        setParams(Param.ZFarK, zFarK);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        if (inputTexture != null) {
            inputTexture.bind(u_texture0);
        }
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        LightPos("u_lightPos", 2),
        LightDepth("u_lightDepth", 0),
        LightRadius("u_lightRadius", 0),
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
