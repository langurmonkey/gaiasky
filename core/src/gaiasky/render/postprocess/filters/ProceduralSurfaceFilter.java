/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture3D;
import com.badlogic.gdx.math.Vector2;
import gaiasky.render.util.ShaderLoader;

public final class ProceduralSurfaceFilter extends NoiseFilter {
    /** LUT texture. **/
    private Texture3D lut;
    /** LUT hue shift. **/
    float lutHueShift;
    /** LUT saturation. **/
    float lutSaturation = 1;

    /** Latitude influence on the temperature. **/
    private float latitudeInfluence = 0.8f;

    private final boolean genNormalMap, genEmissiveMap;



    public ProceduralSurfaceFilter(int viewportWidth,
                                   int viewportHeight,
                                   boolean normalMap,
                                   boolean emissiveMap) {
        this(new Vector2(viewportWidth, viewportHeight), normalMap, emissiveMap);
    }

    public ProceduralSurfaceFilter(Vector2 viewportSize,
                                   boolean normalMap,
                                   boolean emissiveMap) {
        super(ShaderLoader.fromFile(
                "screenspace",
                "proceduralsurface",
                (normalMap ? "#define normalMapFlag\n" : "") +
                        (emissiveMap ? "#define emissiveMapFlag\n" : "")), viewportSize);
        this.genNormalMap = normalMap;
        this.genEmissiveMap = emissiveMap;

        rebind();
    }

    @Override
    public void updateProgram() {
        super.updateProgram(ShaderLoader.fromFile(
                "screenspace",
                "proceduralsurface",
                (genNormalMap ? "#define normalMapFlag\n" : "") +
                        (genEmissiveMap ? "#define emissiveMapFlag\n" : "")));
    }

    public void setLutTexture(Texture3D lut) {
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

    public void setLatitudeInfluence(float li) {
        this.latitudeInfluence = li;
        setParam(Param.LatitudeInfluence, latitudeInfluence);
    }

    @Override
    public void rebind() {
        super.rebindNoEnd();
        // Re-implement super to batch every parameter
        setParams(Param.LatitudeInfluence, this.latitudeInfluence);
        setParams(Param.LatitudeInfluence, this.latitudeInfluence);
        setParams(Param.TextureLut, u_texture1);
        setParams(Param.LutSaturation, lutSaturation);
        setParams(Param.LutHueShift, lutHueShift);

        endParams();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (lut != null)
            lut.bind(u_texture1);
    }

    public enum Param implements Parameter {
        // @formatter:off
        LatitudeInfluence("u_latitudeInfluence", 0),
        TextureLut("u_texture1", 0),
        LutSaturation("u_lutSaturation", 0),
        LutHueShift("u_lutHueShift", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic,
              int arrayElementSize) {
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