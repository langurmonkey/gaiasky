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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.filters.AnaglyphicFilter;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Anaglyphic 3D red-cyan effect
 *
 * @author Toni Sagrista
 */
public final class Anaglyphic extends PostProcessorEffect {
    private AnaglyphicFilter anaglyphic;

    public Anaglyphic() {
        anaglyphic = new AnaglyphicFilter();
    }

    @Override
    public void dispose() {
        anaglyphic.dispose();
    }

    @Override
    public void rebind() {
        anaglyphic.rebind();
    }

    public void setTextureLeft(Texture tex) {
        anaglyphic.setTextureLeft(tex);
    }

    public void setTextureRight(Texture tex) {
        anaglyphic.setTextureRight(tex);
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        anaglyphic.setInput(src).setOutput(dest).render();
    }

    ;
}
