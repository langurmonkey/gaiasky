/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*******************************************************************************
 * Copyright 2012 tsagrista
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.AnaglyphFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Anaglyph 3D red-cyan effect
 */
public final class AnaglyphEffect extends PostProcessorEffect {
    private final AnaglyphFilter anaglyphFilter;

    public AnaglyphEffect() {
        anaglyphFilter = new AnaglyphFilter();
    }

    @Override
    public void dispose() {
        anaglyphFilter.dispose();
    }

    @Override
    public void rebind() {
        anaglyphFilter.rebind();
    }

    public void setTextureLeft(Texture tex) {
        anaglyphFilter.setTextureLeft(tex);
    }

    public void setTextureRight(Texture tex) {
        anaglyphFilter.setTextureRight(tex);
    }

    /**
     * Sets the mode:
     * <ul>
     *     <li>0 - red/blue</li>
     *     <li>1 - red/cyan</li>
     * </ul>
     * @param mode The mode.
     */
    public void setAnaglyphMode(int mode) {
        anaglyphFilter.setAnaglyphMode(mode);
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        anaglyphFilter.setInput(src).setOutput(dest).render();
    }

}
