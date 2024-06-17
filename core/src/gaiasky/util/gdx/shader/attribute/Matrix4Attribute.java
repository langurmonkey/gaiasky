/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.NumberUtils;
import net.jafama.FastMath;

public class Matrix4Attribute extends Attribute {
    public static final String ShadowMapProjViewTransAlias = "shadowMapProjViewTrans";
    public static final int ShadowMapProjViewTrans = register(ShadowMapProjViewTransAlias);
    public static final String EnvRotationAlias = "envRotation";
    public static final int EnvRotation = register(EnvRotationAlias);
    public Matrix4 value;

    public Matrix4Attribute(int index) {
        super(index);
        this.value = new Matrix4();
    }
    public Matrix4Attribute(int index, Matrix4 value) {
        super(index);
        this.value = new Matrix4(value);
    }

    public static Matrix4Attribute createEnvRotation(float azymuthAngleDegree) {
        return new Matrix4Attribute(EnvRotation).set(azymuthAngleDegree);
    }

    public static Matrix4Attribute createEnvRotation(Matrix4 matrix) {
        return new Matrix4Attribute(EnvRotation).set(matrix);
    }

    public Matrix4Attribute set(Matrix4 value) {
        this.value.set(value);
        return this;
    }

    public Matrix4Attribute set(float azimuthAngleDegree) {
        this.value.setToRotation(Vector3.Y, azimuthAngleDegree);
        return this;
    }

    @Override
    public Attribute copy() {
        return new Matrix4Attribute(index, value);
    }

    @Override
    public int hashCode() {
        double result = FastMath.pow(2, index);
        result = 977 * result + NumberUtils.floatToRawIntBits(value.val[Matrix4.M00]) + NumberUtils.floatToRawIntBits(value.val[Matrix4.M11]) + NumberUtils.floatToRawIntBits(value.val[Matrix4.M22]) + NumberUtils.floatToRawIntBits(value.val[Matrix4.M33]);
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
