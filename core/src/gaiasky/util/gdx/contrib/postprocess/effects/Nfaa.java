/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.filters.NfaaFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class Nfaa extends Antialiasing {
    private NfaaFilter nfaaFilter = null;

    /** Create a NFAA with the viewport size */
    public Nfaa(float viewportWidth, float viewportHeight) {
        setup(viewportWidth, viewportHeight);
    }

    private void setup(float viewportWidth, float viewportHeight) {
        nfaaFilter = new NfaaFilter(viewportWidth, viewportHeight);
    }

    public void setViewportSize(int width, int height) {
        nfaaFilter.setViewportSize(width, height);
    }

    public void updateQuality(int quality) {
    }

    @Override
    public void dispose() {
        if (nfaaFilter != null) {
            nfaaFilter.dispose();
            nfaaFilter = null;
        }
    }

    @Override
    public void rebind() {
        nfaaFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        nfaaFilter.setInput(src).setOutput(dest).render();
    }
}
