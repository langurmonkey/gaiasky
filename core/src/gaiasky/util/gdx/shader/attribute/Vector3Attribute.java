/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;
import net.jafama.FastMath;

public class Vector3Attribute extends Attribute {
    public static final String PlanetPosAlias = "planetPos";
    public static final int PlanetPos = register(PlanetPosAlias);
    public static final String LightPosAlias = "lightPos";
    public static final int LightPos = register(LightPosAlias);
    public static final String CameraPosAlias = "cameraPos";
    public static final int CameraPos = register(CameraPosAlias);
    public static final String InvWavelengthAlias = "invWavelength";
    public static final int InvWavelength = register(InvWavelengthAlias);
    public static final String VelDirAlias = "velDir";
    public static final int VelDir = register(VelDirAlias);
    public static final String GwAlias = "gw";
    public static final int Gw = register(GwAlias);
    public static final String FogColorAlias = "fogCol";
    public static final int FogColor = register(FogColorAlias);
    public static final String VrOffsetAlias = "vrOffset";
    public static final int VrOffset = register(VrOffsetAlias);
    public static final String EclipsingBodyPosAlias = "eclipsingBodyPos";
    public static final int EclipsingBodyPos = register(EclipsingBodyPosAlias);
    public static final String VolumeBoundsMinAlias = "volumeBoundsMin";
    public static final int VolumeBoundsMin = register(VolumeBoundsMinAlias);
    public static final String VolumeBoundsMaxAlias = "volumeBoundsMax";
    public static final int VolumeBoundsMax = register(VolumeBoundsMaxAlias);
    public Vector3 value;

    public Vector3Attribute(int index) {
        super(index);
    }
    public Vector3Attribute(int index, Vector3 value) {
        super(index);
        this.value = value;
    }

    @Override
    public Attribute copy() {
        return new Vector3Attribute(index, value);
    }

    @Override
    public int hashCode() {
        double result = FastMath.pow(2, index);
        result = 977 * result + NumberUtils.floatToRawIntBits(value.x) + NumberUtils.floatToRawIntBits(value.y) + NumberUtils.floatToRawIntBits(value.z);
        return (int) result;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int compareTo(Attribute o) {
        return 0;
    }
}
