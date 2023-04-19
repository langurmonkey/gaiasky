/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.utils.NumberUtils;

public class Vector4Attribute extends Attribute {
    public static final String HtermsAlias = "hterms";
    public static final int Hterms = register(HtermsAlias);
    public float[] value;

    public Vector4Attribute(int index) {
        super(index);
    }
    public Vector4Attribute(int index, float[] value) {
        super(index);
        this.value = value;
    }

    @Override
    public Attribute copy() {
        return new Vector4Attribute(index, value);
    }

    @Override
    public int hashCode() {
        double result = Math.pow(2, index);
        result = 977 * result + NumberUtils.floatToRawIntBits(value[0]) + NumberUtils.floatToRawIntBits(value[1]) + NumberUtils.floatToRawIntBits(value[2]) + NumberUtils.floatToRawIntBits(value[3]);
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
