/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import net.jafama.FastMath;

public class Vector2D implements VectorDouble<Vector2D> {
    public final static Vector2D X = new Vector2D(1, 0);
    public final static Vector2D Y = new Vector2D(0, 1);
    public final static Vector2D Zero = new Vector2D(0, 0);
    /** the x-component of this vector **/
    public double x;
    /** the y-component of this vector **/
    public double y;

    /** Constructs a new vector at (0,0) */
    public Vector2D() {
    }

    /**
     * Constructs a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     */
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constructs a vector from the given vector
     *
     * @param v The vector
     */
    public Vector2D(Vector2D v) {
        set(v);
    }

    public static double len(double x, double y) {
        return FastMath.sqrt(x * x + y * y);
    }

    public static double len2(double x, double y) {
        return x * x + y * y;
    }

    public static double dot(double x1, double y1, double x2, double y2) {
        return x1 * x2 + y1 * y2;
    }

    public static double dst(double x1, double y1, double x2, double y2) {
        final double x_d = x2 - x1;
        final double y_d = y2 - y1;
        return FastMath.sqrt(x_d * x_d + y_d * y_d);
    }

    public static double dst2(double x1, double y1, double x2, double y2) {
        final double x_d = x2 - x1;
        final double y_d = y2 - y1;
        return x_d * x_d + y_d * y_d;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public Vector2D cpy() {
        return new Vector2D(this);
    }

    public double len() {
        return FastMath.sqrt(x * x + y * y);
    }

    public double len2() {
        return x * x + y * y;
    }

    public Vector2D set(Vector2D v) {
        x = v.x;
        y = v.y;
        return this;
    }

    public Vector2D set(double[] vals) {
        return this.set(vals[0], vals[1]);
    }

    /**
     * Sets the components of this vector
     *
     * @param x The x-component
     * @param y The y-component
     *
     * @return This vector for chaining
     */
    public Vector2D set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2D sub(Vector2D v) {
        x -= v.x;
        y -= v.y;
        return this;
    }

    /**
     * Substracts the other vector from this vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     *
     * @return This vector for chaining
     */
    public Vector2D sub(double x, double y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vector2D nor() {
        double len = len();
        if (len != 0) {
            x /= len;
            y /= len;
        }
        return this;
    }

    public Vector2D add(Vector2D v) {
        x += v.x;
        y += v.y;
        return this;
    }

    /**
     * Adds the given components to this vector
     *
     * @param x The x-component
     * @param y The y-component
     *
     * @return This vector for chaining
     */
    public Vector2D add(double x, double y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public double dot(Vector2D v) {
        return x * v.x + y * v.y;
    }

    public double dot(double ox, double oy) {
        return x * ox + y * oy;
    }

    public Vector2D scl(double scalar) {
        x *= scalar;
        y *= scalar;
        return this;
    }

    /**
     * Multiplies this vector by a scalar
     *
     * @return This vector for chaining
     */
    public Vector2D scl(double x, double y) {
        this.x *= x;
        this.y *= y;
        return this;
    }

    public Vector2D scl(Vector2D v) {
        this.x *= v.x;
        this.y *= v.y;
        return this;
    }

    public Vector2D mulAdd(Vector2D vec, double scalar) {
        this.x += vec.x * scalar;
        this.y += vec.y * scalar;
        return this;
    }

    public Vector2D mulAdd(Vector2D vec, Vector2D mulVec) {
        this.x += vec.x * mulVec.x;
        this.y += vec.y * mulVec.y;
        return this;
    }

    @Override
    public Vector2D setZero() {
        return null;
    }

    public double dst(Vector2D v) {
        final double x_d = v.x - x;
        final double y_d = v.y - y;
        return FastMath.sqrt(x_d * x_d + y_d * y_d);
    }

    /**
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     *
     * @return the distance between this and the other vector
     */
    public double dst(double x, double y) {
        final double x_d = x - this.x;
        final double y_d = y - this.y;
        return FastMath.sqrt(x_d * x_d + y_d * y_d);
    }

    public double dst2(Vector2D v) {
        final double x_d = v.x - x;
        final double y_d = v.y - y;
        return x_d * x_d + y_d * y_d;
    }

    /**
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     *
     * @return the squared distance between this and the other vector
     */
    public double dst2(double x, double y) {
        final double x_d = x - this.x;
        final double y_d = y - this.y;
        return x_d * x_d + y_d * y_d;
    }

    @Override
    public Vector2D limit(double limit) {
        return limit2(limit * limit);
    }

    @Override
    public Vector2D limit2(double limit2) {
        double len2 = len2();
        if (len2 > limit2) {
            return scl(Math.sqrt(limit2 / len2));
        }
        return this;
    }

    @Override
    public Vector2D setLength(double len) {
        return null;
    }

    @Override
    public Vector2D setLength2(double len2) {
        return null;
    }

    public Vector2D clamp(double min, double max) {
        final double l2 = len2();
        if (l2 == 0f)
            return this;
        if (l2 > max * max)
            return nor().scl(max);
        if (l2 < min * min)
            return nor().scl(min);
        return this;
    }

    public String toString() {
        return "[" + x + ":" + y + "]";
    }

    /**
     * Left-multiplies this vector by the given matrix
     *
     * @param mat the matrix
     *
     * @return this vector
     */
    public Vector2D mul(Matrix3 mat) {
        double x = this.x * mat.val[0] + this.y * mat.val[3] + mat.val[6];
        double y = this.x * mat.val[1] + this.y * mat.val[4] + mat.val[7];
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Calculates the 2D cross product between this and the given vector.
     *
     * @param v the other vector
     *
     * @return the cross product
     */
    public double crs(Vector2D v) {
        return this.x * v.y - this.y * v.x;
    }

    /**
     * Calculates the 2D cross product between this and the given vector.
     *
     * @param x the x-coordinate of the other vector
     * @param y the y-coordinate of the other vector
     *
     * @return the cross product
     */
    public double crs(double x, double y) {
        return this.x * y - this.y * x;
    }

    /**
     * @return the angle in degrees of this vector (point) relative to the x-axis. Angles are towards the positive y-axis (typically
     * counter-clockwise) and between 0 and 360.
     */
    public double angle() {
        double angle = FastMath.atan2(y, x) * MathUtils.radiansToDegrees;
        if (angle < 0)
            angle += 360;
        return angle;
    }

    /**
     * @return the angle in radians of this vector (point) relative to the x-axis. Angles are towards the positive y-axis.
     * (typically counter-clockwise)
     */
    public double getAngleRad() {
        return FastMath.atan2(y, x);
    }

    /**
     * Sets the angle of the vector in radians relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
     *
     * @param radians The angle in radians to set.
     */
    public Vector2D setAngleRad(double radians) {
        this.set(len(), 0f);
        this.rotateRad(radians);

        return this;
    }

    /**
     * Sets the angle of the vector in degrees relative to the x-axis, towards the positive y-axis (typically counter-clockwise).
     *
     * @param degrees The angle in degrees to set.
     */
    public Vector2D setAngle(double degrees) {
        return setAngleRad(degrees * MathUtils.degreesToRadians);
    }

    /**
     * Rotates the Vector2d by the given angle, counter-clockwise assuming the y-axis points up.
     *
     * @param degrees the angle in degrees
     */
    public Vector2D rotate(double degrees) {
        return rotateRad(degrees * MathUtils.degreesToRadians);
    }

    /**
     * Rotates the Vector2d by the given angle, counter-clockwise assuming the y-axis points up.
     *
     * @param radians the angle in radians
     */
    public Vector2D rotateRad(double radians) {
        double cos = FastMath.cos(radians);
        double sin = FastMath.sin(radians);

        double newX = this.x * cos - this.y * sin;
        double newY = this.x * sin + this.y * cos;

        this.x = newX;
        this.y = newY;

        return this;
    }

    public double[] values() {
        return new double[] { x, y };
    }

    /** Rotates the {@link Vector2D} by 90 degrees in the specified direction, where &ge;0 is counter-clockwise and &lt;0 is clockwise. */
    public Vector2D rotate90(int dir) {
        double x = this.x;
        if (dir >= 0) {
            this.x = -y;
            y = x;
        } else {
            this.x = y;
            y = -x;
        }
        return this;
    }

    public Vector2D lerp(Vector2D target, double alpha) {
        final double invAlpha = 1.0f - alpha;
        this.x = (x * invAlpha) + (target.x * alpha);
        this.y = (y * invAlpha) + (target.y * alpha);
        return this;
    }

    @Override
    public Vector2D interpolate(Vector2D target, double alpha, InterpolationDouble interpolation) {
        return lerp(target, interpolation.apply(alpha));
    }

    @Override
    public Vector2D setToRandomDirection() {
        double theta = MathUtilsDouble.random(0, MathUtils.PI2);
        return this.set(MathUtilsDouble.cos(theta), MathUtilsDouble.sin(theta));
    }

    public boolean epsilonEquals(Vector2D other, double epsilon) {
        if (other == null)
            return false;
        if (Math.abs(other.x - x) > epsilon)
            return false;
        return !(Math.abs(other.y - y) > epsilon);
    }

    /**
     * Compares this vector with the other vector, using the supplied epsilon for fuzzy equality testing.
     *
     * @return whether the vectors are the same.
     */
    public boolean epsilonEquals(double x, double y, double epsilon) {
        if (Math.abs(x - this.x) > epsilon)
            return false;
        return !(Math.abs(y - this.y) > epsilon);
    }

    public boolean isUnit() {
        return isUnit(0.000000001);
    }

    public boolean isUnit(final double margin) {
        return FastMath.abs(len2() - 1) < margin;
    }

    public boolean isZero() {
        return x == 0 && y == 0;
    }

    public boolean isZero(final double margin) {
        return len2() < margin;
    }

    @Override
    public boolean isOnLine(Vector2D other) {
        return MathUtilsDouble.isZero(x * other.y - y * other.x);
    }

    @Override
    public boolean isOnLine(Vector2D other, double epsilon) {
        return MathUtilsDouble.isZero(x * other.y - y * other.x, epsilon);
    }

    @Override
    public boolean isCollinear(Vector2D other, double epsilon) {
        return isOnLine(other, epsilon) && dot(other) > 0f;
    }

    @Override
    public boolean isCollinear(Vector2D other) {
        return isOnLine(other) && dot(other) > 0f;
    }

    @Override
    public boolean isCollinearOpposite(Vector2D other, double epsilon) {
        return isOnLine(other, epsilon) && dot(other) < 0f;
    }

    @Override
    public boolean isCollinearOpposite(Vector2D other) {
        return isOnLine(other) && dot(other) < 0f;
    }

    @Override
    public boolean isPerpendicular(Vector2D vector) {
        return MathUtilsDouble.isZero(dot(vector));
    }

    @Override
    public boolean isPerpendicular(Vector2D vector, double epsilon) {
        return MathUtilsDouble.isZero(dot(vector), epsilon);
    }

    @Override
    public boolean hasSameDirection(Vector2D vector) {
        return dot(vector) > 0;
    }

    @Override
    public boolean hasOppositeDirection(Vector2D vector) {
        return dot(vector) < 0;
    }
}
