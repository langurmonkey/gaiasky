/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import gaiasky.render.util.ShaderLoader;

public final class SurfaceGenFilter extends Filter<SurfaceGenFilter> {
    private Texture lut;
    float lutHueShift = 0;
    float lutSaturation = 1;

    public SurfaceGenFilter(boolean generateNormalMap) {
        super(ShaderLoader.fromFile(
                "screenspace",
                "surfacegen",
                generateNormalMap ? "#define normalMapFlag\n" : ""));

        rebind();
    }

    public void setLutTexture(Texture lut) {
        this.lut = lut;
        setParam(Param.TextureLut, u_texture1);
    }

    public void setLutHueShift(float hs) {
        this.lutHueShift = hs;
        setParam(Param.LutHueShift, lutHueShift);
    }

    public void setLutSaturation(float hs) {
        this.lutSaturation = hs;
        setParam(Param.LutSaturation, lutSaturation);
    }


    @Override
    public void rebind() {
        // Re-implement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.TextureLut, u_texture1);
        setParams(Param.LutSaturation, lutSaturation);
        setParams(Param.LutHueShift, lutHueShift);

        endParams();
    }

    @Override
    protected void onBeforeRender() {
        if (inputTexture != null)
            inputTexture.bind(u_texture0);
        if (lut != null)
            lut.bind(u_texture1);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        TextureLut("u_texture1", 0),

        LutSaturation("u_lutSaturation", 0),
        LutHueShift("u_lutHueShift", 0);
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
