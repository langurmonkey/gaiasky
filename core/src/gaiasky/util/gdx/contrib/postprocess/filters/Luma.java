/*******************************************************************************
 * Copyright 2012 bmanuel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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
