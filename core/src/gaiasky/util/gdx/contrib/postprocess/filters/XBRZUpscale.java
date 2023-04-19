/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public class XBRZUpscale extends Filter<XBRZUpscale> {
    private final Vector2 inputSize;
    private final Vector2 outputSize;

    public XBRZUpscale() {
        this(new Vector2(), new Vector2());
    }

    public XBRZUpscale(Vector2 inputSize, Vector2 outputSize) {
        super(ShaderLoader.fromFile("screenspace", "xbrz-freescale"));
        this.inputSize = new Vector2(inputSize);
        this.outputSize = new Vector2(outputSize);

        rebind();
    }

    public void setInputSize(float width, float height) {
        this.inputSize.set(width, height);
        setParam(Param.InputSize, this.inputSize);
    }

    public void setOutputSize(float width, float height) {
        this.outputSize.set(width, height);
        setParam(Param.OutputSize, this.outputSize);
    }

    @Override
    public void rebind() {
        // Re-implement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.InputSize, inputSize);
        setParams(Param.OutputSize, outputSize);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        InputSize("u_inputSize", 2),
        OutputSize("u_outputSize", 2);
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
