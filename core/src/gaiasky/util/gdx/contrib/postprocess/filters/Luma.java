/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public class Luma extends Filter<Luma> {
    private final Vector2 texelSize;
    private final Vector2 imageSize;
    private final ShaderProgram programLuma;
    private final ShaderProgram programAvg;
    private final ShaderProgram programMax;
    private float lodLevel = 0;
    public Luma() {
        super(ShaderLoader.fromFile("screenspace", "luma", "#define LUMA"));
        programLuma = program;
        programAvg = ShaderLoader.fromFile("screenspace", "luma", "#define AVERAGE");
        programMax = ShaderLoader.fromFile("screenspace", "luma", "#define MAX");

        texelSize = new Vector2();
        imageSize = new Vector2();
    }

    public void enableProgramLuma() {
        this.program = programLuma;
        rebind();
    }

    public void enableProgramAvg() {
        this.program = programAvg;
        rebind();
    }

    public void enableProgramMax() {
        this.program = programMax;
        rebind();
    }

    public void setImageSize(float w, float h) {
        imageSize.set(w, h);
        setParam(Param.ImageSize, texelSize);
    }

    public void setTexelSize(float u, float v) {
        texelSize.set(u, v);
        setParam(Param.TexelSize, texelSize);
    }

    public void setLodLevel(float lodLevel) {
        this.lodLevel = lodLevel;
        setParam(Param.LodLevel, lodLevel);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.TexelSize, texelSize);
        setParams(Param.ImageSize, texelSize);
        setParams(Param.LodLevel, lodLevel);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        ImageSize("u_imageSize", 2),
        TexelSize("u_texelSize", 2),
        LodLevel("u_lodLevel", 0);
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
