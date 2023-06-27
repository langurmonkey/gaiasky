/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.WarpingMeshFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;
import gaiasky.util.gdx.loader.PFMData;
import gaiasky.util.gdx.loader.WarpMeshReader.WarpMesh;

public final class WarpingMesh extends PostProcessorEffect {
    private final WarpingMeshFilter warpFilter;

    public WarpingMesh(PFMData data, float rw, float rh) {
        warpFilter = new WarpingMeshFilter(data, rw, rh);
        disposables.add(warpFilter);
    }

    public WarpingMesh(PFMData data, Texture blend) {
        warpFilter = new WarpingMeshFilter(data, blend);
        disposables.add(warpFilter);
    }

    public WarpingMesh(WarpMesh data, int rw, int rh) {
        warpFilter = new WarpingMeshFilter(data, rw, rh);
        disposables.add(warpFilter);
    }

    public void setViewportSize(int width, int height) {
        warpFilter.setViewportSize(width, height);
    }

    public void setBlendTexture(Texture tex) {
        warpFilter.setBlendTexture(tex);
    }

    @Override
    public void rebind() {
        warpFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        warpFilter.setInput(src).setOutput(dest).render();
    }
}
