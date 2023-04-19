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

public final class CameraBlur extends Filter<CameraBlur> {

    private final Vector2 viewport = new Vector2();
    private Texture velocityTexture = null;

    public CameraBlur() {
        super(ShaderLoader.fromFile("screenspace", "camerablur"));
        rebind();
        // dolut = false;
    }

    public void setVelocityTexture(Texture texture) {
        this.velocityTexture = texture;
    }

    public void setBlurMaxSamples(int samples) {
        setParam(Param.BlurMaxSamples, samples);
    }

    public void setBlurScale(float blurScale) {
        setParam(Param.BlurScale, blurScale);
    }

    public void setVelocityScale(float velScale) {
        setParam(Param.VelocityScale, velScale);
    }

    public void setViewport(float width, float height) {
        viewport.set(width, height);
        setParam(Param.Viewport, viewport);
    }

    @Override
    public void rebind() {
        setParams(Param.InputScene, u_texture0);
        setParams(Param.VelocityMap, u_texture1);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        rebind();
        inputTexture.bind(u_texture0);
        velocityTexture.bind(u_texture1);
    }

    public enum Param implements Parameter {
        // @formatter:off
        InputScene("u_texture0", 0),
        VelocityMap("u_texture1", 0),
        BlurMaxSamples("u_blurSamplesMax", 0),
        BlurScale("u_blurScale", 0),
        VelocityScale("u_velScale", 0),
        Viewport("u_viewport", 0);
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
