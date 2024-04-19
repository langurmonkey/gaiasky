/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class RadialDistortionFilter extends Filter<RadialDistortionFilter> {
    private float zoom, distortion;

    public RadialDistortionFilter() {
        super(ShaderLoader.fromFile("screenspace", "radial-distortion"));
        rebind();
        setDistortion(0.3f);
        setZoom(1f);
    }

    public float getDistortion() {
        return distortion;
    }

    public void setDistortion(float distortion) {
        this.distortion = distortion;
        setParam(Param.Distortion, this.distortion);
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
        setParam(Param.Zoom, this.zoom);
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Distortion, distortion);
        setParams(Param.Zoom, zoom);

        endParams();
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Distortion("distortion", 0),
        Zoom("zoom", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String m, int elementSize) {
            this.mnemonic = m;
            this.elementSize = elementSize;
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
