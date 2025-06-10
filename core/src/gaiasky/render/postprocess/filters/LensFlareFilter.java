/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.util.ShaderLoader;

public final class LensFlareFilter extends Filter<LensFlareFilter> {
    private final Vector2 viewport;
    private float[] lightPositions;
    private int nLights = 0;
    private final Vector3 color;
    private float intensity;

    /**
     * Creates a new lens flare filter with the given parameters.
     *
     * @param width       The viewport width.
     * @param height      The viewport height.
     * @param intensity   The intensity of the effect.
     * @param type        The type, 0 for simple, 1 for complex.
     */
    public LensFlareFilter(int width, int height, float intensity, int type, boolean useLensDirt) {
        super("screenspace", "lensflare", getDefines(type, useLensDirt));
        this.viewport = new Vector2(width, height);
        this.intensity = intensity;
        this.color = new Vector3(1.0f, 1.0f, 1.0f);

        rebind();
    }

    private static String getDefines(int type, boolean useLensDirt) {
        StringBuilder sb = new StringBuilder();
        if (type == 0) {
            sb.append("#define simpleLensFlare\n");
        } else {
            sb.append("#define complexLensFlare\n");
        }
        if(useLensDirt) {
            sb.append("#define useLensDirt\n");
        }
        return sb.toString();
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    public void setLightPositions(int nLights, float[] positions, float[] intensities) {
        this.nLights = nLights;
        this.lightPositions = positions;
        setParam(Param.NLights, this.nLights);
        setParamv(Param.LightPositions, this.lightPositions, 0, this.nLights * 2);
        setParamv(Param.LightIntensities, intensities, 0, this.nLights);
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
        setParam(Param.Intensity, this.intensity);
    }

    public void setColor(float[] color) {
        this.color.set(color);
        setParam(Param.Color, this.color);
    }

    @Override
    public void rebind() {
        // Re-implement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.Viewport, viewport);
        setParams(Param.Intensity, intensity);
        setParams(Param.Color, color);
        setParams(Param.NLights, nLights);
        if (lightPositions != null)
            setParamsv(Param.LightPositions, lightPositions, 0, nLights * 2);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        LightPositions("u_lightPositions", 2),
        LightIntensities("u_lightIntensities", 1),
        NLights("u_nLights", 0),
        Color("u_color", 3),
        Intensity("u_intensity", 0),
        Viewport("u_viewport", 2);
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
