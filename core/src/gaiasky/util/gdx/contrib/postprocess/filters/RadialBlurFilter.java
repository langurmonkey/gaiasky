/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class RadialBlurFilter extends Filter<RadialBlurFilter> {
    // ctrl quality
    private final int blur_len;

    // ctrl quantity
    private float strength, x, y;

    private float zoom;

    public RadialBlurFilter(Quality quality) {
        super(ShaderLoader.fromFile("radial-blur", "radial-blur", "#define BLUR_LENGTH " + quality.length + "\n#define ONE_ON_BLUR_LENGTH " + 1f / (float) quality.length));
        this.blur_len = quality.length;
        rebind();
        setOrigin(0.5f, 0.5f);
        setStrength(0.5f);
        setZoom(1f);
    }

    public RadialBlurFilter() {
        this(Quality.Low);
    }

    public void setOrigin(float x, float y) {
        this.x = x;
        this.y = y;
        setParams(Param.OffsetX, x);
        setParams(Param.OffsetY, y);
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

    public float getStrength() {
        return strength;
    }

    public void setStrength(float strength) {
        this.strength = strength;
        setParam(Param.BlurDiv, strength / (float) blur_len);
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture, u_texture0);
        setParams(Param.BlurDiv, this.strength / (float) blur_len);

        // being explicit (could call setOrigin that will call endParams)
        setParams(Param.OffsetX, x);
        setParams(Param.OffsetY, y);

        setParams(Param.Zoom, zoom);

        endParams();
    }

    public enum Quality {
        VeryHigh(16),
        High(8),
        Normal(5),
        Medium(4),
        Low(2);

        final int length;

        Quality(int value) {
            this.length = value;
        }
    }

    public enum Param implements Parameter {
        // @off
        Texture("u_texture0", 0),
        BlurDiv("blur_div", 0),
        OffsetX("offset_x", 0),
        OffsetY("offset_y", 0),
        // OneOnBlurLen( "one_on_blurlen", 0 ),
        Zoom("zoom", 0),
        ;
        // @on

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
