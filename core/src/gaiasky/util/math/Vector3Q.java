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
 * A vector-3 that uses {@link Quadruple}, 128-bit floating point numbers, as components.
 * This provides 33-36 significant digits of precision.
 */
public class Vector3Q {
    private final static Matrix4D tmpMat = new Matrix4D();

    /** the x-component of this vector **/
    public Quadruple x;
    /** the y-component of this vector **/
    public Quadruple y;
    /** the z-component of this vector **/
    public Quadruple z;

    /** Constructs a vector at (0,0,0) */
    public Vector3Q() {
        this.x = Quadruple.zero();
        this.y = Quadruple.zero();
        this.z = Quadruple.zero();
    }

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public Vector3Q(Quadruple x, Quadruple y, Quadruple z) {
        this.x = new Quadruple().assign(x);
        this.y = new Quadruple().assign(y);
        this.z = new Quadruple().assign(z);
    }

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public Vector3Q(double x, double y, double z) {
        this.x = new Quadruple(x);
        this.y = new Quadruple(y);
        this.z = new Quadruple(z);
    }

    public Vector3Q(Vector3D vec) {
        this(vec.x, vec.y, vec.z);
    }

    /**
     * Creates a vector from the given vector
     *
     * @param vector The vector
     */
    public Vector3Q(final Vector3Q vector) {
        this(vector.x, vector.y, vector.z);
    }

    /**
     * Creates a vector from the given array. The array must have at least 3
     * elements.
     *
     * @param values The array
     */
    public Vector3Q(final double[] values) {
        this.set(values[0], values[1], values[2]);
    }

    /** @return The euclidean length */
    public static double len(final double x, final double y, final double z) {
        return FastMath.sqrt(x * x + y * y + z * z);
    }

