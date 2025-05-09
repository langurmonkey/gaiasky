/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import net.jafama.FastMath;

import java.io.Serial;
import java.io.Serializable;

@SuppressWarnings("unused")
public class Matrix4D implements Serializable {
    /**
     * XX: Typically the unrotated X component for scaling, also the cosine of
     * the angle when rotated on the Y and/or Z axis. On Vector3d multiplication
     * this value is multiplied with the source X component and added to the
     * target X component.
     */
    public static final int M00 = 0;
    /**
     * XY: Typically the negative sine of the angle when rotated on the Z axis.
     * On Vector3d multiplication this value is multiplied with the source Y
     * component and added to the target X component.
     */
    public static final int M01 = 4;
    /**
     * XZ: Typically the sine of the angle when rotated on the Y axis. On
     * Vector3d multiplication this value is multiplied with the source Z
     * component and added to the target X component.
     */
    public static final int M02 = 8;
    /**
     * XW: Typically the translation of the X component. On Vector3d
     * multiplication this value is added to the target X component.
     */
    public static final int M03 = 12;
    /**
     * YX: Typically the sine of the angle when rotated on the Z axis. On
     * Vector3d multiplication this value is multiplied with the source X
     * component and added to the target Y component.
     */
    public static final int M10 = 1;
    /**
     * YY: Typically the unrotated Y component for scaling, also the cosine of
     * the angle when rotated on the X and/or Z axis. On Vector3d multiplication
     * this value is multiplied with the source Y component and added to the
     * target Y component.
     */
    public static final int M11 = 5;
    /**
     * YZ: Typically the negative sine of the angle when rotated on the X axis.
     * On Vector3d multiplication this value is multiplied with the source Z
     * component and added to the target Y component.
     */
    public static final int M12 = 9;
    /**
     * YW: Typically the translation of the Y component. On Vector3d
     * multiplication this value is added to the target Y component.
     */
    public static final int M13 = 13;
    /**
     * ZX: Typically the negative sine of the angle when rotated on the Y axis.
     * On Vector3d multiplication this value is multiplied with the source X
     * component and added to the target Z component.
     */
    public static final int M20 = 2;
    /**
     * ZY: Typical the sine of the angle when rotated on the X axis. On Vector3d
     * multiplication this value is multiplied with the source Y component and
     * added to the target Z component.
     */
    public static final int M21 = 6;
    /**
     * ZZ: Typically the unrotated Z component for scaling, also the cosine of
     * the angle when rotated on the X and/or Y axis. On Vector3d multiplication
     * this value is multiplied with the source Z component and added to the
     * target Z component.
     */
    public static final int M22 = 10;
    /**
     * ZW: Typically the translation of the Z component. On Vector3d
     * multiplication this value is added to the target Z component.
     */
    public static final int M23 = 14;
    /**
     * WX: Typically the value zero. On Vector3d multiplication this value is
     * ignored.
     */
    public static final int M30 = 3;
    /**
     * WY: Typically the value zero. On Vector3d multiplication this value is
     * ignored.
     */
    public static final int M31 = 7;
    /**
     * WZ: Typically the value zero. On Vector3d multiplication this value is
     * ignored.
     */
    public static final int M32 = 11;
    /**
     * WW: Typically the value one. On Vector3d multiplication this value is
     * ignored.
     */
    public static final int M33 = 15;
    public final static double[] stmp = new double[16];
    static final Vector3D l_vez = new Vector3D();
    static final Vector3D l_vex = new Vector3D();
    static final Vector3D l_vey = new Vector3D();
    static final Vector3Q l_vezb = new Vector3Q();
    static final Vector3Q l_vexb = new Vector3Q();
    static final Vector3Q l_veyb = new Vector3Q();
    static final Vector3D tmpVec = new Vector3D();
    static final Vector3Q tmpVecb = new Vector3Q();
    static final Matrix4D tmpMat = new Matrix4D();
    static final Vector3D right = new Vector3D();
    static final Vector3D tmpForward = new Vector3D();
    static final Vector3D tmpUp = new Vector3D();
    @Serial private static final long serialVersionUID = -2717655254359579617L;
    static QuaternionDouble quat = new QuaternionDouble();
    public final double[] tmp = new double[16];
    public final double[] val = new double[16];

    /** Constructs an identity matrix */
    public Matrix4D() {
        val[M00] = 1f;
        val[M11] = 1f;
        val[M22] = 1f;
        val[M33] = 1f;
    }

    /**
     * Constructs a matrix from the given matrix.
     *
     * @param matrix The matrix to copy. (This matrix is not modified)
     */
    public Matrix4D(Matrix4D matrix) {
        this.set(matrix);
    }

    /**
     * Constructs a matrix from the given double array. The array must have at
     * least 16 elements; the first 16 will be copied.
     *
     * @param values The double array to copy. Remember that this matrix is in
     *               <a href=
     *               "http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column
     *               major</a> order. (The double array is not modified.)
     */
    public Matrix4D(double[] values) {
        this.set(values);
    }

    public Matrix4D(float[] values) {
        this.set(values);
    }

    /**
     * Constructs a rotation matrix from the given {@link QuaternionDouble}.
     *
     * @param quaternion The quaternion to be copied. (The quaternion is not modified)
     */
    public Matrix4D(QuaternionDouble quaternion) {
        this.set(quaternion);
    }

    /**
     * Construct a matrix from the given translation, rotation and scale.
     *
     * @param position The translation
     * @param rotation The rotation, must be normalized
     * @param scale    The scale
     */
    public Matrix4D(Vector3D position, QuaternionDouble rotation, Vector3D scale) {
        set(position, rotation, scale);
    }

    /**
     * Computes the inverse of the given matrix. The matrix array is assumed to
     * hold a 4x4 column major matrix as you can get from {@link Matrix4#val}.
     *
     * @param values the matrix values.
     *
     * @return false in case the inverse could not be calculated, true
     * otherwise.
     */
    public static boolean inv(double[] values) {
        return matrix4_inv(values);
    }

    /**
     * Multiplies the vector with the given matrix, performing a division by w.
     * The matrix array is assumed to hold a 4x4 column major matrix as you can
     * get from {@link Matrix4D#val}. The vector array is assumed to hold a
     * 3-component vector, with x being the first element, y being the second
     * and z being the last component. The result is stored in the vector array.
     * This is the same as {@link Vector3D#prj(Matrix4D)}.
     *
     * @param mat the matrix
     * @param vec the vector.
     */
    public static void prj(double[] mat, double[] vec) {
        matrix4_proj(mat, vec, 0);
    }

    /**
     * Multiplies the vectors with the given matrix, , performing a division by
     * w. The matrix array is assumed to hold a 4x4 column major matrix as you
     * can get from {@link Matrix4#val}. The vectors array is assumed to hold
     * 3-component vectors. Offset specifies the offset into the array where the
     * x-component of the first vector is located. The numVecs parameter
     * specifies the number of vectors stored in the vectors array. The stride
     * parameter specifies the number of floats between subsequent vectors and
     * must be >= 3. This is the same as {@link Vector3#prj(Matrix4)} applied to
     * multiple vectors.
     *
     * @param mat     the matrix
     * @param vecs    the vectors
     * @param offset  the offset into the vectors array
     * @param numVecs the number of vectors
     * @param stride  the stride between vectors in floats
     */
    public static void prj(double[] mat, double[] vecs, int offset, int numVecs, int stride) {
        for (int i = 0; i < numVecs; i++) {
            matrix4_proj(mat, vecs, offset);
            offset += stride;
        }
    }

