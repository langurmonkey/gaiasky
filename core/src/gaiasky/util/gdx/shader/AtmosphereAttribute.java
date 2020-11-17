/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;

public class AtmosphereAttribute extends FloatAttribute {
    public AtmosphereAttribute(long type) {
        super(type);
    }

    public AtmosphereAttribute(long type, float value) {
        super(type, value);
    }

    public static final String AlphaAlias = "alpha";
    public static final long Alpha = register(AlphaAlias);

    public static final String ColorOpacityAlias = "colorOpacity";
    public static final long ColorOpacity = register(ColorOpacityAlias);

    public static final String CameraHeightAlias = "cameraHeight";
    public static final long CameraHeight = register(CameraHeightAlias);

    public static final String OuterRadiusAlias = "outerRadius";
    public static final long OuterRadius = register(OuterRadiusAlias);

    public static final String InnerRadiusAlias = "innerRadius";
    public static final long InnerRadius = register(InnerRadiusAlias);

    public static final String KrESunAlias = "krESun";
    public static final long KrESun = register(KrESunAlias);

    public static final String KmESunAlias = "kmESun";
    public static final long KmESun = register(KmESunAlias);

    public static final String Kr4PIAlias = "kr4PI";
    public static final long Kr4PI = register(Kr4PIAlias);

    public static final String Km4PIAlias = "km4PI";
    public static final long Km4PI = register(Km4PIAlias);

    public static final String ScaleAlias = "scale";
    public static final long Scale = register(ScaleAlias);

    public static final String ScaleDepthAlias = "scaleDepth";
    public static final long ScaleDepth = register(ScaleDepthAlias);

    public static final String ScaleOverScaleDepthAlias = "scaleOverScaleDepth";
    public static final long ScaleOverScaleDepth = register(ScaleOverScaleDepthAlias);

    public static final String NSamplesAlias = "nSamples";
    public static final long nSamples = register(NSamplesAlias);

    public static final String FogDensityAlias = "fogDensity";
    public static final long FogDensity = register(FogDensityAlias);

    public static final String GAlias = "g";
    public static final long G = register(GAlias);

    @Override
    public Attribute copy() {
        return new AtmosphereAttribute(type, value);
    }

}
