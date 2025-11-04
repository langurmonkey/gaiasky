/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.util.ShaderLoader;

public final class Combine3Filter extends Filter<Combine3Filter> {

    private float s1i, s1s, s2i, s2s, s3i, s3s;
    private Texture inputTexture2 = null;
    private Texture inputTexture3 = null;

    public Combine3Filter() {
        super(ShaderLoader.fromFile("screenspace", "combine3"));
        s1i = 1f;
        s2i = 1f;
        s3i = 1f;
        s1s = 1f;
        s2s = 1f;
        s3s = 1f;

        rebind();
    }

    public Combine3Filter setInput(FrameBuffer buffer1, FrameBuffer buffer2, FrameBuffer buffer3) {
        this.inputTexture = buffer1.getColorBufferTexture();
        this.inputTexture2 = buffer2.getColorBufferTexture();
        this.inputTexture3 = buffer3.getColorBufferTexture();
        return this;
    }

    public Combine3Filter setInput(Texture texture1, Texture texture2, Texture texture3) {
        this.inputTexture = texture1;
        this.inputTexture2 = texture2;
        this.inputTexture3 = texture3;
        return this;
    }

    public float getSource1Intensity() {
        return s1i;
    }

    public void setSource1Intensity(float intensity) {
        s1i = intensity;
        setParam(Combine3Filter.Param.Source1Intensity, intensity);
    }

    public float getSource2Intensity() {
        return s2i;
    }

    public void setSource2Intensity(float intensity) {
        s2i = intensity;
        setParam(Combine3Filter.Param.Source2Intensity, intensity);
    }

    public float getSource3Intensity() {
        return s3i;
    }

    public void setSource3Intensity(float intensity) {
        s3i = intensity;
        setParam(Combine3Filter.Param.Source3Intensity, intensity);
    }

    public float getSource1Saturation() {
        return s1s;
    }

    public void setSource1Saturation(float saturation) {
        s1s = saturation;
        setParam(Combine3Filter.Param.Source1Saturation, saturation);
    }

    public float getSource2Saturation() {
        return s2s;
    }

    public void setSource2Saturation(float saturation) {
        s2s = saturation;
        setParam(Combine3Filter.Param.Source2Saturation, saturation);
    }

    public float getSource3Saturation() {
        return s3s;
    }

    public void setSource3Saturation(float saturation) {
        s3s = saturation;
        setParam(Combine3Filter.Param.Source3Saturation, saturation);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Texture1, u_texture1);
        setParams(Param.Texture2, u_texture2);
        setParams(Param.Source1Intensity, s1i);
        setParams(Param.Source2Intensity, s2i);
        setParams(Param.Source3Intensity, s3i);
        setParams(Param.Source1Saturation, s1s);
        setParams(Param.Source2Saturation, s2s);
        setParams(Param.Source3Saturation, s3s);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        inputTexture2.bind(u_texture1);
        inputTexture3.bind(u_texture2);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Texture1("u_texture1", 0),
        Texture2("u_texture2", 0),
        Source1Intensity("u_src1Intensity", 0),
        Source1Saturation("u_src1Saturation", 0),
        Source2Intensity("u_src2Intensity", 0),
        Source2Saturation("u_src2Saturation", 0),
        Source3Intensity("u_src3Intensity", 0),
        Source3Saturation("u_src3Saturation", 0);
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
