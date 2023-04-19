/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

public class AtmosphereAttribute extends FloatAttribute {
    public static final String AlphaAlias = "alpha";
    public static final int Alpha = register(AlphaAlias);
    public static final String ColorOpacityAlias = "colorOpacity";
    public static final int ColorOpacity = register(ColorOpacityAlias);
    public static final String CameraHeightAlias = "cameraHeight";
    public static final int CameraHeight = register(CameraHeightAlias);
    public static final String OuterRadiusAlias = "outerRadius";
    public static final int OuterRadius = register(OuterRadiusAlias);
    public static final String InnerRadiusAlias = "innerRadius";
    public static final int InnerRadius = register(InnerRadiusAlias);
    public static final String KrESunAlias = "krESun";
    public static final int KrESun = register(KrESunAlias);
    public static final String KmESunAlias = "kmESun";
    public static final int KmESun = register(KmESunAlias);
    public static final String Kr4PIAlias = "kr4PI";
    public static final int Kr4PI = register(Kr4PIAlias);
    public static final String Km4PIAlias = "km4PI";
    public static final int Km4PI = register(Km4PIAlias);
    public static final String ScaleAlias = "scale";
    public static final int Scale = register(ScaleAlias);
    public static final String ScaleDepthAlias = "scaleDepth";
    public static final int ScaleDepth = register(ScaleDepthAlias);
    public static final String ScaleOverScaleDepthAlias = "scaleOverScaleDepth";
    public static final int ScaleOverScaleDepth = register(ScaleOverScaleDepthAlias);
    public static final String NSamplesAlias = "nSamples";
    public static final int nSamples = register(NSamplesAlias);
    public static final String FogDensityAlias = "fogDensity";
    public static final int FogDensity = register(FogDensityAlias);
    public static final String GAlias = "g";
    public static final int G = register(GAlias);

    public AtmosphereAttribute(int index) {
        super(index);
    }
    public AtmosphereAttribute(int index, float value) {
        super(index, value);
    }

    @Override
    public Attribute copy() {
        return new AtmosphereAttribute(index, value);
    }

}
