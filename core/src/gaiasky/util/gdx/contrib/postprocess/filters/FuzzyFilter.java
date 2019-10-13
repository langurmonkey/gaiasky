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

import com.badlogic.gdx.math.Vector2;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.ShaderLoader;

/**
 * Fast approximate anti-aliasing filter.
 *
 * @author Toni Sagrista
 */
public final class FuzzyFilter extends Filter<FuzzyFilter> {
    private Vector2 viewportInverse;
    private float fade;

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        ViewportInverse("u_viewportInverse", 2),
        Fade("u_fade", 0);
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

    public FuzzyFilter(Vector2 viewportSize, float fade) {
        super(ShaderLoader.fromFile("screenspace", "fuzzy"));

        this.viewportInverse = viewportSize;
        this.viewportInverse.x = 1f / this.viewportInverse.x;
        this.viewportInverse.y = 1f / this.viewportInverse.y;

        this.fade = fade;

        rebind();
    }

    public FuzzyFilter(int viewportWidth, int viewportHeight) {
        this(new Vector2(viewportWidth, viewportHeight), 3f);
    }

    public FuzzyFilter(int viewportWidth, int viewportHeight, float fade) {
        this(new Vector2(viewportWidth, viewportHeight), fade);
    }

    public void setViewportSize(float width, float height) {
        this.viewportInverse.set(1f / width, 1f / height);
        setParam(Param.ViewportInverse, this.viewportInverse);
    }

    /**
     * The fade value. The bigger this is, the faster star light
     * fades.
     *
     * @param fade
     */
    public void setFade(float fade) {
        this.fade = fade;
        setParam(Param.Fade, this.fade);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.ViewportInverse, this.viewportInverse);
        setParams(Param.Fade, this.fade);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }
}
