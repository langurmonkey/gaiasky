/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;

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
    public static final String DCamPosAlias = "dCamPos";
    public static final int DCamPos = register(DCamPosAlias);
    public static final String VrOffsetAlias = "vrOffset";
    public static final int VrOffset = register(VrOffsetAlias);
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
        double result = Math.pow(2, index);
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
