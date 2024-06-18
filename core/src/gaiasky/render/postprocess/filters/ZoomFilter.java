/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import gaiasky.render.util.ShaderLoader;

public final class ZoomFilter extends Filter<ZoomFilter> {
    private float x, y, zoom;

    public ZoomFilter() {
        super(ShaderLoader.fromFile("zoom", "zoom"));
        rebind();
        setOrigin(0.5f, 0.5f);
        setZoom(1f);
    }

    /** Specify the zoom origin, in normalized screen coordinates. */
    public void setOrigin(float x, float y) {
        this.x = x;
        this.y = y;
        setParams(Param.OffsetX, this.x);
        setParams(Param.OffsetY, this.y);
        endParams();
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
        setParam(Param.Zoom, this.zoom);
    }

    public float getOriginX() {
        return x;
    }

    public float getOriginY() {
        return y;
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.OffsetX, x);
        setParams(Param.OffsetY, y);
        setParams(Param.Zoom, zoom);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        OffsetX("offset_x", 0),
        OffsetY("offset_y", 0),
        Zoom("zoom", 0),
        ;
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
