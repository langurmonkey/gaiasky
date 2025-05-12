/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.utils.NumberUtils;
import net.jafama.FastMath;

import java.io.Serial;
import java.io.Serializable;

public class QuaternionDouble implements Serializable {
    @Serial
    private static final long serialVersionUID = -7661875440774897168L;
    private static final double NORMALIZATION_TOLERANCE = 0.00001;
    private static final QuaternionDouble tmp1 = new QuaternionDouble(0, 0, 0, 0);
    private static final QuaternionDouble tmp2 = new QuaternionDouble(0, 0, 0, 0);

    private static final Matrix4D tmpMat = new Matrix4D();
    private static final Vector3D v3d1 = new Vector3D();

    public double x;
    public double y;
    public double z;
    public double w;

    /**
     * Constructor, sets the four components of the quaternion.
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component
     */
    public QuaternionDouble(double x,
                            double y,
                            double z,
                            double w) {
        this.set(x, y, z, w);
    }

    public QuaternionDouble() {
        idt();
    }

    /**
     * Constructor, sets the quaternion components from the given quaternion.
     *
     * @param quaternion The quaternion to copy.
     */
    public QuaternionDouble(QuaternionDouble quaternion) {
        this.set(quaternion.x, quaternion.y, quaternion.z, quaternion.w);
    }

    /**
     * Constructor, sets the quaternion components from the given quaternion.
     *
     * @param quaternion The quaternion to copy.
     */
    public QuaternionDouble(Quaternion quaternion) {
        this.set(quaternion);
    }

    /**
     * Constructor, sets the quaternion from the given axis vector and the angle around that axis in degrees.
     *
     * @param axis  The axis
     * @param angle The angle in degrees.
     */
    public QuaternionDouble(Vector3D axis,
                            double angle) {
        this.set(axis, angle);
    }

    /**
     * @return the euclidean length of the specified quaternion
     */
    public static double len(final double x,
                             final double y,
                             final double z,
                             final double w) {
        return FastMath.sqrt(x * x + y * y + z * z + w * w);
    }

    public static double len2(final double x,
                              final double y,
                              final double z,
                              final double w) {
        return x * x + y * y + z * z + w * w;
    }

    /**
     * Get the dot product between the two quaternions (commutative).
     *
     * @param x1 the x component of the first quaternion
     * @param y1 the y component of the first quaternion
     * @param z1 the z component of the first quaternion
     * @param w1 the w component of the first quaternion
     * @param x2 the x component of the second quaternion
     * @param y2 the y component of the second quaternion
     * @param z2 the z component of the second quaternion
     * @param w2 the w component of the second quaternion
     *
     * @return the dot product between the first and second quaternion.
     */
    public static double dot(final double x1,
                             final double y1,
                             final double z1,
                             final double w1,
                             final double x2,
                             final double y2,
                             final double z2,
                             final double w2) {
        return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2;
    }

    /**
     * Sets the components of the quaternion
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component
     *
     * @return This quaternion for chaining
     */
    public QuaternionDouble set(double x,
                                double y,
                                double z,
                                double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public QuaternionDouble set(Quadruple x,
                                Quadruple y,
                                Quadruple z,
                                Quadruple w) {
        this.x = x.doubleValue();
        this.y = y.doubleValue();
        this.z = z.doubleValue();
        this.w = w.doubleValue();
        return this;
    }

    /**
     * Sets the quaternion components from the given quaternion.
     *
     * @param quaternion The quaternion.
     *
     * @return This quaternion for chaining.
     */
    public QuaternionDouble set(QuaternionDouble quaternion) {
        return this.set(quaternion.x, quaternion.y, quaternion.z, quaternion.w);
    }

    /**
     * Sets the quaternion components from the given quaternion.
     *
     * @param quaternion The quaternion.
     *
     * @return This quaternion for chaining.
     */
    public QuaternionDouble set(Quaternion quaternion) {
        return this.set(quaternion.x, quaternion.y, quaternion.z, quaternion.w);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis  The axis
     * @param angle The angle in degrees
     *
     * @return This quaternion for chaining.
     */
    public QuaternionDouble set(Vector3D axis,
                                double angle) {
        return setFromAxis(axis.x, axis.y, axis.z, angle);
    }

    /**
     * @return a copy of this quaternion
     */
    public QuaternionDouble cpy() {
        return new QuaternionDouble(this);
    }

    /**
     * @return the Euclidean length of this quaternion
     */
    public double len() {
        return FastMath.sqrt(x * x + y * y + z * z + w * w);
    }

    @Override
    public String toString() {
        return "[" + x + "|" + y + "|" + z + "|" + w + "]";
    }

    /**
     * Sets the quaternion to the given euler angles in degrees.
     *
     * @param yaw   the rotation around the y-axis in degrees
     * @param pitch the rotation around the x-axis in degrees
     * @param roll  the rotation around the z-axis degrees
     *
     * @return this quaternion
     */
    public QuaternionDouble setEulerAngles(double yaw,
                                           double pitch,
                                           double roll) {
        return setEulerAnglesRad(yaw * MathUtilsDouble.degreesToRadians, pitch * MathUtilsDouble.degreesToRadians, roll * MathUtilsDouble.degreesToRadians);
    }

    /**
     * Sets the quaternion to the given euler angles in radians.
     *
     * @param yaw   the rotation around the Y axis in radians
     * @param pitch the rotation around the X axis in radians
     * @param roll  the rotation around the Z axis in radians
     *
     * @return this quaternion
     */
    public QuaternionDouble setEulerAnglesRad(double yaw,
                                              double pitch,
                                              double roll) {
        final double hr = roll * 0.5f;
        final double shr = FastMath.sin(hr);
        final double chr = FastMath.cos(hr);
        final double hp = pitch * 0.5f;
        final double shp = FastMath.sin(hp);
        final double chp = FastMath.cos(hp);
        final double hy = yaw * 0.5f;
        final double shy = FastMath.sin(hy);
        final double chy = FastMath.cos(hy);
        final double chy_shp = chy * shp;
        final double shy_chp = shy * chp;
        final double chy_chp = chy * chp;
        final double shy_shp = shy * shp;

        x = (chy_shp * chr) + (shy_chp * shr); // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
        y = (shy_chp * chr) - (chy_shp * shr); // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
        z = (chy_chp * shr) - (shy_shp * chr); // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
        w = (chy_chp * chr) + (shy_shp * shr); // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
        return this;
    }

    /**
     * Get the pole of the gimbal lock, if any.
     *
     * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock
     */
    public int getGimbalPole() {
        final double t = y * x + z * w;
        return t > 0.499f ? 1 : (t < -0.499f ? -1 : 0);
    }

    /**
     * Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is
     * normalized.
     *
     * @return the rotation around the z axis in radians (between -PI and +PI)
     */
    public double getRollRad() {
        final int pole = getGimbalPole();
        return pole == 0 ? FastMath.atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z)) : pole * 2f * FastMath.atan2(y, w);
    }

