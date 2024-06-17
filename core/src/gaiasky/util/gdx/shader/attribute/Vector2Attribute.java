/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.NumberUtils;
import net.jafama.FastMath;

public class Vector2Attribute extends Attribute {
    public static final String HeightSizeAlias = "heightSize";
    public static final int HeightSize = register(HeightSizeAlias);
    public static final String SvtResolutionAlias = "svtResolution";
    public static final int SvtResolution = register(SvtResolutionAlias);
    public Vector2 value;

    public Vector2Attribute(int index) {
        super(index);
    }
    public Vector2Attribute(int index, Vector2 value) {
        super(index);
        this.value = value;
    }

    @Override
    public Attribute copy() {
        return new Vector2Attribute(index, value);
    }

    @Override
    public int hashCode() {
        double result = FastMath.pow(2, index);
        result = 977 * result + NumberUtils.floatToRawIntBits(value.x) + NumberUtils.floatToRawIntBits(value.y);
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
