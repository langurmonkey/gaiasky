/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

public class FloatAttribute extends Attribute {
    public static final String ShininessAlias = "shininess";
    public static final int Shininess = register(ShininessAlias);

    public static final String AlphaTestAlias = "alphaTest";
    public static final int AlphaTest = register(AlphaTestAlias);

    public static final String VcAlias = "vc";
    public static final int Vc = register(VcAlias);

    public static final String TsAlias = "ts";
    public static final int Ts = register(TsAlias);

    public static final String OmgwAlias = "omgw";
    public static final int Omgw = register(OmgwAlias);

    public static final String HeightScaleAlias = "heightScale";
    public static final int HeightScale = register(HeightScaleAlias);

    public static final String BodySizeAlias = "bodySize";
    public static final int BodySize = register(BodySizeAlias);

    public static final String ElevationMultiplierAlias = "elevationMultiplier";
    public static final int ElevationMultiplier = register(ElevationMultiplierAlias);

    public static final String HeightNoiseSizeAlias = "noiseSize";
    public static final int HeightNoiseSize = register(HeightNoiseSizeAlias);

    public static final String TessQualityAlias = "tessQuality";
    public static final int TessQuality = register(TessQualityAlias);

    public static final String TimeAlias = "time";
    public static final int Time = register(TimeAlias);

    public static final String SvtTileSizeAlias = "svtTileSize";
    public static final int SvtTileSize = register(SvtTileSizeAlias);

    public static final String SvtDepthAlias = "svtDepth";
    public static final int SvtDepth = register(SvtDepthAlias);

    public static final String SvtIdAlias = "svtId";
    public static final int SvtId = register(SvtIdAlias);

    public static final String SvtDetectionFactorAlias = "svtDetectionFactor";
    public static final int SvtDetectionFactor = register(SvtDetectionFactorAlias);

    public static final String EclipsingBodyRadiusAlias = "eclipsingBodyRadius";
    public static final int EclipsingBodyRadius = register(EclipsingBodyRadiusAlias);

    public static final String Generic1Alias = "generic1";
    public static final int Generic1 = register(Generic1Alias);

    public static final String Generic2Alias = "generic2";
    public static final int Generic2 = register(Generic2Alias);

    public float value;

    public FloatAttribute(int index) {
        super(index);
    }

    protected static int convertType(long oldType) {
        if (oldType == com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute.AlphaTest) {
            return AlphaTest;
        } else if (oldType == com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute.Shininess) {
            return Shininess;
        }
        return -1;
    }

    public FloatAttribute(int index, float value) {
        super(index);
        this.value = value;
    }

    public static FloatAttribute createShininess(float value) {
        return new FloatAttribute(Shininess, value);
    }

    public static FloatAttribute createAlphaTest(float value) {
        return new FloatAttribute(AlphaTest, value);
    }

    @Override
    public Attribute copy() {
        return new FloatAttribute(index, value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 977 * result + NumberUtils.floatToRawIntBits(value);
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index - o.index;
        final float v = ((FloatAttribute) o).value;
        return MathUtils.isEqual(value, v) ? 0 : value < v ? -1 : 1;
    }
}
