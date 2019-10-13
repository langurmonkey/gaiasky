/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.NumberUtils;

public class Vector2Attribute extends Attribute {
    public Vector2Attribute(long type) {
        super(type);
    }

    public Vector2Attribute(long type, Vector2 value) {
        super(type);
        this.value = value;
    }

    public Vector2 value;

    public static final String HeightSizeAlias = "heightSize";
    public static final long HeightSize = register(HeightSizeAlias);

    @Override
    public Attribute copy() {
        return new Vector2Attribute(type, value);
    }

    @Override
    public int hashCode() {
        int result = (int) type;
        result = 977 * result + NumberUtils.floatToRawIntBits(value.x) + NumberUtils.floatToRawIntBits(value.y);
        return result;
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