    /**
     * Calculates the outer product of two given vectors <code>v</code> and
     * <code>w</code> and returns the result as a new {@link Vector3Q}.
     *
     * @param v left operand
     * @param w right operand
     *
     * @return outer product of <code>v</code> and <code>w</code>
     */
    static public Vector3Q crs(final Vector3Q v, final Vector3Q w) {
        final Vector3Q res = new Vector3Q(v);
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
    public Vector3Q set(float x, float y, float z) {
        this.x.assign(x);
        this.y.assign(y);
        this.z.assign(z);
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
    public Vector3Q set(double x, double y, double z) {
        this.x.assign(x);
        this.y.assign(y);
        this.z.assign(z);
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
    public Vector3Q set(Quadruple x, Quadruple y, Quadruple z) {
        this.x.assign(x);
        this.y.assign(y);
        this.z.assign(z);
        return this;
    }

    public Vector3Q set(final Vector3Q vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3Q set(final Vector3D vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3Q set(final Vector3 vec) {
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

    public Vector3Q put(final Vector3Q vec) {
        return vec.set(this.x, this.y, this.z);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     *
     * @param vals The array
     *
     * @return this vector for chaining
     */
    public Vector3Q set(final double[] vals) {
        return this.set(vals[0], vals[1], vals[2]);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     *
     * @param vals The array
     *
     * @return this vector for chaining
     */
    public Vector3Q set(final float[] vals) {
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
    public Vector3Q setFromSpherical(double azimuthalAngle, double polarAngle) {
        double cosPolar = MathUtilsDouble.cos(polarAngle);
        double sinPolar = MathUtilsDouble.sin(polarAngle);

        double cosAzim = MathUtilsDouble.cos(azimuthalAngle);
        double sinAzim = MathUtilsDouble.sin(azimuthalAngle);

        return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
    }

    public Vector3Q setToRandomDirection() {
        double u = MathUtilsDouble.random();
        double v = MathUtilsDouble.random();

        double theta = MathUtilsDouble.PI2 * u; // azimuthal angle
        double phi = FastMath.acos(2f * v - 1f); // polar angle

        return this.setFromSpherical(theta, phi);
    }

    public Vector3Q cpy() {
        return new Vector3Q(this);
    }

    public Vector3Q add(final Vector3Q vec) {
        this.x.add(vec.x);
        this.y.add(vec.y);
        this.z.add(vec.z);
        return this;
    }

    public Vector3Q add(final Vector3D vec) {
        var aux = new Quadruple();
        this.x.add(aux.assign(vec.x));
        this.y.add(aux.assign(vec.y));
        this.z.add(aux.assign(vec.z));
        return this;
    }

    public Vector3Q add(final Vector3 vec) {
        var aux = new Quadruple();
        this.x.add(aux.assign(vec.x));
        this.y.add(aux.assign(vec.y));
        this.z.add(aux.assign(vec.z));
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
    public Vector3Q add(double x, double y, double z) {
        var aux = new Quadruple();
        this.x.add(aux.assign(x));
        this.y.add(aux.assign(y));
        this.z.add(aux.assign(z));
        return this;
    }

    /**
     * Adds the given vector to this component
     *
     * @param vals The 3-value double vector.
     *
     * @return This vector for chaining.
     */
    public Vector3Q add(double... vals) {
        assert vals.length == 3 : "vals must contain 3 values";
        var aux = new Quadruple();
        this.x.add(aux.assign(vals[0]));
        this.y.add(aux.assign(vals[1]));
        this.z.add(aux.assign(vals[2]));
        return this;
    }

    /**
     * Adds the given value to all three components of the vector.
     *
     * @param value The value
     *
     * @return This vector for chaining
     */
    public Vector3Q add(double value) {
        var val = new Quadruple(value);
        x.add(val);
        y.add(val);
        z.add(val);
        return this;
    }

    public Vector3Q sub(final Vector3Q vec) {
        x.subtract(vec.x);
        y.subtract(vec.y);
        z.subtract(vec.z);
        return this;
    }

    public Vector3Q sub(final Vector3 a_vec) {
        return this.sub(a_vec.x, a_vec.y, a_vec.z);
    }

    public Vector3Q sub(final Vector3D a_vec) {
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
    public Vector3Q sub(double x, double y, double z) {
        this.x.subtract(x);
        this.y.subtract(y);
        this.z.subtract(z);
        return this;
    }

    /**
     * Subtracts the given value from all components of this vector
     *
     * @param value The value
     *
     * @return This vector for chaining
     */
    public Vector3Q sub(double value) {
        var val = new Quadruple(value);
        x.subtract(val);
        y.subtract(val);
        z.subtract(val);
        return this;
    }

    public Vector3Q div(Quadruple num) {
        x.divide(num);
        y.divide(num);
        z.divide(num);
        return this;
    }

    public Vector3Q scl(Quadruple scl) {
        x.multiply(scl);
        y.multiply(scl);
        z.multiply(scl);
        return this;
    }

    public Vector3Q scl(double scalar) {
        x.multiply(scalar);
        y.multiply(scalar);
        z.multiply(scalar);
        return this;
    }

    public Vector3Q scl(final Vector3Q vec) {
        x.multiply(vec.x);
        y.multiply(vec.y);
        z.multiply(vec.z);
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
    public Vector3Q scl(double x, double y, double z) {
        this.x.multiply(x);
        this.y.multiply(y);
        this.z.multiply(z);
        return this;
    }

    public Vector3Q mul(Vector3Q vec) {
        x.multiply(vec.x);
        y.multiply(vec.y);
        z.multiply(vec.z);
        return this;
    }

    public Vector3Q div(Vector3Q vec) {
        x.divide(vec.x);
        y.divide(vec.y);
        z.divide(vec.z);
        return this;
    }

    public double lenDouble() {
        return len(x.doubleValue(), y.doubleValue(), z.doubleValue());
    }

    public float lenF() {
        return len().floatValue();
    }

    public Quadruple len() {
        return len2().sqrt();
    }

    public double len2D() {
        return this.len2().doubleValue();
    }

    public Quadruple len2() {
        var res = new Quadruple(x);
        var aux = new Quadruple();
        return res.multiply(x).add(aux.assign(y).multiply(y)).add(aux.assign(z).multiply(z));
    }

    /**
     * @param vec The other vector
     *
     * @return Whether this and the other vector are equal
     */
    public boolean idt(final Vector3Q vec) {
        return x.equals(vec.x) && y.equals(vec.y) && z.equals(vec.z);
    }

    public double dstD(final Vector3Q vec) {
        return dst(vec).doubleValue();
    }

    public double dstD(final Vector3Q vec, final Vector3Q aux) {
        return dst(vec, aux).doubleValue();
    }

    public Quadruple dst(final Vector3Q vec) {
        return dst2(vec).sqrt();
    }

    /**
     * Faster version of {@link Vector3Q#dst(Vector3Q)}, using an auxiliary vector.
     *
     * @param vec The vector to compute the distance to.
     * @param aux The auxiliary vector.
     *
     * @return The distance between the two points.
     */
    public Quadruple dst(final Vector3Q vec, final Vector3Q aux) {
        return aux.set(this).sub(vec).len();
    }

    public Quadruple dst(final Vector3D vec) {
        return dst2(vec).sqrt();
    }

    /**
     * Faster version of {@link Vector3Q#dst(Vector3D)}, using an auxiliary vector.
     *
     * @param vec The vector to compute the distance to.
     * @param aux The auxiliary vector.
     *
     * @return The distance between the two points.
     */
    public Quadruple dst(final Vector3D vec, final Vector3Q aux) {
        return aux.set(this).sub(vec).len();
    }

    public double dstD(double x, double y, double z) {
        return dst(x, y, z).doubleValue();
    }

    /** @return the distance between this point and the given point */
    public Quadruple dst(double x, double y, double z) {
        return dst2(x, y, z).sqrt();
    }

    /** @return the distance between this point and the given point */
    public Quadruple dst(double x, double y, double z, Vector3Q aux) {
        return aux.set(this).sub(x, y, z).len();
    }

    public double dst2D(Vector3Q vec) {
        return this.dst2(vec).doubleValue();
    }

    public double dst2D(Vector3D vec) {
        return this.dst2D(vec.x, vec.y, vec.z);
    }

    public Quadruple dst2(Vector3Q vec) {
        var res = new Quadruple(vec.x).subtract(this.x);
        var aux = new Quadruple();
        return res.multiply(res).add(aux.assign(vec.y).subtract(this.y).multiply(aux)).add(aux.assign(vec.z).subtract(this.z).multiply(aux));
    }

    public double dst2D(double x, double y, double z) {
        return dst2(x, y, z).doubleValue();
    }

    public Quadruple dst2(Vector3D vec) {
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
    public Quadruple dst2(double x, double y, double z) {
        var res = new Quadruple(x).subtract(this.x);
        var aux = new Quadruple();
        return res.multiply(res).add(aux.assign(y).subtract(this.y).multiply(aux)).add(aux.assign(z).subtract(this.z).multiply(aux));
    }

    public Vector3Q nor() {
        final var len2 = this.len2();
        final var len2d = len2.doubleValue();
        if (len2d == 0f || len2d == 1f)

            return this;
        return this.div(len2.sqrt());
    }

    public double dot(final Vector3D vec) {
        var res = new Quadruple();
        var aux = new Quadruple();
        return res.assign(this.x).multiply(vec.x).add(aux.assign(this.y).multiply(vec.y)).add(aux.assign(this.z).multiply(vec.z)).doubleValue();
    }

    public double dotD(final Vector3Q vec) {
        return this.dot(vec).doubleValue();
    }

    public Quadruple dot(final Vector3Q vec) {
        var res = new Quadruple();
        var aux = new Quadruple();
        return res.assign(this.x).multiply(vec.x).add(aux.assign(this.y).multiply(vec.y)).add(aux.assign(this.z).multiply(vec.z));
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
    public Quadruple dot(Quadruple x, Quadruple y, Quadruple z) {
        var res = new Quadruple();
        var aux = new Quadruple();
        return res.assign(this.x).multiply(x).add(aux.assign(this.y).multiply(y)).add(aux.assign(this.z).multiply(z));
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     *
     * @param vec The other vector
     *
     * @return This vector for chaining
     */
    public Vector3Q crs(final Vector3Q vec) {
        var res = new Vector3Q();
        var aux = new Quadruple();
        res.x.assign(this.y).multiply(vec.z).subtract(aux.assign(this.z).multiply(vec.y));
        res.y.assign(this.z).multiply(vec.x).subtract(aux.assign(this.x).multiply(vec.z));
        res.z.assign(this.x).multiply(vec.y).subtract(aux.assign(this.y).multiply(vec.x));
        return this.set(res);
    }

    public Vector3Q crs(final Vector3D vec) {
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
    public Vector3Q crs(final double x, final double y, final double z) {
        var res = new Vector3Q();
        var aux = new Quadruple();
        res.x.assign(this.y).multiply(z).subtract(aux.assign(this.z).multiply(y));
        res.y.assign(this.z).multiply(x).subtract(aux.assign(this.x).multiply(z));
        res.z.assign(this.x).multiply(y).subtract(aux.assign(this.y).multiply(x));
        return this.set(res);
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
    public Vector3Q mul4x3(double[] matrix) {
        var m0 = Quadruple.valueOf(matrix[0]);
        var m1 = Quadruple.valueOf(matrix[1]);
        var m2 = Quadruple.valueOf(matrix[2]);
        var m3 = Quadruple.valueOf(matrix[3]);
        var m4 = Quadruple.valueOf(matrix[4]);
        var m5 = Quadruple.valueOf(matrix[5]);
        var m6 = Quadruple.valueOf(matrix[6]);
        var m7 = Quadruple.valueOf(matrix[7]);
        var m8 = Quadruple.valueOf(matrix[8]);
        var m9 = Quadruple.valueOf(matrix[9]);
        var m10 = Quadruple.valueOf(matrix[10]);
        var m11 = Quadruple.valueOf(matrix[11]);
        var aux = new Vector3Q(this);
        var rx = new Quadruple(aux.x).multiply(m0).add(aux.y.multiply(m3)).add(aux.z.multiply(m6)).add(m9);
        aux.set(this);
        var ry = new Quadruple(aux.x).multiply(m1).add(aux.y.multiply(m4)).add(aux.z.multiply(m7)).add(m10);
        aux.set(this);
        var rz = new Quadruple(aux.x).multiply(m2).add(aux.y.multiply(m5)).add(aux.z.multiply(m8)).add(m11);
        return set(rx, ry, rz);
    }

    /**
     * Left-multiplies the vector by the given matrix, assuming the fourth (w)
     * component of the vector is 1.
     *
     * @param matrix The matrix
     *
     * @return This vector for chaining
     */
    public Vector3Q mul(final Matrix4D matrix) {
        final double[] mat = matrix.val;
        var m00 = Quadruple.valueOf(mat[M00]);
        var m01 = Quadruple.valueOf(mat[M01]);
        var m02 = Quadruple.valueOf(mat[M02]);
        var m03 = Quadruple.valueOf(mat[M03]);
        var m10 = Quadruple.valueOf(mat[M10]);
        var m11 = Quadruple.valueOf(mat[M11]);
        var m12 = Quadruple.valueOf(mat[M12]);
        var m13 = Quadruple.valueOf(mat[M13]);
        var m20 = Quadruple.valueOf(mat[M20]);
        var m21 = Quadruple.valueOf(mat[M21]);
        var m22 = Quadruple.valueOf(mat[M22]);
        var m23 = Quadruple.valueOf(mat[M23]);
        var aux = new Vector3Q(this);
        var rx = new Quadruple(aux.x).multiply(m00).add(aux.y.multiply(m01)).add(aux.z.multiply(m02)).add(m03);
        aux.set(this);
        var ry = new Quadruple(aux.x).multiply(m10).add(aux.y.multiply(m11)).add(aux.z.multiply(m12)).add(m13);
        aux.set(this);
        var rz = new Quadruple(aux.x).multiply(m20).add(aux.y.multiply(m21)).add(aux.z.multiply(m22)).add(m23);
        return set(rx, ry, rz);
    }

    /**
     * Multiplies the vector by the given {@link Quaternion}.
     *
     * @return This vector for chaining
     */
    public Vector3Q mul(final QuaternionDouble quat) {
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
    public Vector3Q rotate(double degrees, double axisX, double axisY, double axisZ) {
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
    public Vector3Q rotate(final Vector3D axis, double degrees) {
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
        return matrix.idt().translate(x.floatValue(), y.floatValue(), z.floatValue());
    }

    /**
     * Sets the given matrix to a translation matrix using this vector.
     *
     * @param matrix The matrix to set as a translation matrix.
     *
     * @return The matrix aux, for chaining.
     */
    public Matrix4D setToTranslation(Matrix4D matrix) {
        return matrix.idt().translate(x.doubleValue(), y.doubleValue(), z.doubleValue());
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

    public Vector3Q setLength(double len) {
        return setLength2(len * len);
    }

    public Vector3Q setLength2(double len2) {
        double oldLen2 = len2D();
        return (oldLen2 == 0 || oldLen2 == len2) ? this : scl(Math.sqrt(len2 / oldLen2));
    }

    public Vector3Q clamp(double min, double max) {
        final double l2 = len2D();
        if (l2 == 0f)
            return this;
        if (l2 > max * max)
            return nor().scl(max);
        if (l2 < min * min)
            return nor().scl(min);
        return this;
    }

    public Quadruple[] values() {
        return new Quadruple[]{x, y, z};
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
    public double angle(Vector3Q v) {
        return MathUtilsDouble.radiansToDegrees * FastMath.acos(MathUtils.clamp(this.dotD(v) / (this.lenDouble() * v.lenDouble()), -1d, 1d));
    }

    /** Gets the angle in degrees between the two vectors **/
    public double angle(Vector3D v) {
        return MathUtilsDouble.radiansToDegrees * FastMath.acos(MathUtils.clamp(this.dot(v) / (this.lenDouble() * v.len()), -1d, 1d));
    }

    /** Gets the angle in degrees between the two vectors **/
    public double anglePrecise(Vector3Q v) {
        return MathUtilsDouble.radiansToDegrees * FastMath.acos(MathUtils.clamp(this.dotD(v) / (this.lenDouble() * v.lenDouble()), -1d, 1d));
    }

    /** Gets the angle in degrees between the two vectors **/
    public double anglePrecise(Vector3D v) {
        return MathUtilsDouble.radiansToDegrees * FastMath.acos(MathUtils.clamp(this.dot(v) / (this.lenDouble() * v.len()), -1d, 1d));
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
        Vector3Q other = (Vector3Q) obj;
        if (x.hashCode() != other.x.hashCode())
            return false;
        if (y.hashCode() != other.y.hashCode())
            return false;
        return z.hashCode() == other.z.hashCode();
    }

    public Vector3Q setZero() {
        x.assign(0);
        y.assign(0);
        z.assign(0);
        return this;
    }

    public boolean hasNaN() {
        return x.isNaN() || y.isNaN() || z.isNaN();
    }

}
