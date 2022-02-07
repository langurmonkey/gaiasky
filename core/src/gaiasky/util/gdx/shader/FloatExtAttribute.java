/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;

public class FloatExtAttribute extends FloatAttribute {

    public FloatExtAttribute(long type) {
        super(type);
    }

    public FloatExtAttribute(long type, float value) {
        super(type, value);
    }

    public static final String VcAlias = "vc";
    public static final long Vc = register(VcAlias);

    public static final String TsAlias = "ts";
    public static final long Ts = register(TsAlias);

    //public static final String OmgwAlias = "omgw";
    //public static final long Omgw = register(OmgwAlias);

    public static final String HeightScaleAlias = "heightScale";
    public static final long HeightScale = register(HeightScaleAlias);

    public static final String HeightNoiseSizeAlias = "noiseSize";
    public static final long HeightNoiseSize = register(HeightNoiseSizeAlias);

    public static final String TessQualityAlias = "tessQuality";
    public static final long TessQuality = register(TessQualityAlias);

    public static final String TimeAlias = "time";
    public static final long Time = register(TimeAlias);

    @Override
    public Attribute copy() {
        return new FloatExtAttribute(type, value);
    }

}
