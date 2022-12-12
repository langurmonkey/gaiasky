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
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class MosaicFilter extends Filter<MosaicFilter> {

    private final Vector2 viewport;
    private final Texture[] tiles = new Texture[4];

    public MosaicFilter(float w, float h) {
        super(null);
        this.viewport = new Vector2(w, h);

        super.program = ShaderLoader.fromFile("screenspace", "mosaic", "");
        rebind();

    }

    public void setTiles(FrameBuffer topLeft, FrameBuffer bottomLeft, FrameBuffer topRight, FrameBuffer bottomRight) {
        tiles[0] = topLeft.getColorBufferTexture();
        tiles[1] = bottomLeft.getColorBufferTexture();
        tiles[2] = topRight.getColorBufferTexture();
        tiles[3] = bottomRight.getColorBufferTexture();

        setParam(Param.Texture0, u_texture0);
        setParam(Param.Texture1, u_texture1);
        setParam(Param.Texture2, u_texture2);
        setParam(Param.Texture3, u_texture3);

    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Texture1, u_texture1);
        setParams(Param.Texture2, u_texture2);
        setParams(Param.Texture3, u_texture3);
        setParams(Param.Viewport, viewport);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        for (int i = 0; i < 4; i++) {
            tiles[i].bind(u_texture0 + i);
        }
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Texture1("u_texture1", 0),
        Texture2("u_texture2", 0),
        Texture3("u_texture3", 0),
        Viewport("u_viewport", 2);
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
