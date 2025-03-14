/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import gaiasky.render.util.ShaderLoader;

public final class CrtScreenFilter extends Filter<CrtScreenFilter> {
    private final Vector2 chromaticDispersion;
    private final Vector3 vtint;
    private final Color tint;
    private final boolean dodistortion;
    private float elapsedSecs, offset, zoom;
    private float cdRedCyan, cdBlueYellow;
    private float distortion;
    private RgbMode mode;

    public CrtScreenFilter(boolean barrelDistortion, RgbMode mode, int effectsSupport) {
        // @off
        super(ShaderLoader.fromFile("screenspace", "crt-screen",
                (barrelDistortion ? "#define ENABLE_BARREL_DISTORTION\n" : "") + (mode == RgbMode.RgbShift ? "#define ENABLE_RGB_SHIFT\n" : "") + (mode == RgbMode.ChromaticAberrations ? "#define ENABLE_CHROMATIC_ABERRATIONS\n" : "") + (isSet(Effect.TweakContrast.v, effectsSupport) ? "#define ENABLE_TWEAK_CONTRAST\n" : "") + (isSet(Effect.Vignette.v, effectsSupport) ? "#define ENABLE_VIGNETTE\n" : "") + (isSet(Effect.Tint.v, effectsSupport) ? "#define ENABLE_TINT\n" : "")
                        + (isSet(Effect.Scanlines.v, effectsSupport) ? "#define ENABLE_SCANLINES\n" : "") + (isSet(Effect.PhosphorVibrance.v, effectsSupport) ? "#define ENABLE_PHOSPHOR_VIBRANCE\n" : "") + (isSet(Effect.ScanDistortion.v, effectsSupport) ? "#define ENABLE_SCAN_DISTORTION\n" : "")));
        // @on

        dodistortion = barrelDistortion;

        vtint = new Vector3();
        tint = new Color();
        chromaticDispersion = new Vector2();

        setTime(0f);
        setTint(1.0f, 1.0f, 0.85f);
        setDistortion(0.3f);
        setZoom(1f);
        setRgbMode(mode);

        // default values
        switch (mode) {
        case ChromaticAberrations:
            setChromaticDispersion(-0.1f, -0.1f);
            break;
        case RgbShift:
            setColorOffset(0.003f);
            break;
        case None:
            break;
        default:
            throw new GdxRuntimeException("Unsupported RGB mode");
        }
    }

    private static boolean isSet(int flag, int flags) {
        return (flags & flag) == flag;
    }

    public void setTime(float elapsedSecs) {
        this.elapsedSecs = elapsedSecs;
        setParam(Param.Time, (elapsedSecs % MathUtils.PI));
    }

    public void setColorOffset(float offset) {
        this.offset = offset;
        if (mode == RgbMode.RgbShift) {
            setParam(Param.ColorOffset, this.offset);
        }
    }

    public void setChromaticDispersion(float redCyan, float blueYellow) {
        this.cdRedCyan = redCyan;
        this.cdBlueYellow = blueYellow;
        chromaticDispersion.x = cdRedCyan;
        chromaticDispersion.y = cdBlueYellow;
        if (mode == RgbMode.ChromaticAberrations) {
            setParam(Param.ChromaticDispersion, chromaticDispersion);
        }
    }

    public void setChromaticDispersionRC(float redCyan) {
        this.cdRedCyan = redCyan;
        chromaticDispersion.x = cdRedCyan;
        if (mode == RgbMode.ChromaticAberrations) {
            setParam(Param.ChromaticDispersion, chromaticDispersion);
        }
    }

    public void setChromaticDispersionBY(float blueYellow) {
        this.cdBlueYellow = blueYellow;
        chromaticDispersion.y = cdBlueYellow;
        if (mode == RgbMode.ChromaticAberrations) {
            setParam(Param.ChromaticDispersion, chromaticDispersion);
        }
    }

    public void setTint(float r, float g, float b) {
        tint.set(r, g, b, 1f);
        vtint.set(tint.r, tint.g, tint.b);
        setParam(Param.Tint, vtint);
    }

    public void setDistortion(float distortion) {
        this.distortion = distortion;
        if (dodistortion) {
            setParam(Param.Distortion, this.distortion);
        }
    }

    public RgbMode getRgbMode() {
        return mode;
    }

    public void setRgbMode(RgbMode mode) {
        this.mode = mode;
    }

    public float getOffset() {
        return offset;
    }

    public Vector2 getChromaticDispersion() {
        return chromaticDispersion;
    }

    public void setChromaticDispersion(Vector2 dispersion) {
        setChromaticDispersion(dispersion.x, dispersion.y);
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
        if (dodistortion) {
            setParam(Param.Zoom, this.zoom);
        }
    }

    public Color getTint() {
        return tint;
    }

    public void setTint(Color color) {
        tint.set(color);
        vtint.set(tint.r, tint.g, tint.b);
        setParam(Param.Tint, vtint);
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Time, elapsedSecs);

        if (mode == RgbMode.RgbShift) {
            setParams(Param.ColorOffset, offset);
        }

        if (mode == RgbMode.ChromaticAberrations) {
            setParams(Param.ChromaticDispersion, chromaticDispersion);
        }

        setParams(Param.Tint, vtint);

        if (dodistortion) {
            setParams(Param.Distortion, distortion);
            setParams(Param.Zoom, zoom);
        }

        endParams();
    }

    public enum RgbMode {
        None(0),
        RgbShift(1),
        ChromaticAberrations(2);

        public int v;

        RgbMode(int value) {
            this.v = value;
        }
    }

    public enum Effect {
        None(0),
        TweakContrast(1),
        Vignette(2),
        Tint(4),
        Scanlines(8),
        PhosphorVibrance(16),
        ScanDistortion(32);

        public int v;

        Effect(int value) {
            this.v = value;
        }
    }

    public enum Param implements Parameter {
        // @off
        Texture0("u_texture0", 0),
        Time("time", 0),
        Tint("tint", 3),
        ColorOffset("offset", 0),
        ChromaticDispersion("chromaticDispersion", 2),
        Distortion("Distortion", 0),
        Zoom("zoom", 0);
        // @on

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
