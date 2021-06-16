/*******************************************************************************
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
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

/**
 * Debug filter, renders the contents of the depth texture attachment
 */
public final class DepthBufferFilter extends Filter<DepthBufferFilter> {

    /**
     * Default depth buffer texture. In our case, it contains the logarithmic
     * depth buffer data.
     */
    private Texture depthTexture;

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        TextureDepth("u_texture1", 0);
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

    public DepthBufferFilter() {
        super(ShaderLoader.fromFile("screenspace", "depthbuffer"));
        rebind();
    }

    public void setDepthTexture(Texture tex){
        this.depthTexture = tex;
        setParam(Param.TextureDepth, u_texture1);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.TextureDepth, u_texture1);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        depthTexture.bind(u_texture1);
    }
}
