/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.math.Matrix4D;

/**
 * A generic 4x4 matrix transformation.
 */
public class MatrixTransform implements ITransform {

    private Matrix4 matFloat;
    private Matrix4D matDouble;

    public MatrixTransform() {
    }

    /**
     * Constructs a matrix transform.
     *
     * @param mat The matrix values in column-major order.
     */
    public MatrixTransform(double[] mat) {
        setMatrix(mat);
    }

    /**
     * Constructs a matrix transform.
     *
     * @param mat The matrix values in column-major order.
     */
    public MatrixTransform(float[] mat) {
        setMatrix(mat);
    }

    public MatrixTransform(Matrix4D mat) {
        setMatrix(mat);
    }

    public MatrixTransform(Matrix4 mat) {
        this.matFloat = new Matrix4(mat);
        this.matDouble = new Matrix4D(this.matFloat.val);
    }

    public void setMatrix(float[] mat) {
        this.matDouble = new Matrix4D(mat);
        this.matFloat = this.matDouble.putIn(new Matrix4());
    }

    public void setMatrix(Matrix4 mat) {
        this.matFloat = new Matrix4(mat);
        this.matDouble = new Matrix4D(this.matFloat.val);
    }

    public void setMatrix(double[] mat) {
        this.matDouble = new Matrix4D(mat);
        this.matFloat = this.matDouble.putIn(new Matrix4());
    }

    public void setMatrix(Matrix4D mat) {
        this.matDouble = new Matrix4D(mat);
        this.matFloat = this.matDouble.putIn(new Matrix4());
    }


    @Override
    public void apply(Matrix4 mat) {
        mat.mul(matFloat);
    }

    @Override
    public void apply(Matrix4D mat) {
        mat.mul(matDouble);
    }

    public boolean isEmpty() {
        return this.matFloat == null || this.matDouble == null;
    }

    public Matrix4D getMatDouble() {
        return matDouble;
    }

    public Matrix4 getMatFloat() {
        return matFloat;
    }

    @Override
    public ITransform copy() {
        if (this.matDouble != null) {
            return new MatrixTransform(this.matDouble.val);
        } else if (this.matFloat != null) {
            return new MatrixTransform(this.matFloat.val);
        } else {
            return new MatrixTransform();
        }
    }

}
