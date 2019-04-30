/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.effects.CubemapProjections;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.ShaderLoader;

/** Fast approximate anti-aliasing filter.
 * @author Toni Sagrista */
public final class CubemapProjectionsFilter extends Filter<CubemapProjectionsFilter> {


    private ShaderProgram[] programs;

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0), Cubemap("u_cubemap", 0);
        // @formatter:on

        private String mnemonic;
        private int elementSize;

        private Param(String mnemonic, int arrayElementSize) {
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

    private TextureData[] cubemapSides;
    private int cmId = Integer.MIN_VALUE;

    public CubemapProjectionsFilter() {
        super(null);
        ShaderProgram equirectangular = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define equirectangular");
        ShaderProgram cylindrical = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define cylindrical");
        ShaderProgram hammeraitoff = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define hammer");

        programs = new ShaderProgram[3];
        programs[0] = equirectangular;
        programs[1] = cylindrical;
        programs[2] = hammeraitoff;

        super.program = equirectangular;
        rebind();

    }

    /**
     * Sets the projection to use
     * @param proj
     */
    public void setProjection(CubemapProjections.CubemapProjection proj) {
        switch (proj) {
        case EQUIRECTANGULAR:
            super.program = programs[0];
            rebind();
            break;
        case CYLINDRICAL:
            super.program = programs[1];
            rebind();
            break;
        case HAMMER:
            super.program = programs[2];
            rebind();
            break;
        default:
            break;
        }
    }

    public void setSides(FrameBuffer xpositive, FrameBuffer xnegative, FrameBuffer ypositive, FrameBuffer ynegative, FrameBuffer zpositive, FrameBuffer znegative) {
        cubemapSides = new TextureData[6];
        cubemapSides[0] = xpositive.getColorBufferTexture().getTextureData();
        cubemapSides[1] = xnegative.getColorBufferTexture().getTextureData();
        cubemapSides[2] = ypositive.getColorBufferTexture().getTextureData();
        cubemapSides[3] = ynegative.getColorBufferTexture().getTextureData();
        cubemapSides[4] = zpositive.getColorBufferTexture().getTextureData();
        cubemapSides[5] = znegative.getColorBufferTexture().getTextureData();

        FrameBuffer[] fbos = new FrameBuffer[6];
        fbos[0] = xpositive;
        fbos[1] = xnegative;
        fbos[2] = ypositive;
        fbos[3] = ynegative;
        fbos[4] = zpositive;
        fbos[5] = znegative;

        if (cmId < 0)
            cmId = Gdx.gl.glGenTexture();

        // Make active
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + u_texture1);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_CUBE_MAP, cmId);

        // Call glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i) for all sides
        for (int i = 0; i < cubemapSides.length; i++) {
            if (cubemapSides[i].getType() == TextureData.TextureDataType.Custom) {
                cubemapSides[i].consumeCustomData(GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i);
            }
        }

        for (int i = 0; i < cubemapSides.length; i++) {
            fbos[i].begin();
            Gdx.gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, cmId, 0);
            fbos[i].end();
        }

        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_CUBE_MAP, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_CUBE_MAP, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_CUBE_MAP, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_CUBE_MAP, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_CUBE_MAP, 0);

        setParam(Param.Cubemap, u_texture1);

    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Cubemap, u_texture1);
        setParams(Param.Texture, u_texture0);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        // Bind cubemap
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + u_texture1);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_CUBE_MAP, cmId);

        inputTexture.bind(u_texture0);
    }
}
