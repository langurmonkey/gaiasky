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

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

/**
 * Screen space reflections filter.
 */
public class SSRFilter extends Filter<SSRFilter> {

    private Texture depthTexture, normalTexture, reflectionTexture;
    private final Vector2 zfark;

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Texture1("u_texture1", 0),
        Texture2("u_texture2", 0),
        Texture3("u_texture3", 0),
        ZfarK("u_zfark", 2),
        ;
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

    public SSRFilter() {
        super(ShaderLoader.fromFile("screenspace", "ssr"));
        this.zfark = new Vector2();
    }

    public void setDepthTexture(Texture tex){
        this.depthTexture = tex;
        setParam(Param.Texture1, u_texture1);
    }

    public void setNormalTexture(Texture tex){
        this.normalTexture = tex;
        setParam(Param.Texture2, u_texture2);
    }

    public void setReflectionTexture(Texture tex){
        this.reflectionTexture = tex;
        setParam(Param.Texture3, u_texture3);
    }


    public void setZfarK(float zfar, float k) {
        this.zfark.set(zfar, k);
        setParam(Param.ZfarK, this.zfark);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Texture1, u_texture1);
        setParams(Param.Texture2, u_texture2);
        setParams(Param.Texture3, u_texture3);
        setParams(Param.ZfarK, zfark);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        depthTexture.bind(u_texture1);
        normalTexture.bind(u_texture2);
        reflectionTexture.bind(u_texture3);
    }
}
