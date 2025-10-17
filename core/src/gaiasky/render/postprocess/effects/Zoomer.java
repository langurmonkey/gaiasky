/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.RadialBlurFilter;
import gaiasky.render.postprocess.filters.ZoomFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class Zoomer extends PostProcessorEffect {
    private final boolean doRadial;
    private final RadialBlurFilter radialBlurFilter;
    private final ZoomFilter zoomFilter;
    private final float oneOnW;
    private final float oneOnH;
    private float userOriginX, userOriginY;

    /**
     * Creating a Zoomer specifying the radial blur quality will enable radial blur
     */
    public Zoomer(int viewportWidth, int viewportHeight, RadialBlurFilter.Quality quality) {
        radialBlurFilter = quality != null ? new RadialBlurFilter(quality) : null;
        if (radialBlurFilter != null) {
            doRadial = true;
            zoomFilter = null;
            disposables.add(radialBlurFilter);
        } else {
            doRadial = false;
            zoomFilter = new ZoomFilter();
            disposables.add(zoomFilter);
        }

        oneOnW = 1f / (float) viewportWidth;
        oneOnH = 1f / (float) viewportHeight;

    }

    /**
     * Creating a Zoomer without any parameter will use plain simple zooming
     */
    public Zoomer(int viewportWidth, int viewportHeight) {
        this(viewportWidth, viewportHeight, null);
    }

    /**
     * Specify the zoom origin, in screen coordinates.
     */
    public void setOrigin(Vector2 o) {
        setOrigin(o.x, o.y);
    }

    /**
     * Specify the zoom origin, in screen coordinates.
     */
    public void setOrigin(float x, float y) {
        userOriginX = x;
        userOriginY = y;

        if (doRadial) {
            radialBlurFilter.setOrigin(x * oneOnW, 1f - y * oneOnH);
        } else {
            zoomFilter.setOrigin(x * oneOnW, 1f - y * oneOnH);
        }
    }

    public float getZoom() {
        if (doRadial) {
            return 1f / radialBlurFilter.getZoom();
        } else {
            return 1f / zoomFilter.getZoom();
        }
    }

    public void setZoom(float zoom) {
        if (doRadial) {
            radialBlurFilter.setZoom(1f / zoom);
        } else {
            this.zoomFilter.setZoom(1f / zoom);
        }
    }

    public float getBlurStrength() {
        if (doRadial) {
            return radialBlurFilter.getStrength();
        }

        return -1;
    }

    public void setBlurStrength(float strength) {
        if (doRadial) {
            radialBlurFilter.setStrength(strength);
        }
    }

    public float getOriginX() {
        return userOriginX;
    }

    public float getOriginY() {
        return userOriginY;
    }

    @Override
    public void rebind() {
        radialBlurFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        if (doRadial) {
            radialBlurFilter.setInput(src).setOutput(dest).render();
        } else {
            zoomFilter.setInput(src).setOutput(dest).render();
        }
    }
}
