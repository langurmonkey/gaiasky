/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.filters.NfaaFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class Nfaa extends Antialiasing {
    private final NfaaFilter nfaaFilter;

    /**
     * Create a NFAA with the viewport size
     */
    public Nfaa(float viewportWidth, float viewportHeight) {
        nfaaFilter = new NfaaFilter(viewportWidth, viewportHeight);
        disposables.add(nfaaFilter);
    }

    public void setViewportSize(int width, int height) {
        nfaaFilter.setViewportSize(width, height);
    }

    public void updateQuality(int quality) {
    }

    @Override
    public void rebind() {
        nfaaFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        nfaaFilter.setInput(src).setOutput(dest).render();
    }
}
