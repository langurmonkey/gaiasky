/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.util.math.Vector3D;

public class RenderingContext {
    // Reference screen size to compare to
    private static final float REFERENCE_SIZE = 1280f + 720f;

    /** The post process bean. It may have no effects enabled. **/
    public PostProcessBean ppb;

    /**
     * In case this is not null, we are using the screenshot or frame output
     * feature. This is the renderToFile frame buffer.
     **/
    public FrameBuffer fb;
    /** VR position offset **/
    public Vector3D vrOffset;
    /**
     * Scale factor, the ratio between the diagonal of HD resolution (1280x720)
     * and the current resolution
     */
    public float scaleFactor;
    /** Side of the cubemap, if any **/
    public CubemapSide cubemapSide = CubemapSide.SIDE_NONE;
    /** Render width and height **/
    private int w, h;

    /**
     * Gets the width
     *
     * @return The width in pixels
     */
    public int w() {
        return w;
    }

    /**
     * Gets the height
     *
     * @return The height in pixels
     */
    public int h() {
        return h;
    }

    /**
     * Sets the width and height
     *
     * @param w The width in pixels
     * @param h The height in pixels
     */
    public void set(int w, int h) {
        if (w != this.w || h != this.h) {
            this.scaleFactor = (float) (h + w) / REFERENCE_SIZE;
        }
        this.w = w;
        this.h = h;
    }

    public boolean isCubemap() {
        return cubemapSide != null && !cubemapSide.equals(CubemapSide.SIDE_NONE);
    }

    public enum CubemapSide {
        SIDE_UP,
        SIDE_DOWN,
        SIDE_RIGHT,
        SIDE_LEFT,
        SIDE_FRONT,
        SIDE_BACK,
        SIDE_NONE
    }
}
