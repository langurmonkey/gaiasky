/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import net.jafama.FastMath;

import static gaiasky.util.math.Matrix4D.*;

/**
 * A vector-3 that uses {@link QuadrupleImmutable}, 128-bit floating point numbers, as components.
 * This provides 33-36 significant digits of precision.
 */
public class Vector3b {
    private final static Matrix4D tmpMat = new Matrix4D();

    /** the x-component of this vector **/
    public QuadrupleImmutable x;
    /** the y-component of this vector **/
    public QuadrupleImmutable y;
    /** the z-component of this vector **/
    public QuadrupleImmutable z;

    /** Constructs a vector at (0,0,0) */
    public Vector3b() {
        this.x = QuadrupleImmutable.ZERO;
        this.y = QuadrupleImmutable.ZERO;
        this.z = QuadrupleImmutable.ZERO;
    }

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public Vector3b(QuadrupleImmutable x, QuadrupleImmutable y, QuadrupleImmutable z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public Vector3b(double x, double y, double z) {
        this.x = QuadrupleImmutable.from(x);
        this.y = QuadrupleImmutable.from(y);
        this.z = QuadrupleImmutable.from(z);
    }

    public Vector3b(Vector3D vec) {
        this(vec.x, vec.y, vec.z);
    }

    /**
     * Creates a vector from the given vector
     *
     * @param vector The vector
     */
    public Vector3b(final Vector3b vector) {
        this(vector.x, vector.y, vector.z);
    }

    /**
     * Creates a vector from the given array. The array must have at least 3
     * elements.
     *
     * @param values The array
     */
    public Vector3b(final double[] values) {
        this.set(values[0], values[1], values[2]);
    }

    /** @return The euclidean length */
    public static double len(final double x, final double y, final double z) {
        return FastMath.sqrt(x * x + y * y + z * z);
    }

    /**
     * Calculates the outer product of two given vectors <code>v</code> and
     * <code>w</code> and returns the result as a new {@link Vector3b}.
     *
     * @param v left operand
     * @param w right operand
     *
     * @return outer product of <code>v</code> and <code>w</code>
     */
    static public Vector3b crs(final Vector3b v, final Vector3b w) {
        final Vector3b res = new Vector3b(v);
        return res.crs(w);
    }

    public double x() {
        return x.doubleValue();
    }

    public double y() {
        return y.doubleValue();
    }

    public double z() {
        return z.doubleValue();
    }

    /**
     * Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     *
     * @return this vector for chaining
     */
    public Vector3b set(float x, float y, float z) {
        this.x = QuadrupleImmutable.from(x);
        this.y = QuadrupleImmutable.from(y);
        this.z = QuadrupleImmutable.from(z);
        return this;
    }

    /**
     * Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     *
     * @return this vector for chaining
     */
    public Vector3b set(double x, double y, double z) {
        this.x = QuadrupleImmutable.from(x);
        this.y = QuadrupleImmutable.from(y);
        this.z = QuadrupleImmutable.from(z);
        return this;
    }

    /**
     * Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     *
     * @return this vector for chaining
     */
    public Vector3b set(QuadrupleImmutable x, QuadrupleImmutable y, QuadrupleImmutable z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3b set(final Vector3b vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3b set(final Vector3D vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3b set(final Vector3 vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3 put(final Vector3 vec) {
        return vec.set(this.x.floatValue(), this.y.floatValue(), this.z.floatValue());
    }

    public Vector3 tov3() {
        return new Vector3(this.x.floatValue(), this.y.floatValue(), this.z.floatValue());
    }

    public Vector3 tov3(Vector3 out) {
        return out.set(this.x.floatValue(), this.y.floatValue(), this.z.floatValue());
    }

    public Vector3D put(final Vector3D vec) {
        return vec.set(this.x.doubleValue(), this.y.doubleValue(), this.z.doubleValue());
    }

    public Vector3D tov3d() {
        return new Vector3D(this.x.doubleValue(), this.y.doubleValue(), this.z.doubleValue());
    }

    public Vector3D tov3d(Vector3D out) {
        return out.set(this.x.doubleValue(), this.y.doubleValue(), this.z.doubleValue());
    }

    public Vector3b put(final Vector3b vec) {
        return vec.set(this.x, this.y, this.z);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     *
     * @param vals The array
     *
     * @return this vector for chaining
     */
    public Vector3b set(final double[] vals) {
        return this.set(vals[0], vals[1], vals[2]);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     *
     * @param vals The array
     *
     * @return this vector for chaining
     */
    public Vector3b set(final float[] vals) {
        return this.set(vals[0], vals[1], vals[2]);
    }

    /**
     * Sets the components from the given spherical coordinate
     *
     * @param azimuthalAngle The angle between x-axis in radians [0, 2pi]
     * @param polarAngle     The angle between z-axis in radians [0, pi]
     *
     * @return This vector for chaining
     */
    public Vector3b setFromSpherical(double azimuthalAngle, double polarAngle) {
        double cosPolar = MathUtilsDouble.cos(polarAngle);
        double sinPolar = MathUtilsDouble.sin(polarAngle);

        double cosAzim = MathUtilsDouble.cos(azimuthalAngle);
        double sinAzim = MathUtilsDouble.sin(azimuthalAngle);

        return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
    }

    public Vector3b setToRandomDirection() {
        double u = MathUtilsDouble.random();
        double v = MathUtilsDouble.random();

        double theta = MathUtilsDouble.PI2 * u; // azimuthal angle
        double phi = FastMath.acos(2f * v - 1f); // polar angle

        return this.setFromSpherical(theta, phi);
    }

    public Vector3b cpy() {
        return new Vector3b(this);
    }

    public Vector3b add(final Vector3b vec) {
        this.x = this.x.add(vec.x);
        this.y = this.y.add(vec.y);
        this.z = this.z.add(vec.z);
        return this;
    }

    public Vector3b add(final Vector3D vec) {
        this.x = this.x.add(vec.x);
        this.y = this.y.add(vec.y);
        this.z = this.z.add(vec.z);
        return this;
    }

    public Vector3b add(final Vector3 vec) {
        this.x = this.x.add(vec.x);
        this.y = this.y.add(vec.y);
        this.z = this.z.add(vec.z);
        return this;
    }

    /**
     * Adds the given vector to this component
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     *
     * @return This vector for chaining.
     */
    public Vector3b add(double x, double y, double z) {
        this.x = this.x.add(x);
        this.y = this.y.add(y);
        this.z = this.z.add(z);
        return this;
    }

    /**
     * Adds the given vector to this component
     *
     * @param vals The 3-value double vector.
     *
     * @return This vector for chaining.
     */
    public Vector3b add(double... vals) {
        assert vals.length == 3 : "vals must contain 3 values";
        this.x = this.x.add(vals[0]);
        this.y = this.y.add(vals[1]);
        this.z = this.z.add(vals[2]);
        return this;
    }

    /**
     * Adds the given value to all three components of the vector.
     *
     * @param value The value
     *
     * @return This vector for chaining
     */
    public Vector3b add(double value) {
        x = x.add(value);
        y = y.add(value);
        z = z.add(value);
        return this;
    }

    public Vector3b sub(final Vector3b vec) {
        x = x.subtract(vec.x);
        y = y.subtract(vec.y);
        z = z.subtract(vec.z);
        return this;
    }

    public Vector3b sub(final Vector3 a_vec) {
        return this.sub(a_vec.x, a_vec.y, a_vec.z);
    }

    public Vector3b sub(final Vector3D a_vec) {
        return this.sub(a_vec.x, a_vec.y, a_vec.z);
    }

    /**
     * Subtracts the other vector from this vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     *
     * @return This vector for chaining
     */
    public Vector3b sub(double x, double y, double z) {
        this.x = this.x.subtract(x);
        this.y = this.y.subtract(y);
        this.z = this.z.subtract(z);
        return this;
    }

    /**
     * Subtracts the given value from all components of this vector
     *
     * @param value The value
     *
     * @return This vector for chaining
     */
    public Vector3b sub(double value) {
        x = x.subtract(value);
        y = y.subtract(value);
        z = z.subtract(value);
        return this;
    }

    public Vector3b div(QuadrupleImmutable num) {
        x = x.divide(num);
        y = y.divide(num);
        z = z.divide(num);
        return this;
    }

    public Vector3b scl(QuadrupleImmutable scl) {
        x = x.multiply(scl);
        y = y.multiply(scl);
        z = z.multiply(scl);
        return this;
    }

    public Vector3b scl(double scalar) {
        x = x.multiply(scalar);
        y = y.multiply(scalar);
        z = z.multiply(scalar);
        return this;
    }

    public Vector3b scl(final Vector3b vec) {
        x = x.multiply(vec.x);
        y = y.multiply(vec.y);
        z = z.multiply(vec.z);
        return this;
    }

    /**
     * Scales this vector by the given values
     *
     * @param x X value
     * @param y Y value
     * @param z Z value
     *
     * @return This vector for chaining
     */
    public Vector3b scl(double x, double y, double z) {
        this.x = this.x.multiply(x);
        this.y = this.y.multiply(y);
        this.z = this.z.multiply(z);
        return this;
    }

    public Vector3b mul(Vector3b vec) {
        x = x.multiply(vec.x);
        y = y.multiply(vec.y);
        z = z.multiply(vec.z);
        return this;
    }

    public Vector3b div(Vector3b vec) {
        x = x.divide(vec.x);
        y = y.divide(vec.y);
        z = z.divide(vec.z);
        return this;
    }

    public double lenDouble() {
        return len(x.doubleValue(), y.doubleValue(), z.doubleValue());
    }

    public float lenF() {
        return len().floatValue();
    }

    public QuadrupleImmutable len() {
        return len2().sqrt();
    }

    public double len2D() {
        return this.len2()
                .doubleValue();
    }

    public QuadrupleImmutable len2() {
        return x.multiply(x)
                .add(y.multiply(y))
                .add(z.multiply(z));
    }

    /**
     * @param vec The other vector
     *
     * @return Whether this and the other vector are equal
     */
    public boolean idt(final Vector3b vec) {
        return x.equals(vec.x) && y.equals(vec.y) && z.equals(vec.z);
    }

    public double dstD(final Vector3b vec) {
        return dst(vec).doubleValue();
    }

    public double dstD(final Vector3b vec, final Vector3b aux) {
        return dst(vec, aux).doubleValue();
    }

    public QuadrupleImmutable dst(final Vector3b vec) {
        var a = vec.x.subtract(this.x);
        var b = vec.y.subtract(this.y);
        var c = vec.z.subtract(this.z);
        return a.multiply(a)
                .add(b.multiply(b))
                .add(c.multiply(c))
                .sqrt();
    }

    /**
     * Faster version of {@link Vector3b#dst(Vector3b)}, using an auxiliary vector.
     *
     * @param vec The vector to compute the distance to.
     * @param aux The auxiliary vector.
     *
     * @return The distance between the two points.
     */
    public QuadrupleImmutable dst(final Vector3b vec, final Vector3b aux) {
        return aux.set(this)
                .sub(vec)
                .len();
    }

    public QuadrupleImmutable dst(final Vector3D vec) {
        return dst2(vec).sqrt();
    }

    /**
     * Faster version of {@link Vector3b#dst(Vector3D)}, using an auxiliary vector.
     *
     * @param vec The vector to compute the distance to.
     * @param aux The auxiliary vector.
     *
     * @return The distance between the two points.
     */
    public QuadrupleImmutable dst(final Vector3D vec, final Vector3b aux) {
        return aux.set(this)
                .sub(vec)
                .len();
    }

    public double dstD(double x, double y, double z) {
        return dst(x, y, z).doubleValue();
    }

    /** @return the distance between this point and the given point */
    public QuadrupleImmutable dst(double x, double y, double z) {
        return dst2(x, y, z).sqrt();
    }

    /** @return the distance between this point and the given point */
    public QuadrupleImmutable dst(double x, double y, double z, Vector3b aux) {
        return aux.set(this)
                .sub(x, y, z)
                .len();
    }

    public double dst2D(Vector3b vec) {
        return this.dst2(vec)
                .doubleValue();
    }

    public double dst2D(Vector3D vec) {
        return this.dst2D(vec.x, vec.y, vec.z);
    }

    public QuadrupleImmutable dst2(Vector3b vec) {
        QuadrupleImmutable a = vec.x.subtract(this.x);
        QuadrupleImmutable b = vec.y.subtract(this.y);
        QuadrupleImmutable c = vec.z.subtract(this.z);
        return a.multiply(a)
                .add(b.multiply(b))
                .add(c.multiply(c));
    }

    public double dst2D(double x, double y, double z) {
        return dst2(x, y, z).doubleValue();
    }

    public QuadrupleImmutable dst2(Vector3D vec) {
        return dst2(vec.x, vec.y, vec.z);
    }

    /**
     * Returns the squared distance between this point and the given point
     *
     * @param x The x-component of the other point
     * @param y The y-component of the other point
     * @param z The z-component of the other point
     *
     * @return The squared distance
     */
    public QuadrupleImmutable dst2(double x, double y, double z) {
        var a = QuadrupleImmutable.from(x).subtract(this.x);
        var b = QuadrupleImmutable.from(y).subtract(this.y);
        var c = QuadrupleImmutable.from(z).subtract(this.z);
        return a.multiply(a).add(b.multiply(b)).add(c.multiply(c));
    }

    public Vector3b nor() {
        final var len2 = this.len2();
        final var len2d = len2.doubleValue();
        if (len2d == 0f || len2d == 1f)
            return this;
        return this.div(len2.sqrt());
    }

    public double dot(final Vector3D vec) {
        var vx = QuadrupleImmutable.from(vec.x);
        var vy = QuadrupleImmutable.from(vec.y);
        var vz = QuadrupleImmutable.from(vec.z);
        return this.x.multiply(vx)
                .add(this.y.multiply(vy))
                .add(this.z.multiply(vz))
                .doubleValue();
    }

    public double dotD(final Vector3b vec) {
        return this.dot(vec)
                .doubleValue();
    }

    public QuadrupleImmutable dot(final Vector3b vec) {
        return this.x.multiply(vec.x)
                .add(this.y.multiply(vec.y))
                .add(this.z.multiply(vec.z));
    }

    public double dotd(double x, double y, double z) {
        return this.dot(QuadrupleImmutable.from(x), QuadrupleImmutable.from(y), QuadrupleImmutable.from(z))
                .doubleValue();
    }

    /**
     * Returns the dot product between this and the given vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     *
     * @return The dot product
     */
    public QuadrupleImmutable dot(QuadrupleImmutable x, QuadrupleImmutable y, QuadrupleImmutable z) {
        return this.x.multiply(x)
                .add(this.y.multiply(y))
                .add(this.z.multiply(z));
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     *
     * @param vec The other vector
     *
     * @return This vector for chaining
     */
    public Vector3b crs(final Vector3b vec) {
        return this.set(this.y.multiply(vec.z)
                                .subtract(this.z.multiply(vec.y)), this.z.multiply(vec.x)
                                .subtract(this.x.multiply(vec.z)), this.x.multiply(vec.y)
                                .subtract(this.y.multiply(vec.x)));
    }

    public Vector3b crs(final Vector3D vec) {
        return this.crs(vec.x, vec.y, vec.z);
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     *
     * @return This vector for chaining
     */
    public Vector3b crs(double x, double y, double z) {
        var vx = QuadrupleImmutable.from(x);
        var vy = QuadrupleImmutable.from(y);
        var vz = QuadrupleImmutable.from(z);
        return this.set(this.y.multiply(vz)
                                .subtract(this.z.multiply(vy)), this.z.multiply(vx)
                                .subtract(this.x.multiply(vz)), this.x.multiply(vy)
                                .subtract(this.y.multiply(vx)));
    }

    /**
     * Left-multiplies the vector by the given 4x3 column major matrix. The matrix
     * should be composed by a 3x3 matrix representing rotation and scale plus a 1x3
     * matrix representing the translation.
     *
     * @param matrix The matrix
     *
     * @return This vector for chaining
     */
    public Vector3b mul4x3(double[] matrix) {
        var m0 = QuadrupleImmutable.from(matrix[0]);
        var m1 = QuadrupleImmutable.from(matrix[1]);
        var m2 = QuadrupleImmutable.from(matrix[2]);
        var m3 = QuadrupleImmutable.from(matrix[3]);
        var m4 = QuadrupleImmutable.from(matrix[4]);
        var m5 = QuadrupleImmutable.from(matrix[5]);
        var m6 = QuadrupleImmutable.from(matrix[6]);
        var m7 = QuadrupleImmutable.from(matrix[7]);
        var m8 = QuadrupleImmutable.from(matrix[8]);
        var m9 = QuadrupleImmutable.from(matrix[9]);
        var m10 = QuadrupleImmutable.from(matrix[10]);
        var m11 = QuadrupleImmutable.from(matrix[11]);
        return set(x.multiply(m0)
                           .add(y.multiply(m3))
                           .add(z.multiply(m6))
                           .add(m9), x.multiply(m1)
                           .add(y.multiply(m4))
                           .add(z.multiply(m7))
                           .add(m10), x.multiply(m2)
                           .add(y.multiply(m5))
                           .add(z.multiply(m8))
                           .add(m11));
    }

    /**
     * Left-multiplies the vector by the given matrix, assuming the fourth (w)
     * component of the vector is 1.
     *
     * @param matrix The matrix
     *
     * @return This vector for chaining
     */
    public Vector3b mul(final Matrix4D matrix) {
        final double[] mat = matrix.val;
        var m00 = QuadrupleImmutable.from(mat[M00]);
        var m01 = QuadrupleImmutable.from(mat[M01]);
        var m02 = QuadrupleImmutable.from(mat[M02]);
        var m03 = QuadrupleImmutable.from(mat[M03]);
        var m10 = QuadrupleImmutable.from(mat[M10]);
        var m11 = QuadrupleImmutable.from(mat[M11]);
        var m12 = QuadrupleImmutable.from(mat[M12]);
        var m13 = QuadrupleImmutable.from(mat[M13]);
        var m20 = QuadrupleImmutable.from(mat[M20]);
        var m21 = QuadrupleImmutable.from(mat[M21]);
        var m22 = QuadrupleImmutable.from(mat[M22]);
        var m23 = QuadrupleImmutable.from(mat[M23]);
        return this.set(x.multiply(m00)
                                .add(y.multiply(m01))
                                .add(z.multiply(m02))
                                .add(m03), x.multiply(m10)
                                .add(y.multiply(m11))
                                .add(z.multiply(m12))
                                .add(m13), x.multiply(m20)
                                .add(y.multiply(m21))
                                .add(z.multiply(m22))
                                .add(m23));
    }

    /**
     * Multiplies the vector by the given {@link Quaternion}.
     *
     * @return This vector for chaining
     */
    public Vector3b mul(final QuaternionDouble quat) {
        return quat.transform(this);
    }

    /**
     * Rotates this vector by the given angle in degrees around the given axis.
     *
     * @param degrees the angle in degrees
     * @param axisX   the x-component of the axis
     * @param axisY   the y-component of the axis
     * @param axisZ   the z-component of the axis
     *
     * @return This vector for chaining
     */
    public Vector3b rotate(double degrees, double axisX, double axisY, double axisZ) {
        return this.mul(tmpMat.setToRotation(axisX, axisY, axisZ, degrees));
    }

    /**
     * Rotates this vector by the given angle in degrees around the given axis.
     *
     * @param axis    the axis
     * @param degrees the angle in degrees
     *
     * @return This vector for chaining
     */
    public Vector3b rotate(final Vector3D axis, double degrees) {
        tmpMat.setToRotation(axis, degrees);
        return this.mul(tmpMat);
    }

    /**
     * Sets the given matrix to a translation matrix using this vector.
     *
     * @param matrix The matrix to set as a translation matrix.
     *
     * @return The matrix aux, for chaining.
     */
    public Matrix4 setToTranslation(Matrix4 matrix) {
        return matrix.idt()
                .translate(x.floatValue(), y.floatValue(), z.floatValue());
    }

    /**
     * Sets the given matrix to a translation matrix using this vector.
     *
     * @param matrix The matrix to set as a translation matrix.
     *
     * @return The matrix aux, for chaining.
     */
    public Matrix4D setToTranslation(Matrix4D matrix) {
        return matrix.idt()
                .translate(x.doubleValue(), y.doubleValue(), z.doubleValue());
    }

    public boolean isUnit() {
        return isUnit(0.000000001);
    }

    public boolean isUnit(final double margin) {
        return FastMath.abs(len2D() - 1f) < margin;
    }

    public boolean isZero() {
        return x.doubleValue() == 0 && y.doubleValue() == 0 && z.doubleValue() == 0;
    }

    public boolean isZero(final double margin) {
        return len2D() < margin;
    }

    public String toString() {
        return x + "," + y + "," + z;
    }

    public Vector3b setLength(double len) {
        return setLength2(len * len);
    }

    public Vector3b setLength2(double len2) {
        double oldLen2 = len2D();
        return (oldLen2 == 0 || oldLen2 == len2) ? this : scl(Math.sqrt(len2 / oldLen2));
    }

    public Vector3b clamp(double min, double max) {
        final double l2 = len2D();
        if (l2 == 0f)
            return this;
        if (l2 > max * max)
            return nor().scl(max);
        if (l2 < min * min)
            return nor().scl(min);
        return this;
    }

    public QuadrupleImmutable[] values() {
        return new QuadrupleImmutable[]{x, y, z};
    }

    public double[] valuesD() {
        return new double[]{x.doubleValue(), y.doubleValue(), z.doubleValue()};
    }

    public float[] valuesF() {
        return new float[]{x.floatValue(), y.floatValue(), z.floatValue()};
    }

    public float[] valuesF(float[] vec) {
        vec[0] = x.floatValue();
        vec[1] = y.floatValue();
        vec[2] = z.floatValue();
        return vec;
    }


    /** Gets the angle in degrees between the two vectors **/
    public double angle(Vector3b v) {
        return MathUtilsDouble.radiansToDegrees * FastMath.acos(
                MathUtils.clamp(this.dotD(v) / (this.lenDouble() * v.lenDouble()), -1d, 1d));
    }

    /** Gets the angle in degrees between the two vectors **/
    public double angle(Vector3D v) {
        return MathUtilsDouble.radiansToDegrees * FastMath.acos(
                MathUtils.clamp(this.dot(v) / (this.lenDouble() * v.len()), -1d, 1d));
    }

    /** Gets the angle in degrees between the two vectors **/
    public double anglePrecise(Vector3b v) {
        return MathUtilsDouble.radiansToDegrees * FastMath.acos(
                MathUtils.clamp(this.dotD(v) / (this.lenDouble() * v.lenDouble()), -1d, 1d));
    }

    /** Gets the angle in degrees between the two vectors **/
    public double anglePrecise(Vector3D v) {
        return MathUtilsDouble.radiansToDegrees * FastMath.acos(
                MathUtils.clamp(this.dot(v) / (this.lenDouble() * v.len()), -1d, 1d));
    }

    @Override
    public int hashCode() {
        final long prime = 31;
        long res = 1;
        res = prime * res + x.hashCode();
        res = prime * res + y.hashCode();
        res = prime * res + z.hashCode();
        return (int) res;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vector3b other = (Vector3b) obj;
        if (x.hashCode() != other.x.hashCode())
            return false;
        if (y.hashCode() != other.y.hashCode())
            return false;
        return z.hashCode() == other.z.hashCode();
    }

    public Vector3b setZero() {
        this.x = QuadrupleImmutable.ZERO;
        this.y = QuadrupleImmutable.ZERO;
        this.z = QuadrupleImmutable.ZERO;
        return this;
    }

    public boolean hasNaN() {
        return x.isNaN() || y.isNaN() || z.isNaN();
    }

}
