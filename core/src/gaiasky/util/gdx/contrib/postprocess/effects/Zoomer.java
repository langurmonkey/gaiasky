/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.RadialBlur;
import gaiasky.util.gdx.contrib.postprocess.filters.Zoom;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public final class Zoomer extends PostProcessorEffect {
    private boolean doRadial = false;
    private RadialBlur radialBlur = null;
    private Zoom zoom = null;
    private float oneOnW, oneOnH;
    private float userOriginX, userOriginY;

    /** Creating a Zoomer specifying the radial blur quality will enable radial blur */
    public Zoomer(int viewportWidth, int viewportHeight, RadialBlur.Quality quality) {
        setup(viewportWidth, viewportHeight, new RadialBlur(quality));
    }

    /** Creating a Zoomer without any parameter will use plain simple zooming */
    public Zoomer(int viewportWidth, int viewportHeight) {
        setup(viewportWidth, viewportHeight, null);
    }

    private void setup(int viewportWidth, int viewportHeight, RadialBlur radialBlurFilter) {
        radialBlur = radialBlurFilter;
        if (radialBlur != null) {
            doRadial = true;
            zoom = null;
        } else {
            doRadial = false;
            zoom = new Zoom();
        }

        oneOnW = 1f / (float) viewportWidth;
        oneOnH = 1f / (float) viewportHeight;
    }

    /** Specify the zoom origin, in screen coordinates. */
    public void setOrigin(Vector2 o) {
        setOrigin(o.x, o.y);
    }

    /** Specify the zoom origin, in screen coordinates. */
    public void setOrigin(float x, float y) {
        userOriginX = x;
        userOriginY = y;

        if (doRadial) {
            radialBlur.setOrigin(x * oneOnW, 1f - y * oneOnH);
        } else {
            zoom.setOrigin(x * oneOnW, 1f - y * oneOnH);
        }
    }

    public float getZoom() {
        if (doRadial) {
            return 1f / radialBlur.getZoom();
        } else {
            return 1f / zoom.getZoom();
        }
    }

    public void setZoom(float zoom) {
        if (doRadial) {
            radialBlur.setZoom(1f / zoom);
        } else {
            this.zoom.setZoom(1f / zoom);
        }
    }

    public float getBlurStrength() {
        if (doRadial) {
            return radialBlur.getStrength();
        }

        return -1;
    }

    public void setBlurStrength(float strength) {
        if (doRadial) {
            radialBlur.setStrength(strength);
        }
    }

    public float getOriginX() {
        return userOriginX;
    }

    public float getOriginY() {
        return userOriginY;
    }

    @Override
    public void dispose() {
        if (radialBlur != null) {
            radialBlur.dispose();
            radialBlur = null;
        }

        if (zoom != null) {
            zoom.dispose();
            zoom = null;
        }
    }

    @Override
    public void rebind() {
        radialBlur.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        if (doRadial) {
            radialBlur.setInput(src).setOutput(dest).render();
        } else {
            zoom.setInput(src).setOutput(dest).render();
        }
    }
}