    /**
     * Get the roll euler angle in degrees, which is the rotation around the z axis. Requires that this quaternion is
     * normalized.
     *
     * @return the rotation around the z axis in degrees (between -180 and +180)
     */
    public double getRoll() {
        return getRollRad() * MathUtilsDouble.radiansToDegrees;
    }

    /**
     * Get the pitch euler angle in radians, which is the rotation around the x axis. Requires that this quaternion is
     * normalized.
     *
     * @return the rotation around the x axis in radians (between -(PI/2) and +(PI/2))
     */
    public double getPitchRad() {
        final int pole = getGimbalPole();
        return pole == 0 ? FastMath.asin(2f * (w * x - z * y)) : pole * MathUtilsDouble.PI * 0.5f;
    }

    /**
     * Get the pitch euler angle in degrees, which is the rotation around the x axis. Requires that this quaternion is
     * normalized.
     *
     * @return the rotation around the x axis in degrees (between -90 and +90)
     */
    public double getPitch() {
        return getPitchRad() * MathUtilsDouble.radiansToDegrees;
    }

    /**
     * Get the yaw euler angle in radians, which is the rotation around the y axis. Requires that this quaternion is
     * normalized.
     *
     * @return the rotation around the y axis in radians (between -PI and +PI)
     */
    public double getYawRad() {
        return getGimbalPole() == 0 ? FastMath.atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x)) : 0f;
    }

    /**
     * Get the yaw euler angle in degrees, which is the rotation around the y axis. Requires that this quaternion is
     * normalized.
     *
     * @return the rotation around the y axis in degrees (between -180 and +180)
     */
    public double getYaw() {
        return getYawRad() * MathUtilsDouble.radiansToDegrees;
    }

    /**
     * @return the length of this quaternion without square root
     */
    public double len2() {
        return x * x + y * y + z * z + w * w;
    }

    /**
     * Normalizes this quaternion to unit length.
     *
     * @return the quaternion for chaining.
     */
    public QuaternionDouble nor() {
        double len = len2();
        if (len != 0.f && (Math.abs(len - 1.0f) > NORMALIZATION_TOLERANCE)) {
            len = FastMath.sqrt(len);
            w /= len;
            x /= len;
            y /= len;
            z /= len;
        }
        return this;
    }

    /**
     * Conjugate the quaternion.
     *
     * @return This quaternion for chaining
     */
    public QuaternionDouble conjugate() {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    /**
     * Transforms the given vector using this quaternion
     *
     * @param v Vector to transform
     */
    public Vector3D transform(Vector3D v) {
        tmp2.set(this);
        tmp2.conjugate();
        tmp2.mulLeft(tmp1.set(v.x, v.y, v.z, 0)).mulLeft(this);

        v.x = tmp2.x;
        v.y = tmp2.y;
        v.z = tmp2.z;
        return v;
    }

    public Vector3b transform(Vector3b v) {
        tmp2.set(this);
        tmp2.conjugate();
        tmp2.mulLeft(tmp1.set(v.x.doubleValue(), v.y.doubleValue(), v.z.doubleValue(), 0.0)).mulLeft(this);

        v.set(tmp2.x, tmp2.y, tmp2.z);
        return v;
    }

    /**
     * Sets the value of this quaternion to the quaternion inverse of itself.
     * Warning: this quaternion will NOT be normalized. Note that if q not
     * normalized, then q*qinverse=(0,0,0,1) whereas q*qconjugate
     * =(0,0,0,Norm(q)*Norm(q))
     *
     * @return Quaternion
     */
    public QuaternionDouble inverse() {
        double invNormSqu;

        invNormSqu = 1.0 / ((this.w * this.w) + (this.x * this.x) + (this.y * this.y) + (this.z * this.z));
        this.w *= invNormSqu;
        this.x *= -invNormSqu;
        this.y *= -invNormSqu;
        this.z *= -invNormSqu;

        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = this * other
     *
     * @param other QuaternionDouble to multiply with
     *
     * @return This quaternion for chaining
     */
    public QuaternionDouble mul(final QuaternionDouble other) {
        final double newX = this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y;
        final double newY = this.w * other.y + this.y * other.w + this.z * other.x - this.x * other.z;
        final double newZ = this.w * other.z + this.z * other.w + this.x * other.y - this.y * other.x;
        final double newW = this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = this * other
     *
     * @param x the x component of the other quaternion to multiply with
     * @param y the y component of the other quaternion to multiply with
     * @param z the z component of the other quaternion to multiply with
     * @param w the w component of the other quaternion to multiply with
     *
     * @return This quaternion for chaining
     */
    public QuaternionDouble mul(final double x,
                                final double y,
                                final double z,
                                final double w) {
        final double newX = this.w * x + this.x * w + this.y * z - this.z * y;
        final double newY = this.w * y + this.y * w + this.z * x - this.x * z;
        final double newZ = this.w * z + this.z * w + this.x * y - this.y * x;
        final double newW = this.w * w - this.x * x - this.y * y - this.z * z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = other * this
     *
     * @param other QuaternionDouble to multiply with
     *
     * @return This quaternion for chaining
     */
    public QuaternionDouble mulLeft(QuaternionDouble other) {
        final double newX = other.w * this.x + other.x * this.w + other.y * this.z - other.z * y;
        final double newY = other.w * this.y + other.y * this.w + other.z * this.x - other.x * z;
        final double newZ = other.w * this.z + other.z * this.w + other.x * this.y - other.y * x;
        final double newW = other.w * this.w - other.x * this.x - other.y * this.y - other.z * z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion with another one in the form of this = other * this
     *
     * @param x the x component of the other quaternion to multiply with
     * @param y the y component of the other quaternion to multiply with
     * @param z the z component of the other quaternion to multiply with
     * @param w the w component of the other quaternion to multiply with
     *
     * @return This quaternion for chaining
     */
    public QuaternionDouble mulLeft(final double x,
                                    final double y,
                                    final double z,
                                    final double w) {
        final double newX = w * this.x + x * this.w + y * this.z - z * y;
        final double newY = w * this.y + y * this.w + z * this.x - x * z;
        final double newZ = w * this.z + z * this.w + x * this.y - y * x;
        final double newW = w * this.w - x * this.x - y * this.y - z * z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /**
     * Multiplies this quaternion by the inverse of quaternion q1 and places the
     * value into this quaternion. The value of the argument quaternion is
     * preserved (this = this * q^-1). Warning: this quaternion will NOT be
     * normalized.
     *
     * @param q1 the other quaternion
     *
     * @return QuaternionDouble
     */
    public QuaternionDouble mulInverse(final QuaternionDouble q1) {
        final QuaternionDouble tempQuat = new QuaternionDouble(q1);

        tempQuat.inverse();
        this.mul(tempQuat);

        return this;
    }

    // TODO : the matrix4 set(quaternion) doesnt set the last row+col of the matrix to 0,0,0,1 so... that's why there is this
    // method

    /**
     * Pre-multiplies this quaternion by the inverse of quaternion q and places
     * the value into this quaternion. The value of the argument quaternion is
     * preserved (this = q^-1 * this). Warning: this quaternion will NOT be
     * normalized.
     *
     * @param q1 the other quaternion
     *
     * @return QuaternionDouble
     */
    public QuaternionDouble mulLeftInverse(final QuaternionDouble q1) {
        final QuaternionDouble tempQuat = new QuaternionDouble(q1);

        tempQuat.inverse();
        this.mulLeft(tempQuat);
        return this;
    }

    /**
     * Fills a 4x4 matrix with the rotation matrix represented by this quaternion.
     *
     * @param matrix Matrix to fill
     */
    public void toMatrix(final double[] matrix) {
        final double xx = x * x;
        final double xy = x * y;
        final double xz = x * z;
        final double xw = x * w;
        final double yy = y * y;
        final double yz = y * z;
        final double yw = y * w;
        final double zz = z * z;
        final double zw = z * w;
        // Set matrix from quaternion
        matrix[Matrix4D.M00] = 1 - 2 * (yy + zz);
        matrix[Matrix4D.M01] = 2 * (xy - zw);
        matrix[Matrix4D.M02] = 2 * (xz + yw);
        matrix[Matrix4D.M03] = 0;
        matrix[Matrix4D.M10] = 2 * (xy + zw);
        matrix[Matrix4D.M11] = 1 - 2 * (xx + zz);
        matrix[Matrix4D.M12] = 2 * (yz - xw);
        matrix[Matrix4D.M13] = 0;
        matrix[Matrix4D.M20] = 2 * (xz - yw);
        matrix[Matrix4D.M21] = 2 * (yz + xw);
        matrix[Matrix4D.M22] = 1 - 2 * (xx + yy);
        matrix[Matrix4D.M23] = 0;
        matrix[Matrix4D.M30] = 0;
        matrix[Matrix4D.M31] = 0;
        matrix[Matrix4D.M32] = 0;
        matrix[Matrix4D.M33] = 1;
    }

    /**
     * Sets the quaternion to an identity QuaternionDouble
     *
     * @return this quaternion for chaining
     */
    public QuaternionDouble idt() {
        return this.set(0, 0, 0, 1);
    }

    /**
     * @return If this quaternion is an identity QuaternionDouble
     */
    public boolean isIdentity() {
        return MathUtilsDouble.isZero(x) && MathUtilsDouble.isZero(y) && MathUtilsDouble.isZero(z) && MathUtilsDouble.isEqual(w, 1f);
    }

    // todo : the setFromAxis(v3,double) method should replace the set(v3,double) method

    /**
     * @return If this quaternion is an identity QuaternionDouble
     */
    public boolean isIdentity(final double tolerance) {
        return MathUtilsDouble.isZero(x, tolerance) && MathUtilsDouble.isZero(y, tolerance) && MathUtilsDouble.isZero(z, tolerance) && MathUtilsDouble.isEqual(w, 1f,
                                                                                                                                                               tolerance);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis    The axis
     * @param degrees The angle in degrees
     *
     * @return This quaternion for chaining.
     */
    public QuaternionDouble setFromAxis(final Vector3D axis,
                                        final double degrees) {
        return setFromAxis(axis.x, axis.y, axis.z, degrees);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis    The axis
     * @param radians The angle in radians
     *
     * @return This quaternion for chaining.
     */
    public QuaternionDouble setFromAxisRad(final Vector3D axis,
                                           final double radians) {
        return setFromAxisRad(axis.x, axis.y, axis.z, radians);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param x       X direction of the axis
     * @param y       Y direction of the axis
     * @param z       Z direction of the axis
     * @param degrees The angle in degrees
     *
     * @return This quaternion for chaining.
     */
    public QuaternionDouble setFromAxis(final double x,
                                        final double y,
                                        final double z,
                                        final double degrees) {
        return setFromAxisRad(x, y, z, degrees * MathUtilsDouble.degreesToRadians);
    }

    /**
     * Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param x       X direction of the axis
     * @param y       Y direction of the axis
     * @param z       Z direction of the axis
     * @param radians The angle in radians
     *
     * @return This quaternion for chaining.
     */
    public QuaternionDouble setFromAxisRad(final double x,
                                           final double y,
                                           final double z,
                                           final double radians) {
        double d = Vector3D.len(x, y, z);
        if (d == 0f)
            return idt();
        d = 1f / d;
        double l_ang = radians;
        double l_sin = FastMath.sin(l_ang / 2);
        double l_cos = FastMath.cos(l_ang / 2);
        return this.set(d * x * l_sin, d * y * l_sin, d * z * l_sin, l_cos).nor();
    }

    /**
     * Sets the QuaternionDouble from the given matrix, optionally removing any scaling.
     */
    public QuaternionDouble setFromMatrix(boolean normalizeAxes,
                                          Matrix4D matrix) {
        return setFromAxes(normalizeAxes,
                           matrix.val[Matrix4D.M00], matrix.val[Matrix4D.M01], matrix.val[Matrix4D.M02],
                           matrix.val[Matrix4D.M10], matrix.val[Matrix4D.M11], matrix.val[Matrix4D.M12],
                           matrix.val[Matrix4D.M20], matrix.val[Matrix4D.M21], matrix.val[Matrix4D.M22]);
    }

    /**
     * Sets the QuaternionDouble from the given rotation matrix, which must not contain scaling.
     */
    public QuaternionDouble setFromMatrix(Matrix4D matrix) {
        return setFromMatrix(false, matrix);
    }

    /**
     * <p>
     * Sets the QuaternionDouble from the given x-, y- and z-axis which have to be orthonormal.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see <a href="http://www.aptalkarga.com/bones/">this site</a>, which in turn took it from Graphics Gem.
     * code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     *
     * @param xx x-axis x-coordinate
     * @param xy x-axis y-coordinate
     * @param xz x-axis z-coordinate
     * @param yx y-axis x-coordinate
     * @param yy y-axis y-coordinate
     * @param yz y-axis z-coordinate
     * @param zx z-axis x-coordinate
     * @param zy z-axis y-coordinate
     * @param zz z-axis z-coordinate
     */
    public QuaternionDouble setFromAxes(double xx,
                                        double xy,
                                        double xz,
                                        double yx,
                                        double yy,
                                        double yz,
                                        double zx,
                                        double zy,
                                        double zz) {
        return setFromAxes(false, xx, xy, xz, yx, yy, yz, zx, zy, zz);
    }

    /**
     * Sets this quaternion from the given camera basis, using its direction and up vectors.
     *
     * @param direction The camera direction vector.
     * @param up        The camera up vector.
     *
     * @return The quaternion representing the current camera orientation.
     */
    public QuaternionDouble setFromCamera(Vector3D direction,
                                          Vector3D up) {
        var side = v3d1.set(direction).crs(up).nor();
        return fromAxes(direction, up, side);
    }

    /**
     * Gets the camera direction vector corresponding to this quaternion rotation.
     *
     * @param aux The vector to put the result.
     *
     * @return The direction vector.
     */
    public Vector3D getDirection(Vector3D aux) {
        return aux.set(1, 0, 0).mul(this);
    }

    /**
     * Gets the camera up vector corresponding to this quaternion rotation.
     *
     * @param aux The vector to put the result.
     *
     * @return The up vector.
     */
    public Vector3D getUp(Vector3D aux) {
        return aux.set(0, 1, 0).mul(this);
    }

    /**
     * <code>fromAxes</code> creates a <code>Quaternion</code> that
     * represents the coordinate system defined by three axes. These axes are
     * assumed to be orthogonal and no error checking is applied. Thus, the user
     * must ensure that the three axes being provided indeed represents a proper
     * right-handed coordinate system.
     *
     * @param xAxis vector representing the x-axis of the coordinate system.
     * @param yAxis vector representing the y-axis of the coordinate system.
     * @param zAxis vector representing the z-axis of the coordinate system.
     */
    public QuaternionDouble fromAxes(Vector3D xAxis,
                                     Vector3D yAxis,
                                     Vector3D zAxis) {
        return fromRotationMatrix(xAxis.x, yAxis.x, zAxis.x, xAxis.y, yAxis.y,
                                  zAxis.y, xAxis.z, yAxis.z, zAxis.z);
    }

    public QuaternionDouble fromRotationMatrix(double m00,
                                               double m01,
                                               double m02,
                                               double m10,
                                               double m11,
                                               double m12,
                                               double m20,
                                               double m21,
                                               double m22) {
        // Use the Graphics Gems code, from
        // ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z
        // *NOT* the "Matrix and Quaternions FAQ", which has errors!

        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        double t = m00 + m11 + m22;

        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            double s = FastMath.sqrt(t + 1); // |s|>=1 ...
            w = 0.5f * s;
            s = 0.5f / s;                 // so this division isn't bad
            x = (m21 - m12) * s;
            y = (m02 - m20) * s;
            z = (m10 - m01) * s;
        } else if ((m00 > m11) && (m00 > m22)) {
            double s = FastMath.sqrt(1.0f + m00 - m11 - m22); // |s|>=1
            x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            y = (m10 + m01) * s;
            z = (m02 + m20) * s;
            w = (m21 - m12) * s;
        } else if (m11 > m22) {
            double s = FastMath.sqrt(1.0f + m11 - m00 - m22); // |s|>=1
            y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            x = (m10 + m01) * s;
            z = (m21 + m12) * s;
            w = (m02 - m20) * s;
        } else {
            double s = FastMath.sqrt(1.0f + m22 - m00 - m11); // |s|>=1
            z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            x = (m02 + m20) * s;
            y = (m21 + m12) * s;
            w = (m10 - m01) * s;
        }

        return this;
    }

    /**
     * <p>
     * Sets the QuaternionDouble from the given x-, y- and z-axis.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see <a href="http://www.aptalkarga.com/bones/">this site</a>, which in turn took it from Graphics Gem.
     * Code at ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     *
     * @param normalizeAxes whether to normalize the axes (necessary when they contain scaling)
     * @param xx            x-axis x-coordinate
     * @param xy            x-axis y-coordinate
     * @param xz            x-axis z-coordinate
     * @param yx            y-axis x-coordinate
     * @param yy            y-axis y-coordinate
     * @param yz            y-axis z-coordinate
     * @param zx            z-axis x-coordinate
     * @param zy            z-axis y-coordinate
     * @param zz            z-axis z-coordinate
     */
    public QuaternionDouble setFromAxes(boolean normalizeAxes,
                                        double xx,
                                        double xy,
                                        double xz,
                                        double yx,
                                        double yy,
                                        double yz,
                                        double zx,
                                        double zy,
                                        double zz) {
        if (normalizeAxes) {
            final double lx = 1f / Vector3D.len(xx, xy, xz);
            final double ly = 1f / Vector3D.len(yx, yy, yz);
            final double lz = 1f / Vector3D.len(zx, zy, zz);
            xx *= lx;
            xy *= lx;
            xz *= lx;
            yz *= ly;
            yy *= ly;
            yz *= ly;
            zx *= lz;
            zy *= lz;
            zz *= lz;
        }
        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        final double t = xx + yy + zz;

        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            double s = FastMath.sqrt(t + 1); // |s|>=1 ...
            w = 0.5f * s;
            s = 0.5f / s; // so this division isn't bad
            x = (zy - yz) * s;
            y = (xz - zx) * s;
            z = (yx - xy) * s;
        } else if ((xx > yy) && (xx > zz)) {
            double s = FastMath.sqrt(1.0 + xx - yy - zz); // |s|>=1
            x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            y = (yx + xy) * s;
            z = (xz + zx) * s;
            w = (zy - yz) * s;
        } else if (yy > zz) {
            double s = FastMath.sqrt(1.0 + yy - xx - zz); // |s|>=1
            y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            x = (yx + xy) * s;
            z = (zy + yz) * s;
            w = (xz - zx) * s;
        } else {
            double s = FastMath.sqrt(1.0 + zz - xx - yy); // |s|>=1
            z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            x = (xz + zx) * s;
            y = (zy + yz) * s;
            w = (yx - xy) * s;
        }

        return this;
    }

    /**
     * Set this quaternion to the rotation between two vectors.
     *
     * @param v1 The base vector, which should be normalized.
     * @param v2 The target vector, which should be normalized.
     *
     * @return This quaternion for chaining
     */
    public QuaternionDouble setFromCross(final Vector3D v1,
                                         final Vector3D v2) {
        final double dot = MathUtilsDouble.clamp(v1.dot(v2), -1f, 1f);
        final double angle = FastMath.acos(dot);
        return setFromAxisRad(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x, angle);
    }

    /**
     * Set this quaternion to the rotation between two vectors.
     *
     * @param x1 The base vectors x value, which should be normalized.
     * @param y1 The base vectors y value, which should be normalized.
     * @param z1 The base vectors z value, which should be normalized.
     * @param x2 The target vector x value, which should be normalized.
     * @param y2 The target vector y value, which should be normalized.
     * @param z2 The target vector z value, which should be normalized.
     *
     * @return This quaternion for chaining
     */
    public QuaternionDouble setFromCross(final double x1,
                                         final double y1,
                                         final double z1,
                                         final double x2,
                                         final double y2,
                                         final double z2) {
        final double dot = MathUtilsDouble.clamp(Vector3D.dot(x1, y1, z1, x2, y2, z2), -1f, 1f);
        final double angle = FastMath.acos(dot);
        return setFromAxisRad(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2, x1 * y2 - y1 * x2, angle);
    }

    /**
     * Normalized linear interpolation between the current instance and {@code q2} using
     * nlerp, and stores the result in the current instance.
     *
     * <p>This method is often faster than
     * {@link QuaternionDouble#slerp(QuaternionDouble, double)}, but less accurate.
     *
     * @param q2    the desired value when blend=1 (not null, unaffected).
     * @param blend the fractional change amount.
     */
    public void nlerp(QuaternionDouble q2,
                      double blend) {
        double dot = dot(q2);
        double blendI = 1.0 - blend;
        if (dot < 0.0) {
            x = blendI * x - blend * q2.x;
            y = blendI * y - blend * q2.y;
            z = blendI * z - blend * q2.z;
            w = blendI * w - blend * q2.w;
        } else {
            x = blendI * x + blend * q2.x;
            y = blendI * y + blend * q2.y;
            z = blendI * z + blend * q2.z;
            w = blendI * w + blend * q2.w;
        }
        nor();
    }

    /**
     * Spherical linear interpolation between this quaternion and the {@code end} quaternion, based on the alpha value
     * in the range [0,1]. Taken from. Taken from Bones framework for JPCT,
     * see <a href="http://www.aptalkarga.com/bones/">here</a>.
     *
     * @param end   the end quaternion.
     * @param alpha alpha in the range [0,1].
     */
    public void slerp(QuaternionDouble end,
                      double alpha) {
        final double dot = dot(end);
        double absDot = dot < 0.f ? -dot : dot;

        // Set the first and second scale for the interpolation
        double scale0 = 1 - alpha;
        double scale1 = alpha;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - absDot) > 0.1) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final double angle = FastMath.acos(absDot);
            final double invSinTheta = 1f / FastMath.sin(angle);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = (Math.sin((1 - alpha) * angle) * invSinTheta);
            scale1 = (Math.sin((alpha * angle)) * invSinTheta);
        }

        if (dot < 0.f)
            scale1 = -scale1;

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        x = (scale0 * x) + (scale1 * end.x);
        y = (scale0 * y) + (scale1 * end.y);
        z = (scale0 * z) + (scale1 * end.z);
        w = (scale0 * w) + (scale1 * end.w);
    }

    /**
     * Second implementation of slerp, slightly different from the default one.
     *
     * @param end   The end quaternion.
     * @param alpha The fractional change amount in [0,1].
     */
    public void slerp2(QuaternionDouble end,
                       double alpha) {
        if (x == end.x && y == end.y && z == end.z && w == end.w) {
            return;
        }

        double result = (x * end.x) + (y * end.y) + (z * end.z) + (w * end.w);

        if (result < 0.0) {
            // Negate the second quaternion and the result of the dot product
            end.x = -end.x;
            end.y = -end.y;
            end.z = -end.z;
            end.w = -end.w;
            result = -result;
        }

        // Set the first and second scale for the interpolation
        double scale0 = 1.0 - alpha;
        double scale1 = alpha;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - result) > 0.1) {
            // Get the angle between the 2 quaternions, and then store the sin()
            // of that angle
            double theta = FastMath.acos(result);
            double invSinTheta = 1.0 / FastMath.sin(theta);

            // Calculate the scale for q1 and q2, according to the angle and
            // its sine
            scale0 = FastMath.sin((1 - alpha) * theta) * invSinTheta;
            scale1 = FastMath.sin((alpha * theta)) * invSinTheta;
        }

        // Calculate the x, y, z and w values for the quaternion by using a
        // special
        // form of linear interpolation for quaternions.
        x = (scale0 * x) + (scale1 * end.x);
        y = (scale0 * y) + (scale1 * end.y);
        z = (scale0 * z) + (scale1 * end.z);
        w = (scale0 * w) + (scale1 * end.w);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + NumberUtils.floatToRawIntBits((float) w);
        result = prime * result + NumberUtils.floatToRawIntBits((float) x);
        result = prime * result + NumberUtils.floatToRawIntBits((float) y);
        result = prime * result + NumberUtils.floatToRawIntBits((float) z);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof QuaternionDouble other)) {
            return false;
        }
        return (NumberUtils.floatToRawIntBits((float) w) == NumberUtils.floatToRawIntBits((float) other.w)) && (NumberUtils.floatToRawIntBits((float) x)
                == NumberUtils.floatToRawIntBits((float) other.x)) && (NumberUtils.floatToRawIntBits((float) y) == NumberUtils.floatToRawIntBits((float) other.y)) && (
                NumberUtils.floatToRawIntBits((float) z) == NumberUtils.floatToRawIntBits((float) other.z));
    }

    /**
     * Get the dot product between this and the other quaternion (commutative).
     *
     * @param other the other quaternion.
     *
     * @return the dot product of this and the other quaternion.
     */
    public double dot(final QuaternionDouble other) {
        return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
    }

    /**
     * Get the dot product between this and the other quaternion (commutative).
     *
     * @param x the x component of the other quaternion
     * @param y the y component of the other quaternion
     * @param z the z component of the other quaternion
     * @param w the w component of the other quaternion
     *
     * @return the dot product of this and the other quaternion.
     */
    public double dot(final double x,
                      final double y,
                      final double z,
                      final double w) {
        return this.x * x + this.y * y + this.z * z + this.w * w;
    }

    /**
     * Multiplies the components of this quaternion with the given scalar.
     *
     * @param scalar the scalar.
     *
     * @return this quaternion for chaining.
     */
    public QuaternionDouble mul(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.w *= scalar;
        return this;
    }

    /**
     * Add quaternion q, scaled by s, to this quaternion (this = this + q * s)
     *
     * @return this quaternion for chaining.
     */
    public QuaternionDouble mulAdd(QuaternionDouble q,
                                   double s) {
        this.x += q.x * s;
        this.y += q.y * s;
        this.z += q.z * s;
        this.w += q.w * s;
        return this;
    }

    /**
     * Get the axis angle representation of the rotation in degrees. The supplied vector will receive the axis (x, y and
     * z values)
     * of the rotation and the value returned is the angle in degrees around that axis. Note that this method will alter
     * the
     * supplied vector, the existing value of the vector is ignored. This will normalize this quaternion if needed. The
     * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the
     * axis may be
     * zero.
     *
     * @param axis vector which will receive the axis
     *
     * @return the angle in degrees
     *
     * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a>
     */
    public double getAxisAngle(Vector3D axis) {
        return getAxisAngleRad(axis) * MathUtilsDouble.radiansToDegrees;
    }

    /**
     * Get the axis-angle representation of the rotation in radians. The supplied vector will receive the axis (x, y and
     * z values)
     * of the rotation and the value returned is the angle in radians around that axis. Note that this method will alter
     * the
     * supplied vector, the existing value of the vector is ignored. This will normalize this quaternion if needed.
     * The received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the
     * axis may be zero.
     *
     * @param axis vector which will receive the axis
     *
     * @return the angle in radians
     *
     * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a>
     */
    public double getAxisAngleRad(Vector3D axis) {
        if (this.w > 1)
            this.nor(); // if w>1 acos and sqrt will produce errors, this can't happen if quaternion is normalised
        double angle = (2.0 * FastMath.acos(this.w));
        double s = FastMath.sqrt(1 - this.w * this.w); // assuming quaternion normalised then w is less than 1, so term always positive.
        if (s < NORMALIZATION_TOLERANCE) { // test to avoid divide by zero, s is always positive due to sqrt
            // if s close to zero then direction of axis not important
            axis.x = this.x; // if it is important that axis is normalised then replace with x=1; y=z=0;
            axis.y = this.y;
            axis.z = this.z;
        } else {
            axis.x = (this.x / s); // normalise axis
            axis.y = (this.y / s);
            axis.z = (this.z / s);
        }

        return angle;
    }

    /**
     * Get the angle in radians of the rotation this quaternion represents. Does not normalize the quaternion. Use
     * {@link #getAxisAngleRad(Vector3D)} to get both the axis and the angle of this rotation. Use
     * {@link #getAngleAroundRad(Vector3D)} to get the angle around a specific axis.
     *
     * @return the angle in radians of the rotation
     */
    public double getAngleRad() {
        return (2.0 * FastMath.acos((this.w > 1) ? (this.w / len()) : this.w));
    }

    /**
     * Get the angle in degrees of the rotation this quaternion represents. Use {@link #getAxisAngle(Vector3D)} to get
     * both the axis
     * and the angle of this rotation. Use {@link #getAngleAround(Vector3D)} to get the angle around a specific axis.
     *
     * @return the angle in degrees of the rotation
     */
    public double getAngle() {
        return getAngleRad() * MathUtilsDouble.radiansToDegrees;
    }

    /**
     * <p>
     * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation
     * around the
     * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation
     * around an
     * axis perpendicular to the specified axis.
     * </p>
     * <p>
     * The swing and twist rotation can be used to reconstruct the original quaternion: this = swing * twist
     * </p>
     *
     * @param axisX the X component of the normalized axis for which to get the swing and twist rotation
     * @param axisY the Y component of the normalized axis for which to get the swing and twist rotation
     * @param axisZ the Z component of the normalized axis for which to get the swing and twist rotation
     * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
     * @param twist will receive the twist rotation: the rotation around the specified axis
     *
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
     */
    public void getSwingTwist(final double axisX,
                              final double axisY,
                              final double axisZ,
                              final QuaternionDouble swing,
                              final QuaternionDouble twist) {
        final double d = Vector3D.dot(this.x, this.y, this.z, axisX, axisY, axisZ);
        twist.set(axisX * d, axisY * d, axisZ * d, this.w).nor();
        swing.set(twist).conjugate().mulLeft(this);
    }

    /**
     * <p>
     * Get the swing rotation and twist rotation for the specified axis. The twist rotation represents the rotation
     * around the
     * specified axis. The swing rotation represents the rotation of the specified axis itself, which is the rotation
     * around an
     * axis perpendicular to the specified axis.
     * </p>
     * <p>
     * The swing and twist rotation can be used to reconstruct the original quaternion: this = swing * twist
     * </p>
     *
     * @param axis  the normalized axis for which to get the swing and twist rotation
     * @param swing will receive the swing rotation: the rotation around an axis perpendicular to the specified axis
     * @param twist will receive the twist rotation: the rotation around the specified axis
     *
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/for/decomposition">calculation</a>
     */
    public void getSwingTwist(final Vector3D axis,
                              final QuaternionDouble swing,
                              final QuaternionDouble twist) {
        getSwingTwist(axis.x, axis.y, axis.z, swing, twist);
    }

    /**
     * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
     *
     * @param axisX the x component of the normalized axis for which to get the angle
     * @param axisY the y component of the normalized axis for which to get the angle
     * @param axisZ the z component of the normalized axis for which to get the angle
     *
     * @return the angle in radians of the rotation around the specified axis
     */
    public double getAngleAroundRad(final double axisX,
                                    final double axisY,
                                    final double axisZ) {
        final double d = Vector3D.dot(this.x, this.y, this.z, axisX, axisY, axisZ);
        final double l2 = QuaternionDouble.len2(axisX * d, axisY * d, axisZ * d, this.w);
        return l2 == 0f ? 0f : (2.0 * FastMath.acos(this.w / FastMath.sqrt(l2)));
    }

    /**
     * Get the angle in radians of the rotation around the specified axis. The axis must be normalized.
     *
     * @param axis the normalized axis for which to get the angle
     *
     * @return the angle in radians of the rotation around the specified axis
     */
    public double getAngleAroundRad(final Vector3D axis) {
        return getAngleAroundRad(axis.x, axis.y, axis.z);
    }

    /**
     * Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
     *
     * @param axisX the x component of the normalized axis for which to get the angle
     * @param axisY the y component of the normalized axis for which to get the angle
     * @param axisZ the z component of the normalized axis for which to get the angle
     *
     * @return the angle in degrees of the rotation around the specified axis
     */
    public double getAngleAround(final double axisX,
                                 final double axisY,
                                 final double axisZ) {
        return getAngleAroundRad(axisX, axisY, axisZ) * MathUtilsDouble.radiansToDegrees;
    }

    /**
     * Get the angle in degrees of the rotation around the specified axis. The axis must be normalized.
     *
     * @param axis the normalized axis for which to get the angle
     *
     * @return the angle in degrees of the rotation around the specified axis
     */
    public double getAngleAround(final Vector3D axis) {
        return getAngleAround(axis.x, axis.y, axis.z);
    }

    /**
     * Returns the components of this quaternion as an array of [x, y, z, w].
     * @return The components of this quaternion as an array.
     */
    public double[] values() {
        return new double[] { x, y, z, w };
    }
}
