/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.math.Matrix4d;

/**
 * A generic 4x4 matrix transformation.
 */
public class MatrixTransform implements ITransform {

    private final Matrix4 matFloat;
    private final Matrix4d matDouble;

    /**
     * Constructs a matrix transform.
     * @param mat The matrix values in column-major order.
     */
    public MatrixTransform(double[] mat) {
        this.matDouble = new Matrix4d(mat);
        this.matFloat = this.matDouble.putIn(new Matrix4());

    }

    /**
     * Constructs a matrix transform.
     * @param mat The matrix values in column-major order.
     */
    public MatrixTransform(float[] mat) {
        this.matDouble = new Matrix4d(mat);
        this.matFloat = this.matDouble.putIn(new Matrix4());
    }

    public MatrixTransform(Matrix4d mat) {
        this.matDouble = new Matrix4d(mat);
        this.matFloat = this.matDouble.putIn(new Matrix4());
    }

    public MatrixTransform(Matrix4 mat) {
        this.matFloat = new Matrix4(mat);
        this.matDouble = new Matrix4d(this.matFloat.val);
    }


    @Override
    public void apply(Matrix4 mat) {
        mat.mul(matFloat);
    }

    @Override
    public void apply(Matrix4d mat) {
        mat.mul(matDouble);
    }
}
