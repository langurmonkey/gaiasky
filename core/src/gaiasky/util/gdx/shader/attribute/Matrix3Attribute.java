/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.utils.NumberUtils;

public class Matrix3Attribute extends Attribute {
    public static final String Gwmat3Alias = "gwmat3";
    public static final int Gwmat3 = register(Gwmat3Alias);
    public Matrix3 value;

    public Matrix3Attribute(int index) {
        super(index);
        this.value = new Matrix3();
    }
    public Matrix3Attribute(int index, Matrix3 value) {
        super(index);
        this.value = value;
    }

    public void set(Matrix3 value) {
        this.value.set(value);
    }

    @Override
    public Attribute copy() {
        return new Matrix3Attribute(index, value);
    }

    @Override
    public int hashCode() {
        double result = Math.pow(2, index);
        result = 977 * result + NumberUtils.floatToRawIntBits(value.val[Matrix3.M00]) + NumberUtils.floatToRawIntBits(value.val[Matrix3.M11]) + NumberUtils.floatToRawIntBits(value.val[Matrix3.M22]);
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