    static void matrix4_mul(double[] mata, double[] matb) {
        stmp[M00] = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20] + mata[M03] * matb[M30];
        stmp[M01] = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21] + mata[M03] * matb[M31];
        stmp[M02] = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22] + mata[M03] * matb[M32];
        stmp[M03] = mata[M00] * matb[M03] + mata[M01] * matb[M13] + mata[M02] * matb[M23] + mata[M03] * matb[M33];
        stmp[M10] = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20] + mata[M13] * matb[M30];
        stmp[M11] = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21] + mata[M13] * matb[M31];
        stmp[M12] = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22] + mata[M13] * matb[M32];
        stmp[M13] = mata[M10] * matb[M03] + mata[M11] * matb[M13] + mata[M12] * matb[M23] + mata[M13] * matb[M33];
        stmp[M20] = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20] + mata[M23] * matb[M30];
        stmp[M21] = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21] + mata[M23] * matb[M31];
        stmp[M22] = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22] + mata[M23] * matb[M32];
        stmp[M23] = mata[M20] * matb[M03] + mata[M21] * matb[M13] + mata[M22] * matb[M23] + mata[M23] * matb[M33];
        stmp[M30] = mata[M30] * matb[M00] + mata[M31] * matb[M10] + mata[M32] * matb[M20] + mata[M33] * matb[M30];
        stmp[M31] = mata[M30] * matb[M01] + mata[M31] * matb[M11] + mata[M32] * matb[M21] + mata[M33] * matb[M31];
        stmp[M32] = mata[M30] * matb[M02] + mata[M31] * matb[M12] + mata[M32] * matb[M22] + mata[M33] * matb[M32];
        stmp[M33] = mata[M30] * matb[M03] + mata[M31] * matb[M13] + mata[M32] * matb[M23] + mata[M33] * matb[M33];
        System.arraycopy(stmp, 0, mata, 0, 16);
    }

    static double matrix4_det(double[] val) {
        return val[M30] * val[M21] * val[M12] * val[M03] - val[M20] * val[M31] * val[M12] * val[M03] - val[M30] * val[M11] * val[M22] * val[M03] + val[M10] * val[M31] * val[M22] * val[M03] + val[M20] * val[M11] * val[M32] * val[M03] - val[M10] * val[M21] * val[M32] * val[M03] - val[M30] * val[M21] * val[M02] * val[M13] + val[M20] * val[M31] * val[M02] * val[M13] + val[M30] * val[M01] * val[M22] * val[M13] - val[M00] * val[M31] * val[M22] * val[M13] - val[M20] * val[M01] * val[M32] * val[M13]
                + val[M00] * val[M21] * val[M32] * val[M13] + val[M30] * val[M11] * val[M02] * val[M23] - val[M10] * val[M31] * val[M02] * val[M23] - val[M30] * val[M01] * val[M12] * val[M23] + val[M00] * val[M31] * val[M12] * val[M23] + val[M10] * val[M01] * val[M32] * val[M23] - val[M00] * val[M11] * val[M32] * val[M23] - val[M20] * val[M11] * val[M02] * val[M33] + val[M10] * val[M21] * val[M02] * val[M33] + val[M20] * val[M01] * val[M12] * val[M33] - val[M00] * val[M21] * val[M12] * val[M33]
                - val[M10] * val[M01] * val[M22] * val[M33] + val[M00] * val[M11] * val[M22] * val[M33];
    }

    static boolean matrix4_inv(double[] val) {
        double[] tmp = new double[16];
        double l_det = matrix4_det(val);
        if (l_det == 0)
            return false;
        stmp[M00] = val[M12] * val[M23] * val[M31] - val[M13] * val[M22] * val[M31] + val[M13] * val[M21] * val[M32] - val[M11] * val[M23] * val[M32] - val[M12] * val[M21] * val[M33] + val[M11] * val[M22] * val[M33];
        stmp[M01] = val[M03] * val[M22] * val[M31] - val[M02] * val[M23] * val[M31] - val[M03] * val[M21] * val[M32] + val[M01] * val[M23] * val[M32] + val[M02] * val[M21] * val[M33] - val[M01] * val[M22] * val[M33];
        stmp[M02] = val[M02] * val[M13] * val[M31] - val[M03] * val[M12] * val[M31] + val[M03] * val[M11] * val[M32] - val[M01] * val[M13] * val[M32] - val[M02] * val[M11] * val[M33] + val[M01] * val[M12] * val[M33];
        stmp[M03] = val[M03] * val[M12] * val[M21] - val[M02] * val[M13] * val[M21] - val[M03] * val[M11] * val[M22] + val[M01] * val[M13] * val[M22] + val[M02] * val[M11] * val[M23] - val[M01] * val[M12] * val[M23];
        stmp[M10] = val[M13] * val[M22] * val[M30] - val[M12] * val[M23] * val[M30] - val[M13] * val[M20] * val[M32] + val[M10] * val[M23] * val[M32] + val[M12] * val[M20] * val[M33] - val[M10] * val[M22] * val[M33];
        stmp[M11] = val[M02] * val[M23] * val[M30] - val[M03] * val[M22] * val[M30] + val[M03] * val[M20] * val[M32] - val[M00] * val[M23] * val[M32] - val[M02] * val[M20] * val[M33] + val[M00] * val[M22] * val[M33];
        stmp[M12] = val[M03] * val[M12] * val[M30] - val[M02] * val[M13] * val[M30] - val[M03] * val[M10] * val[M32] + val[M00] * val[M13] * val[M32] + val[M02] * val[M10] * val[M33] - val[M00] * val[M12] * val[M33];
        stmp[M13] = val[M02] * val[M13] * val[M20] - val[M03] * val[M12] * val[M20] + val[M03] * val[M10] * val[M22] - val[M00] * val[M13] * val[M22] - val[M02] * val[M10] * val[M23] + val[M00] * val[M12] * val[M23];
        stmp[M20] = val[M11] * val[M23] * val[M30] - val[M13] * val[M21] * val[M30] + val[M13] * val[M20] * val[M31] - val[M10] * val[M23] * val[M31] - val[M11] * val[M20] * val[M33] + val[M10] * val[M21] * val[M33];
        stmp[M21] = val[M03] * val[M21] * val[M30] - val[M01] * val[M23] * val[M30] - val[M03] * val[M20] * val[M31] + val[M00] * val[M23] * val[M31] + val[M01] * val[M20] * val[M33] - val[M00] * val[M21] * val[M33];
        stmp[M22] = val[M01] * val[M13] * val[M30] - val[M03] * val[M11] * val[M30] + val[M03] * val[M10] * val[M31] - val[M00] * val[M13] * val[M31] - val[M01] * val[M10] * val[M33] + val[M00] * val[M11] * val[M33];
        stmp[M23] = val[M03] * val[M11] * val[M20] - val[M01] * val[M13] * val[M20] - val[M03] * val[M10] * val[M21] + val[M00] * val[M13] * val[M21] + val[M01] * val[M10] * val[M23] - val[M00] * val[M11] * val[M23];
        stmp[M30] = val[M12] * val[M21] * val[M30] - val[M11] * val[M22] * val[M30] - val[M12] * val[M20] * val[M31] + val[M10] * val[M22] * val[M31] + val[M11] * val[M20] * val[M32] - val[M10] * val[M21] * val[M32];
        stmp[M31] = val[M01] * val[M22] * val[M30] - val[M02] * val[M21] * val[M30] + val[M02] * val[M20] * val[M31] - val[M00] * val[M22] * val[M31] - val[M01] * val[M20] * val[M32] + val[M00] * val[M21] * val[M32];
        stmp[M32] = val[M02] * val[M11] * val[M30] - val[M01] * val[M12] * val[M30] - val[M02] * val[M10] * val[M31] + val[M00] * val[M12] * val[M31] + val[M01] * val[M10] * val[M32] - val[M00] * val[M11] * val[M32];
        stmp[M33] = val[M01] * val[M12] * val[M20] - val[M02] * val[M11] * val[M20] + val[M02] * val[M10] * val[M21] - val[M00] * val[M12] * val[M21] - val[M01] * val[M10] * val[M22] + val[M00] * val[M11] * val[M22];

        double inv_det = 1.0 / l_det;
        val[M00] = stmp[M00] * inv_det;
        val[M01] = stmp[M01] * inv_det;
        val[M02] = stmp[M02] * inv_det;
        val[M03] = stmp[M03] * inv_det;
        val[M10] = stmp[M10] * inv_det;
        val[M11] = stmp[M11] * inv_det;
        val[M12] = stmp[M12] * inv_det;
        val[M13] = stmp[M13] * inv_det;
        val[M20] = stmp[M20] * inv_det;
        val[M21] = stmp[M21] * inv_det;
        val[M22] = stmp[M22] * inv_det;
        val[M23] = stmp[M23] * inv_det;
        val[M30] = stmp[M30] * inv_det;
        val[M31] = stmp[M31] * inv_det;
        val[M32] = stmp[M32] * inv_det;
        val[M33] = stmp[M33] * inv_det;
        return true;
    }

    static void matrix4_mulVec(double[] mat, double[] vec, int offset) {
        double x = vec[offset] * mat[M00] + vec[offset + 1] * mat[M01] + vec[offset + 2] * mat[M02] + mat[M03];
        double y = vec[offset] * mat[M10] + vec[offset + 1] * mat[M11] + vec[offset + 2] * mat[M12] + mat[M13];
        double z = vec[offset] * mat[M20] + vec[offset + 1] * mat[M21] + vec[offset + 2] * mat[M22] + mat[M23];
        vec[offset] = x;
        vec[offset + 1] = y;
        vec[offset + 2] = z;
    }

    static void matrix4_proj(double[] mat, double[] vec, int offset) {
        double inv_w = 1.0 / (vec[offset] * mat[M30] + vec[offset + 1] * mat[M31] + vec[offset + 2] * mat[M32] + mat[M33]);
        double x = (vec[offset] * mat[M00] + vec[offset + 1] * mat[M01] + vec[offset + 2] * mat[M02] + mat[M03]) * inv_w;
        double y = (vec[offset] * mat[M10] + vec[offset + 1] * mat[M11] + vec[offset + 2] * mat[M12] + mat[M13]) * inv_w;
        double z = (vec[offset] * mat[M20] + vec[offset + 1] * mat[M21] + vec[offset + 2] * mat[M22] + mat[M23]) * inv_w;
        vec[offset] = x;
        vec[offset + 1] = y;
        vec[offset + 2] = z;
    }

    static void matrix4_rot(double[] mat, double[] vec, int offset) {
        double x = vec[offset] * mat[M00] + vec[offset + 1] * mat[M01] + vec[offset + 2] * mat[M02];
        double y = vec[offset] * mat[M10] + vec[offset + 1] * mat[M11] + vec[offset + 2] * mat[M12];
        double z = vec[offset] * mat[M20] + vec[offset + 1] * mat[M21] + vec[offset + 2] * mat[M22];
        vec[offset] = x;
        vec[offset + 1] = y;
        vec[offset + 2] = z;
    }

    /**
     * Multiplies the matrix mata with matrix matb, storing the result in mata.
     * The arrays are assumed to hold 4x4 column major matrices as you can get
     * from {@link Matrix4D#val}. This is the same as
     * {@link Matrix4D#mul(Matrix4D)}.
     *
     * @param mata the first matrix.
     * @param matb the second matrix.
     */
    public static void mul(double[] mata, double[] matb) {
        matrix4_mul(mata, matb);
    }

    /**
     * Constructs a change of basis from the canonical basis to the base defined by the
     * given x, y and z vectors.
     *
     * @param x The x vector of the new basis expressed in the canonical basis.
     * @param y The y vector of the new basis expressed in the canonical basis.
     * @param z The z vector of the new basis expressed in the canonical basis.
     *
     * @return The change of basis matrix.
     */
    public static Matrix4D changeOfBasis(Vector3D x, Vector3D y, Vector3D z) {
        double[] vals = new double[] {
                x.x, y.x, z.x, 0,
                x.y, y.y, z.y, 0,
                x.z, y.z, z.z, 0,
                0, 0, 0, 1
        };
        Matrix4D c = new Matrix4D(vals);
        return c.tra();
    }

    /**
     * Constructs a change of basis from the canonical basis to the base defined by the
     * given x, y and z vectors.
     *
     * @param x The x vector of the new basis expressed in the canonical basis.
     * @param y The y vector of the new basis expressed in the canonical basis.
     * @param z The z vector of the new basis expressed in the canonical basis.
     *
     * @return The change of basis matrix.
     */
    public static Matrix4D changeOfBasis(double[] x, double[] y, double[] z) {
        double[] vals = new double[] {
                x[0], y[0], z[0], 0,
                x[1], y[1], z[1], 0,
                x[2], y[2], z[2], 0,
                0, 0, 0, 1
        };
        Matrix4D c = new Matrix4D(vals);
        return c.tra();
    }

    /**
     * Sets the matrix to the given matrix.
     *
     * @param matrix The matrix that is to be copied. (The given matrix is not
     *               modified)
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D set(Matrix4D matrix) {
        return this.set(matrix.val);
    }

    public Matrix4D set(Matrix4 matrix) {
        return this.set(matrix.val);
    }

    /**
     * Sets the matrix to the given matrix as a double array. The double array
     * must have at least 16 elements; the first 16 will be copied.
     *
     * @param values The matrix, in double form, that is to be copied. Remember
     *               that this matrix is in <a href=
     *               "http://en.wikipedia.org/wiki/Row-major_order#Column-major_order">column
     *               major</a> order.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D set(double[] values) {
        System.arraycopy(values, 0, val, 0, val.length);
        return this;
    }

    public Matrix4D set(float[] values) {
        for (int i = 0; i < val.length; i++) {
            val[i] = values[i];
        }
        return this;
    }

    /**
     * Sets the matrix to a rotation matrix representing the quaternion.
     *
     * @param quaternion The quaternion that is to be used to set this matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D set(QuaternionDouble quaternion) {
        return set(quaternion.x, quaternion.y, quaternion.z, quaternion.w);
    }

    /**
     * Sets the matrix to a rotation matrix representing the quaternion.
     *
     * @param quaternionX The X component of the quaternion that is to be used to set
     *                    this matrix.
     * @param quaternionY The Y component of the quaternion that is to be used to set
     *                    this matrix.
     * @param quaternionZ The Z component of the quaternion that is to be used to set
     *                    this matrix.
     * @param quaternionW The W component of the quaternion that is to be used to set
     *                    this matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D set(double quaternionX, double quaternionY, double quaternionZ, double quaternionW) {
        return set(0f, 0f, 0f, quaternionX, quaternionY, quaternionZ, quaternionW);
    }

    /**
     * Set this matrix to the specified translation and rotation.
     *
     * @param position    The translation
     * @param orientation The rotation, must be normalized
     *
     * @return This matrix for chaining
     */
    public Matrix4D set(Vector3D position, QuaternionDouble orientation) {
        return set(position.x, position.y, position.z, orientation.x, orientation.y, orientation.z, orientation.w);
    }

    /**
     * Sets the matrix to a rotation matrix representing the translation and
     * quaternion.
     *
     * @param translationX The X component of the translation that is to be used to set
     *                     this matrix.
     * @param translationY The Y component of the translation that is to be used to set
     *                     this matrix.
     * @param translationZ The Z component of the translation that is to be used to set
     *                     this matrix.
     * @param quaternionX  The X component of the quaternion that is to be used to set
     *                     this matrix.
     * @param quaternionY  The Y component of the quaternion that is to be used to set
     *                     this matrix.
     * @param quaternionZ  The Z component of the quaternion that is to be used to set
     *                     this matrix.
     * @param quaternionW  The W component of the quaternion that is to be used to set
     *                     this matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D set(double translationX, double translationY, double translationZ, double quaternionX, double quaternionY, double quaternionZ, double quaternionW) {
        final double xs = quaternionX * 2f, ys = quaternionY * 2f, zs = quaternionZ * 2f;
        final double wx = quaternionW * xs, wy = quaternionW * ys, wz = quaternionW * zs;
        final double xx = quaternionX * xs, xy = quaternionX * ys, xz = quaternionX * zs;
        final double yy = quaternionY * ys, yz = quaternionY * zs, zz = quaternionZ * zs;

        val[M00] = (1.0f - (yy + zz));
        val[M01] = (xy - wz);
        val[M02] = (xz + wy);
        val[M03] = translationX;

        val[M10] = (xy + wz);
        val[M11] = (1.0f - (xx + zz));
        val[M12] = (yz - wx);
        val[M13] = translationY;

        val[M20] = (xz - wy);
        val[M21] = (yz + wx);
        val[M22] = (1.0f - (xx + yy));
        val[M23] = translationZ;

        val[M30] = 0.f;
        val[M31] = 0.f;
        val[M32] = 0.f;
        val[M33] = 1.0f;
        return this;
    }

    /**
     * Set this matrix to the specified translation, rotation and scale.
     *
     * @param position    The translation
     * @param orientation The rotation, must be normalized
     * @param scale       The scale
     *
     * @return This matrix for chaining
     */
    public Matrix4D set(Vector3D position, QuaternionDouble orientation, Vector3D scale) {
        return set(position.x, position.y, position.z, orientation.x, orientation.y, orientation.z, orientation.w, scale.x, scale.y, scale.z);
    }

    /**
     * Sets the matrix to a rotation matrix representing the translation and
     * quaternion.
     *
     * @param translationX The X component of the translation that is to be used to set
     *                     this matrix.
     * @param translationY The Y component of the translation that is to be used to set
     *                     this matrix.
     * @param translationZ The Z component of the translation that is to be used to set
     *                     this matrix.
     * @param quaternionX  The X component of the quaternion that is to be used to set
     *                     this matrix.
     * @param quaternionY  The Y component of the quaternion that is to be used to set
     *                     this matrix.
     * @param quaternionZ  The Z component of the quaternion that is to be used to set
     *                     this matrix.
     * @param quaternionW  The W component of the quaternion that is to be used to set
     *                     this matrix.
     * @param scaleX       The X component of the scaling that is to be used to set this
     *                     matrix.
     * @param scaleY       The Y component of the scaling that is to be used to set this
     *                     matrix.
     * @param scaleZ       The Z component of the scaling that is to be used to set this
     *                     matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D set(double translationX, double translationY, double translationZ, double quaternionX, double quaternionY, double quaternionZ, double quaternionW, double scaleX, double scaleY, double scaleZ) {
        final double xs = quaternionX * 2f, ys = quaternionY * 2f, zs = quaternionZ * 2f;
        final double wx = quaternionW * xs, wy = quaternionW * ys, wz = quaternionW * zs;
        final double xx = quaternionX * xs, xy = quaternionX * ys, xz = quaternionX * zs;
        final double yy = quaternionY * ys, yz = quaternionY * zs, zz = quaternionZ * zs;

        val[M00] = scaleX * (1.0f - (yy + zz));
        val[M01] = scaleY * (xy - wz);
        val[M02] = scaleZ * (xz + wy);
        val[M03] = translationX;

        val[M10] = scaleX * (xy + wz);
        val[M11] = scaleY * (1.0f - (xx + zz));
        val[M12] = scaleZ * (yz - wx);
        val[M13] = translationY;

        val[M20] = scaleX * (xz - wy);
        val[M21] = scaleY * (yz + wx);
        val[M22] = scaleZ * (1.0f - (xx + yy));
        val[M23] = translationZ;

        val[M30] = 0.f;
        val[M31] = 0.f;
        val[M32] = 0.f;
        val[M33] = 1.0f;
        return this;
    }

    /**
     * Sets the four columns of the matrix which correspond to the x-, y- and
     * z-axis of the vector space this matrix creates as well as the 4th column
     * representing the translation of any point that is multiplied by this
     * matrix.
     *
     * @param xAxis The x-axis.
     * @param yAxis The y-axis.
     * @param zAxis The z-axis.
     * @param pos   The translation vector.
     */
    public Matrix4D set(Vector3D xAxis, Vector3D yAxis, Vector3D zAxis, Vector3D pos) {
        val[M00] = xAxis.x;
        val[M01] = xAxis.y;
        val[M02] = xAxis.z;
        val[M10] = yAxis.x;
        val[M11] = yAxis.y;
        val[M12] = yAxis.z;
        val[M20] = -zAxis.x;
        val[M21] = -zAxis.y;
        val[M22] = -zAxis.z;
        val[M03] = pos.x;
        val[M13] = pos.y;
        val[M23] = pos.z;
        val[M30] = 0;
        val[M31] = 0;
        val[M32] = 0;
        val[M33] = 1;
        return this;
    }

    /** @return a copy of this matrix */
    public Matrix4D cpy() {
        return new Matrix4D(this);
    }

    /**
     * Adds a translational component to the matrix in the 4th column. The other
     * columns are untouched.
     *
     * @param vector The translation vector to add to the current matrix. (This
     *               vector is not modified)
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D trn(Vector3D vector) {
        val[M03] += vector.x;
        val[M13] += vector.y;
        val[M23] += vector.z;
        return this;
    }

    /**
     * Adds a translational component to the matrix in the 4th column. The other
     * columns are untouched.
     *
     * @param x The x-component of the translation vector.
     * @param y The y-component of the translation vector.
     * @param z The z-component of the translation vector.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D trn(double x, double y, double z) {
        val[M03] += x;
        val[M13] += y;
        val[M23] += z;
        return this;
    }

    /** @return the backing double array */
    public double[] getValues() {
        return val;
    }

    public float[] getValuesFloat() {
        float[] res = new float[val.length];
        for (int i = 0; i < val.length; i++)
            res[i] = (float) val[i];
        return res;
    }

    /**
     * Postmultiplies this matrix with the given matrix, storing the result in
     * this matrix. For example:
     *
     * <pre>
     * A.mul(B) results in A := AB.
     * </pre>
     *
     * @param matrix The other matrix to multiply by.
     *
     * @return This matrix for the purpose of chaining operations together.
     */
    public Matrix4D mul(Matrix4D matrix) {
        matrix4_mul(val, matrix.val);
        return this;
    }

    /**
     * Premultiplies this matrix with the given matrix, storing the result in
     * this matrix. For example:
     *
     * <pre>
     * A.mulLeft(B) results in A := BA.
     * </pre>
     *
     * @param matrix The other matrix to multiply by.
     *
     * @return This matrix for the purpose of chaining operations together.
     */
    public Matrix4D mulLeft(Matrix4D matrix) {
        tmpMat.set(matrix);
        // mul(tmpMat.val, this.val);
        tmpMat.mul(this);
        return set(tmpMat);
    }

    /**
     * Transposes the matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D tra() {
        tmp[M00] = val[M00];
        tmp[M01] = val[M10];
        tmp[M02] = val[M20];
        tmp[M03] = val[M30];
        tmp[M10] = val[M01];
        tmp[M11] = val[M11];
        tmp[M12] = val[M21];
        tmp[M13] = val[M31];
        tmp[M20] = val[M02];
        tmp[M21] = val[M12];
        tmp[M22] = val[M22];
        tmp[M23] = val[M32];
        tmp[M30] = val[M03];
        tmp[M31] = val[M13];
        tmp[M32] = val[M23];
        tmp[M33] = val[M33];
        return set(tmp);
    }

    /**
     * Sets the matrix to an identity matrix.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D idt() {
        val[M00] = 1;
        val[M01] = 0;
        val[M02] = 0;
        val[M03] = 0;
        val[M10] = 0;
        val[M11] = 1;
        val[M12] = 0;
        val[M13] = 0;
        val[M20] = 0;
        val[M21] = 0;
        val[M22] = 1;
        val[M23] = 0;
        val[M30] = 0;
        val[M31] = 0;
        val[M32] = 0;
        val[M33] = 1;
        return this;
    }

    public Matrix4D inv() {
        matrix4_inv(val);
        return this;
    }

    /** @return The determinant of this matrix */
    public double det() {
        return val[M30] * val[M21] * val[M12] * val[M03] - val[M20] * val[M31] * val[M12] * val[M03] - val[M30] * val[M11] * val[M22] * val[M03] + val[M10] * val[M31] * val[M22] * val[M03] + val[M20] * val[M11] * val[M32] * val[M03] - val[M10] * val[M21] * val[M32] * val[M03] - val[M30] * val[M21] * val[M02] * val[M13] + val[M20] * val[M31] * val[M02] * val[M13] + val[M30] * val[M01] * val[M22] * val[M13] - val[M00] * val[M31] * val[M22] * val[M13] - val[M20] * val[M01] * val[M32] * val[M13]
                + val[M00] * val[M21] * val[M32] * val[M13] + val[M30] * val[M11] * val[M02] * val[M23] - val[M10] * val[M31] * val[M02] * val[M23] - val[M30] * val[M01] * val[M12] * val[M23] + val[M00] * val[M31] * val[M12] * val[M23] + val[M10] * val[M01] * val[M32] * val[M23] - val[M00] * val[M11] * val[M32] * val[M23] - val[M20] * val[M11] * val[M02] * val[M33] + val[M10] * val[M21] * val[M02] * val[M33] + val[M20] * val[M01] * val[M12] * val[M33] - val[M00] * val[M21] * val[M12] * val[M33]
                - val[M10] * val[M01] * val[M22] * val[M33] + val[M00] * val[M11] * val[M22] * val[M33];
    }

    /** @return The determinant of the 3x3 upper left matrix */
    public double det3x3() {
        return val[M00] * val[M11] * val[M22] + val[M01] * val[M12] * val[M20] + val[M02] * val[M10] * val[M21] - val[M00] * val[M12] * val[M21] - val[M01] * val[M10] * val[M22] - val[M02] * val[M11] * val[M20];
    }

    /**
     * Sets the matrix to a projection matrix with a near- and far plane, a
     * field of view in degrees and an aspect ratio.
     *
     * @param near        The near plane
     * @param far         The far plane
     * @param fov         The field of view in degrees
     * @param aspectRatio The "width over height" aspect ratio
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToProjection(double near, double far, double fov, double aspectRatio) {
        idt();
        double l_fd = 1.0 / FastMath.tan((fov * (Math.PI / 180)) / 2.0);
        double l_a1 = (far + near) / (near - far);
        double l_a2 = (2 * far * near) / (near - far);
        val[M00] = l_fd / aspectRatio;
        val[M10] = 0;
        val[M20] = 0;
        val[M30] = 0;
        val[M01] = 0;
        val[M11] = l_fd;
        val[M21] = 0;
        val[M31] = 0;
        val[M02] = 0;
        val[M12] = 0;
        val[M22] = l_a1;
        val[M32] = -1;
        val[M03] = 0;
        val[M13] = 0;
        val[M23] = l_a2;
        val[M33] = 0;

        return this;
    }

    /**
     * Sets this matrix to an orthographic projection matrix with the origin at
     * (x,y) extending by width and height. The near plane is set to 0, the far
     * plane is set to 1.
     *
     * @param x      The x-coordinate of the origin
     * @param y      The y-coordinate of the origin
     * @param width  The width
     * @param height The height
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToOrtho2D(double x, double y, double width, double height) {
        setToOrtho(x, x + width, y, y + height, 0, 1);
        return this;
    }

    /**
     * Sets this matrix to an orthographic projection matrix with the origin at
     * (x,y) extending by width and height, having a near and far plane.
     *
     * @param x      The x-coordinate of the origin
     * @param y      The y-coordinate of the origin
     * @param width  The width
     * @param height The height
     * @param near   The near plane
     * @param far    The far plane
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToOrtho2D(double x, double y, double width, double height, double near, double far) {
        setToOrtho(x, x + width, y, y + height, near, far);
        return this;
    }

    /**
     * Sets the matrix to an orthographic projection like
     * <a href="http://www.opengl.org/sdk/docs/man/xhtml/glOrtho.xml">glOrtho</a> following the
     * OpenGL equivalent
     *
     * @param left   The left clipping plane
     * @param right  The right clipping plane
     * @param bottom The bottom clipping plane
     * @param top    The top clipping plane
     * @param near   The near clipping plane
     * @param far    The far clipping plane
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToOrtho(double left, double right, double bottom, double top, double near, double far) {

        this.idt();
        double x_orth = 2 / (right - left);
        double y_orth = 2 / (top - bottom);
        double z_orth = -2 / (far - near);

        double tx = -(right + left) / (right - left);
        double ty = -(top + bottom) / (top - bottom);
        double tz = -(far + near) / (far - near);

        val[M00] = x_orth;
        val[M10] = 0;
        val[M20] = 0;
        val[M30] = 0;
        val[M01] = 0;
        val[M11] = y_orth;
        val[M21] = 0;
        val[M31] = 0;
        val[M02] = 0;
        val[M12] = 0;
        val[M22] = z_orth;
        val[M32] = 0;
        val[M03] = tx;
        val[M13] = ty;
        val[M23] = tz;
        val[M33] = 1;

        return this;
    }

    /**
     * Sets the 4th column to the translation vector.
     *
     * @param x The X coordinate of the translation vector
     * @param y The Y coordinate of the translation vector
     * @param z The Z coordinate of the translation vector
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setTranslation(double x, double y, double z) {
        val[M03] = x;
        val[M13] = y;
        val[M23] = z;
        return this;
    }

    /**
     * Sets this matrix to a translation matrix, overwriting it first by an
     * identity matrix and then setting the 4th column to the translation
     * vector.
     *
     * @param vector The translation vector
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToTranslation(Vector3D vector) {
        idt();
        val[M03] = vector.x;
        val[M13] = vector.y;
        val[M23] = vector.z;
        return this;
    }

    public Matrix4D setToTranslation(Vector3 vector) {
        idt();
        val[M03] = vector.x;
        val[M13] = vector.y;
        val[M23] = vector.z;
        return this;
    }

    /**
     * Sets this matrix to a translation matrix, overwriting it first by an
     * identity matrix and then setting the 4th column to the translation
     * vector.
     *
     * @param x The x-component of the translation vector.
     * @param y The y-component of the translation vector.
     * @param z The z-component of the translation vector.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToTranslation(double x, double y, double z) {
        idt();
        val[M03] = x;
        val[M13] = y;
        val[M23] = z;
        return this;
    }

    /**
     * Sets this matrix to a translation and scaling matrix by first overwriting
     * it with an identity and then setting the translation vector in the 4th
     * column and the scaling vector in the diagonal.
     *
     * @param translation The translation vector
     * @param scaling     The scaling vector
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToTranslationAndScaling(Vector3D translation, Vector3D scaling) {
        idt();
        val[M03] = translation.x;
        val[M13] = translation.y;
        val[M23] = translation.z;
        val[M00] = scaling.x;
        val[M11] = scaling.y;
        val[M22] = scaling.z;
        return this;
    }

    /**
     * Sets this matrix to a translation and scaling matrix by first overwriting
     * it with an identity and then setting the translation vector in the 4th
     * column and the scaling vector in the diagonal.
     *
     * @param translationX The x-component of the translation vector
     * @param translationY The y-component of the translation vector
     * @param translationZ The z-component of the translation vector
     * @param scalingX     The x-component of the scaling vector
     * @param scalingY     The x-component of the scaling vector
     * @param scalingZ     The x-component of the scaling vector
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToTranslationAndScaling(double translationX, double translationY, double translationZ, double scalingX, double scalingY, double scalingZ) {
        idt();
        val[M03] = translationX;
        val[M13] = translationY;
        val[M23] = translationZ;
        val[M00] = scalingX;
        val[M11] = scalingY;
        val[M22] = scalingZ;
        return this;
    }

    /**
     * Sets the matrix to a rotation matrix around the given axis.
     *
     * @param axis    The axis
     * @param degrees The angle in degrees
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToRotation(Vector3D axis, double degrees) {
        if (degrees == 0) {
            idt();
            return this;
        }
        return set(quat.set(axis, degrees));
    }

    /**
     * Sets the matrix to a rotation matrix around the given axis.
     *
     * @param axis    The axis
     * @param radians The angle in radians
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToRotationRad(Vector3D axis, double radians) {
        if (radians == 0) {
            idt();
            return this;
        }
        return set(quat.setFromAxisRad(axis, radians));
    }

    /**
     * Sets the matrix to a rotation matrix around the given axis.
     *
     * @param axisX   The x-component of the axis
     * @param axisY   The y-component of the axis
     * @param axisZ   The z-component of the axis
     * @param degrees The angle in degrees
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToRotation(double axisX, double axisY, double axisZ, double degrees) {
        if (degrees == 0) {
            idt();
            return this;
        }
        return set(quat.setFromAxis(axisX, axisY, axisZ, degrees));
    }

    /**
     * Sets the matrix to a rotation matrix around the given axis.
     *
     * @param axisX   The x-component of the axis
     * @param axisY   The y-component of the axis
     * @param axisZ   The z-component of the axis
     * @param radians The angle in radians
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToRotationRad(double axisX, double axisY, double axisZ, double radians) {
        if (radians == 0) {
            idt();
            return this;
        }
        return set(quat.setFromAxisRad(axisX, axisY, axisZ, radians));
    }

    /**
     * Set the matrix to a rotation matrix between two vectors.
     *
     * @param v1 The base vector
     * @param v2 The target vector
     *
     * @return This matrix for the purpose of chaining methods together
     */
    public Matrix4D setToRotation(final Vector3D v1, final Vector3D v2) {
        return set(quat.setFromCross(v1, v2));
    }

    /**
     * Set the matrix to a rotation matrix between two vectors.
     *
     * @param x1 The base vectors x value
     * @param y1 The base vectors y value
     * @param z1 The base vectors z value
     * @param x2 The target vector x value
     * @param y2 The target vector y value
     * @param z2 The target vector z value
     *
     * @return This matrix for the purpose of chaining methods together
     */
    public Matrix4D setToRotation(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
        return set(quat.setFromCross(x1, y1, z1, x2, y2, z2));
    }

    /**
     * Sets this matrix to a rotation matrix from the given euler angles.
     *
     * @param yaw   the yaw in degrees
     * @param pitch the pitch in degrees
     * @param roll  the roll in degrees
     *
     * @return This matrix
     */
    public Matrix4D setFromEulerAngles(double yaw, double pitch, double roll) {
        quat.setEulerAngles(yaw, pitch, roll);
        return set(quat);
    }

    /**
     * Sets this matrix to a scaling matrix
     *
     * @param vector The scaling vector
     *
     * @return This matrix for chaining.
     */
    public Matrix4D setToScaling(Vector3D vector) {
        idt();
        val[M00] = vector.x;
        val[M11] = vector.y;
        val[M22] = vector.z;
        return this;
    }

    /**
     * Sets this matrix to a scaling matrix
     *
     * @param x The x-component of the scaling vector
     * @param y The y-component of the scaling vector
     * @param z The z-component of the scaling vector
     *
     * @return This matrix for chaining.
     */
    public Matrix4D setToScaling(double x, double y, double z) {
        idt();
        val[M00] = x;
        val[M11] = y;
        val[M22] = z;
        return this;
    }

    /**
     * Sets the matrix to a look at matrix with a direction and an up vector.
     * Multiply with a translation matrix to get a camera model view matrix.
     *
     * @param direction The direction vector
     * @param up        The up vector
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setToLookAt(Vector3D direction, Vector3D up) {
        l_vez.set(direction).nor();
        l_vex.set(direction).nor();
        l_vex.crs(up).nor();
        l_vey.set(l_vex).crs(l_vez).nor();
        idt();
        val[M00] = l_vex.x;
        val[M01] = l_vex.y;
        val[M02] = l_vex.z;
        val[M10] = l_vey.x;
        val[M11] = l_vey.y;
        val[M12] = l_vey.z;
        val[M20] = -l_vez.x;
        val[M21] = -l_vez.y;
        val[M22] = -l_vez.z;

        return this;
    }

    public Matrix4D setToLookAt(Vector3Q direction, Vector3Q up) {
        l_vezb.set(direction).nor();
        l_vexb.set(direction).nor();
        l_vexb.crs(up).nor();
        l_veyb.set(l_vexb).crs(l_vezb).nor();
        idt();
        val[M00] = l_vexb.x.doubleValue();
        val[M01] = l_vexb.y.doubleValue();
        val[M02] = l_vexb.z.doubleValue();
        val[M10] = l_veyb.x.doubleValue();
        val[M11] = l_veyb.y.doubleValue();
        val[M12] = l_veyb.z.doubleValue();
        val[M20] = -l_vezb.x.doubleValue();
        val[M21] = -l_vezb.y.doubleValue();
        val[M22] = -l_vezb.z.doubleValue();

        return this;
    }

    /**
     * Sets this matrix to a look at matrix with the given position, target and
     * up vector.
     *
     * @param position the position
     * @param target   the target
     * @param up       the up vector
     *
     * @return This matrix
     */
    public Matrix4D setToLookAt(Vector3D position, Vector3D target, Vector3D up) {
        tmpVec.set(target).sub(position);
        setToLookAt(tmpVec, up);
        this.mul(tmpMat.setToTranslation(-position.x, -position.y, -position.z));

        return this;
    }

    public Matrix4D setToLookAt(Vector3Q position, Vector3Q target, Vector3Q up) {
        tmpVecb.set(target).sub(position);
        setToLookAt(tmpVecb, up);
        this.mul(tmpMat.setToTranslation(-position.x.doubleValue(), -position.y.doubleValue(), -position.z.doubleValue()));

        return this;
    }

    public Matrix4D setToWorld(Vector3D position, Vector3D forward, Vector3D up) {
        tmpForward.set(forward).nor();
        right.set(tmpForward).crs(up).nor();
        tmpUp.set(right).crs(tmpForward).nor();

        this.set(right, tmpUp, tmpForward, position);
        return this;
    }

    public String toString() {
        return "[" + val[M00] + "|" + val[M01] + "|" + val[M02] + "|" + val[M03] + "]\n" + "[" + val[M10] + "|" + val[M11] + "|" + val[M12] + "|" + val[M13] + "]\n" + "[" + val[M20] + "|" + val[M21] + "|" + val[M22] + "|" + val[M23] + "]\n" + "[" + val[M30] + "|" + val[M31] + "|" + val[M32] + "|" + val[M33] + "]\n";
    }

    /**
     * Linearly interpolates between this matrix and the given matrix mixing by
     * alpha
     *
     * @param matrix the matrix
     * @param alpha  the alpha value in the range [0,1]
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D lerp(Matrix4D matrix, double alpha) {
        for (int i = 0; i < 16; i++)
            this.val[i] = this.val[i] * (1 - alpha) + matrix.val[i] * alpha;
        return this;
    }

    /**
     * Sets this matrix to the given 3x3 matrix. The third column of this matrix
     * is set to (0,0,1,0).
     *
     * @param mat the matrix
     */
    public Matrix4D set(Matrix3 mat) {
        val[0] = mat.val[0];
        val[1] = mat.val[1];
        val[2] = mat.val[2];
        val[3] = 0;
        val[4] = mat.val[3];
        val[5] = mat.val[4];
        val[6] = mat.val[5];
        val[7] = 0;
        val[8] = 0;
        val[9] = 0;
        val[10] = 1;
        val[11] = 0;
        val[12] = mat.val[6];
        val[13] = mat.val[7];
        val[14] = 0;
        val[15] = mat.val[8];
        return this;
    }

    public Matrix4D scl(Vector3D scale) {
        val[M00] *= scale.x;
        val[M11] *= scale.y;
        val[M22] *= scale.z;
        return this;
    }

    public Matrix4D scl(double x, double y, double z) {
        val[M00] *= x;
        val[M11] *= y;
        val[M22] *= z;
        return this;
    }

    public Matrix4D scl(double scale) {
        val[M00] *= scale;
        val[M11] *= scale;
        val[M22] *= scale;
        return this;
    }

    public Vector3D getTranslation(Vector3D position) {
        position.x = val[M03];
        position.y = val[M13];
        position.z = val[M23];
        return position;
    }

    public Vector3 getTranslationf(Vector3 position) {
        position.x = (float) val[M03];
        position.y = (float) val[M13];
        position.z = (float) val[M23];
        return position;
    }

    public double[] getTranslation() {
        return new double[] { val[M03], val[M13], val[M23] };
    }

    /**
     * Sets the 4th column to the translation vector.
     *
     * @param vector The translation vector
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D setTranslation(Vector3D vector) {
        val[M03] = vector.x;
        val[M13] = vector.y;
        val[M23] = vector.z;
        return this;
    }

    public float[] getTranslationf() {
        return new float[] { (float) val[M03], (float) val[M13], (float) val[M23] };
    }

    public void getTranslationf(float[] vec) {
        vec[0] = (float) val[M03];
        vec[1] = (float) val[M13];
        vec[2] = (float) val[M23];
    }

    public Vector3D addTranslationTo(Vector3D position) {
        position.x += val[M03];
        position.y += val[M13];
        position.z += val[M23];
        return position;
    }

    /**
     * Gets the rotation of this matrix.
     *
     * @param rotation      The {@link QuaternionDouble} to receive the rotation
     * @param normalizeAxes True to normalize the axes, necessary when the matrix might also include scaling.
     *
     * @return The provided {@link QuaternionDouble} for chaining.
     */
    public QuaternionDouble getRotation(QuaternionDouble rotation, boolean normalizeAxes) {
        return rotation.setFromMatrix(normalizeAxes, this);
    }

    /**
     * Gets the rotation of this matrix.
     *
     * @param rotation The {@link QuaternionDouble} to receive the rotation
     *
     * @return The provided {@link QuaternionDouble} for chaining.
     */
    public QuaternionDouble getRotation(QuaternionDouble rotation) {
        return rotation.setFromMatrix(this);
    }

    /** @return the squared scale factor on the X axis */
    public double getScaleXSquared() {
        return val[Matrix4D.M00] * val[Matrix4D.M00] + val[Matrix4D.M01] * val[Matrix4D.M01] + val[Matrix4D.M02] * val[Matrix4D.M02];
    }

    /** @return the squared scale factor on the Y axis */
    public double getScaleYSquared() {
        return val[Matrix4D.M10] * val[Matrix4D.M10] + val[Matrix4D.M11] * val[Matrix4D.M11] + val[Matrix4D.M12] * val[Matrix4D.M12];
    }

    /** @return the squared scale factor on the Z axis */
    public double getScaleZSquared() {
        return val[Matrix4D.M20] * val[Matrix4D.M20] + val[Matrix4D.M21] * val[Matrix4D.M21] + val[Matrix4D.M22] * val[Matrix4D.M22];
    }

    /** @return the scale factor on the X axis (non-negative) */
    public double getScaleX() {
        return (MathUtilsDouble.isZero(val[Matrix4D.M01]) && MathUtilsDouble.isZero(val[Matrix4D.M02])) ? val[Matrix4D.M00] : FastMath.sqrt(getScaleXSquared());
    }

    /** @return the scale factor on the Y axis (non-negative) */
    public double getScaleY() {
        return (MathUtilsDouble.isZero(val[Matrix4D.M10]) && MathUtilsDouble.isZero(val[Matrix4D.M12])) ? val[Matrix4D.M11] : FastMath.sqrt(getScaleYSquared());
    }

    // @on

    /** @return the scale factor on the X axis (non-negative) */
    public double getScaleZ() {
        return (MathUtilsDouble.isZero(val[Matrix4D.M20]) && MathUtilsDouble.isZero(val[Matrix4D.M21])) ? val[Matrix4D.M22] : FastMath.sqrt(getScaleZSquared());
    }

    // @on

    public Vector3D getScale(Vector3D scale) {
        return scale.set(getScaleX(), getScaleY(), getScaleZ());
    }

    /** removes the translational part and transposes the matrix. */
    public Matrix4D toNormalMatrix() {
        val[M03] = 0;
        val[M13] = 0;
        val[M23] = 0;
        return inv().tra();
    }

    /**
     * Postmultiplies this matrix by a translation matrix. Postmultiplication is
     * also used by OpenGL ES' glTranslate/glRotate/glScale
     *
     * @param translation
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D translate(Vector3D translation) {
        return translate(translation.x, translation.y, translation.z);
    }

    /**
     * Postmultiplies this matrix by a translation matrix. Postmultiplication is
     * also used by OpenGL ES' glTranslate/glRotate/glScale
     *
     * @param translation
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D translate(double[] translation) {
        return translate(translation[0], translation[1], translation[2]);
    }

    /**
     * Postmultiplies this matrix by a translation matrix. Postmultiplication is
     * also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
     *
     * @param x Translation in the x-axis.
     * @param y Translation in the y-axis.
     * @param z Translation in the z-axis.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D translate(double x, double y, double z) {
        tmp[M00] = 1;
        tmp[M01] = 0;
        tmp[M02] = 0;
        tmp[M03] = x;
        tmp[M10] = 0;
        tmp[M11] = 1;
        tmp[M12] = 0;
        tmp[M13] = y;
        tmp[M20] = 0;
        tmp[M21] = 0;
        tmp[M22] = 1;
        tmp[M23] = z;
        tmp[M30] = 0;
        tmp[M31] = 0;
        tmp[M32] = 0;
        tmp[M33] = 1;

        matrix4_mul(val, tmp);
        return this;
    }

    /**
     * Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
     * Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param axis    The vector axis to rotate around.
     * @param degrees The angle in degrees.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D rotate(Vector3D axis, double degrees) {
        if (degrees == 0)
            return this;
        quat.set(axis, degrees);
        return rotate(quat);
    }

    /**
     * Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
     * Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param axis    The vector axis to rotate around.
     * @param radians The angle in radians.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D rotateRad(Vector3D axis, double radians) {
        if (radians == 0)
            return this;
        quat.setFromAxisRad(axis, radians);
        return rotate(quat);
    }

    /**
     * Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
     * Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale
     *
     * @param axisX   The x-axis component of the vector to rotate around.
     * @param axisY   The y-axis component of the vector to rotate around.
     * @param axisZ   The z-axis component of the vector to rotate around.
     * @param degrees The angle in degrees
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D rotate(double axisX, double axisY, double axisZ, double degrees) {
        if (degrees == 0)
            return this;
        quat.setFromAxis(axisX, axisY, axisZ, degrees);
        return rotate(quat);
    }

    /**
     * Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
     * Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale
     *
     * @param axisX   The x-axis component of the vector to rotate around.
     * @param axisY   The y-axis component of the vector to rotate around.
     * @param axisZ   The z-axis component of the vector to rotate around.
     * @param radians The angle in radians
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D rotateRad(double axisX, double axisY, double axisZ, double radians) {
        if (radians == 0)
            return this;
        quat.setFromAxisRad(axisX, axisY, axisZ, radians);
        return rotate(quat);
    }

    /**
     * Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
     * Postmultiplication is also used by OpenGL ES' 1.x
     * glTranslate/glRotate/glScale.
     *
     * @param rotation
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D rotate(QuaternionDouble rotation) {
        rotation.toMatrix(tmp);
        matrix4_mul(val, tmp);
        return this;
    }

    /**
     * Postmultiplies this matrix by the rotation between two vectors.
     *
     * @param v1 The base vector
     * @param v2 The target vector
     *
     * @return This matrix for the purpose of chaining methods together
     */
    public Matrix4D rotate(final Vector3D v1, final Vector3D v2) {
        return rotate(quat.setFromCross(v1, v2));
    }

    /**
     * Postmultiplies this matrix with a scale matrix. Postmultiplication is
     * also used by OpenGL ES' 1.x glTranslate/glRotate/glScale.
     *
     * @param scaleX The scale in the x-axis.
     * @param scaleY The scale in the y-axis.
     * @param scaleZ The scale in the z-axis.
     *
     * @return This matrix for the purpose of chaining methods together.
     */
    public Matrix4D scale(double scaleX, double scaleY, double scaleZ) {
        tmp[M00] = scaleX;
        tmp[M01] = 0;
        tmp[M02] = 0;
        tmp[M03] = 0;
        tmp[M10] = 0;
        tmp[M11] = scaleY;
        tmp[M12] = 0;
        tmp[M13] = 0;
        tmp[M20] = 0;
        tmp[M21] = 0;
        tmp[M22] = scaleZ;
        tmp[M23] = 0;
        tmp[M30] = 0;
        tmp[M31] = 0;
        tmp[M32] = 0;
        tmp[M33] = 1;

        matrix4_mul(val, tmp);
        return this;
    }

    /**
     * Copies the 4x3 upper-left sub-matrix into double array. The destination
     * array is supposed to be a column major matrix.
     *
     * @param dst the destination matrix
     */
    public void extract4x3Matrix(double[] dst) {
        dst[0] = val[M00];
        dst[1] = val[M10];
        dst[2] = val[M20];
        dst[3] = val[M01];
        dst[4] = val[M11];
        dst[5] = val[M21];
        dst[6] = val[M02];
        dst[7] = val[M12];
        dst[8] = val[M22];
        dst[9] = val[M03];
        dst[10] = val[M13];
        dst[11] = val[M23];
    }

    /**
     * Sets the given matrix to this matrix
     *
     * @param aux The out matrix
     *
     * @return The aux matrix
     */
    public Matrix4 putIn(Matrix4 aux) {
        float[] auxVal = aux.val;
        for (int i = 0; i < val.length; i++) {
            auxVal[i] = (float) val[i];
        }
        return aux;
    }
}
