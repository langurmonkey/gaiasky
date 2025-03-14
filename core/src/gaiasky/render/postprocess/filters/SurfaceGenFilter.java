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
    private Texture lut, emissive;
    float lutHueShift = 0;
    float lutSaturation = 1;

    public SurfaceGenFilter(boolean normalMap, boolean emissiveMap) {
        super(ShaderLoader.fromFile(
                "screenspace",
                "surfacegen",
                // Generate normal map.
                (normalMap ? "#define normalMapFlag\n" : "") +
                        // The biome texture includes an emissive map in the blue channel.
                        (emissiveMap ? "#define emissiveMapFlag\n" : "")));

        rebind();
    }

    public void setLutTexture(Texture lut) {
        this.lut = lut;
        setParam(Param.TextureLut, u_texture1);
    }

    public void setEmissiveTexture(Texture emissive) {
        this.emissive = emissive;
        setParam(Param.TextureEmissive, u_texture2);
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
        if (emissive != null)
            setParams(Param.TextureEmissive, u_texture2);
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
        if (emissive != null)
            emissive.bind(u_texture2);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        TextureLut("u_texture1", 0),
        TextureEmissive("u_texture2", 0),

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
