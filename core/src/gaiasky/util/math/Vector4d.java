/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.NumberUtils;

import java.io.Serial;
import java.io.Serializable;

public class Vector4d implements Serializable, VectorDouble<Vector4d> {
    @Serial
    private static final long serialVersionUID = -5394070284130414492L;
    public double x;
    public double y;
    public double z;
    public double w;
    public static final Vector4d X = new Vector4d(1.0, 0.0, 0.0, 0.0);
    public static final Vector4d Y = new Vector4d(0.0, 1.0, 0.0, 0.0);
    public static final Vector4d Z = new Vector4d(0.0, 0.0, 1.0, 0.0);
    public static final Vector4d W = new Vector4d(0.0, 0.0, 0.0F, 1.0F);
    public static final Vector4d Zero = new Vector4d(0.0, 0.0, 0.0, 0.0);

    public Vector4d() {
    }

    public Vector4d(double x, double y, double z, double w) {
        this.set(x, y, z, w);
    }

    public Vector4d(Vector4d vector) {
        this.set(vector.x, vector.y, vector.z, vector.w);
    }

    public Vector4d(double[] values) {
        this.set(values[0], values[1], values[2], values[3]);
    }

    public Vector4d(Vector2 vector, double z, double w) {
        this.set(vector.x, vector.y, z, w);
    }

    public Vector4d(Vector3 vector, double w) {
        this.set(vector.x, vector.y, vector.z, w);
    }

    public Vector4d set(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4d set(Vector4d vector) {
        return this.set(vector.x, vector.y, vector.z, vector.w);
    }

    public Vector4d set(double[] values) {
        return this.set(values[0], values[1], values[2], values[3]);
    }

    public Vector4d set(Vector2 vector, double z, double w) {
        return this.set(vector.x, vector.y, z, w);
    }

    public Vector4d set(Vector3 vector, double w) {
        return this.set(vector.x, vector.y, vector.z, w);
    }

    public Vector4d setToRandomDirection() {
        double v1;
        double v2;
        double s;
        do {
            v1 = (MathUtilsDouble.random() - 0.5F) * 2.0F;
            v2 = (MathUtilsDouble.random() - 0.5F) * 2.0F;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1.0F || s == 0.0F);

        double multiplier = (double) Math.sqrt(-2.0 * Math.log((double) s) / (double) s);
        this.x = v1 * multiplier;
        this.y = v2 * multiplier;

        do {
            do {
                v1 = (MathUtilsDouble.random() - 0.5F) * 2.0F;
                v2 = (MathUtilsDouble.random() - 0.5F) * 2.0F;
                s = v1 * v1 + v2 * v2;
            } while (s >= 1.0F);
        } while (s == 0.0F);

        multiplier = (double) Math.sqrt(-2.0 * Math.log((double) s) / (double) s);
        this.z = v1 * multiplier;
        this.w = v2 * multiplier;
        return this.nor();
    }

    public Vector4d cpy() {
        return new Vector4d(this);
    }

    public Vector4d add(Vector4d vector) {
        return this.add(vector.x, vector.y, vector.z, vector.w);
    }

    public Vector4d add(double x, double y, double z, double w) {
        return this.set(this.x + x, this.y + y, this.z + z, this.w + w);
    }

    public Vector4d add(double values) {
        return this.set(this.x + values, this.y + values, this.z + values, this.w + values);
    }

    public Vector4d sub(Vector4d a_vec) {
        return this.sub(a_vec.x, a_vec.y, a_vec.z, a_vec.w);
    }

    public Vector4d sub(double x, double y, double z, double w) {
        return this.set(this.x - x, this.y - y, this.z - z, this.w - w);
    }

    public Vector4d sub(double value) {
        return this.set(this.x - value, this.y - value, this.z - value, this.w - value);
    }

    public Vector4d scl(double scalar) {
        return this.set(this.x * scalar, this.y * scalar, this.z * scalar, this.w * scalar);
    }

    public Vector4d scl(Vector4d other) {
        return this.set(this.x * other.x, this.y * other.y, this.z * other.z, this.w * other.w);
    }

    public Vector4d scl(double vx, double vy, double vz, double vw) {
        return this.set(this.x * vx, this.y * vy, this.z * vz, this.w * vw);
    }

    public Vector4d mulAdd(Vector4d vec, double scalar) {
        this.x += vec.x * scalar;
        this.y += vec.y * scalar;
        this.z += vec.z * scalar;
        this.w += vec.w * scalar;
        return this;
    }

    public Vector4d mulAdd(Vector4d vec, Vector4d mulVec) {
        this.x += vec.x * mulVec.x;
        this.y += vec.y * mulVec.y;
        this.z += vec.z * mulVec.z;
        this.w += vec.w * mulVec.w;
        return this;
    }

    public static double len(double x, double y, double z, double w) {
        return (double) Math.sqrt((double) (x * x + y * y + z * z + w * w));
    }

    public double len() {
        return (double) Math.sqrt((double) (this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w));
    }

    public static double len2(double x, double y, double z, double w) {
        return x * x + y * y + z * z + w * w;
    }

    public double len2() {
        return this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
    }

    public boolean idt(Vector4d vector) {
        return this.x == vector.x && this.y == vector.y && this.z == vector.z && this.w == vector.w;
    }

    public static double dst(double x1, double y1, double z1, double w1, double x2, double y2, double z2, double w2) {
        double a = x2 - x1;
        double b = y2 - y1;
        double c = z2 - z1;
        double d = w2 - w1;
        return (double) Math.sqrt((double) (a * a + b * b + c * c + d * d));
    }

    public double dst(Vector4d vector) {
        double a = vector.x - this.x;
        double b = vector.y - this.y;
        double c = vector.z - this.z;
        double d = vector.w - this.w;
        return (double) Math.sqrt((double) (a * a + b * b + c * c + d * d));
    }

    public double dst(double x, double y, double z, double w) {
        double a = x - this.x;
        double b = y - this.y;
        double c = z - this.z;
        double d = w - this.w;
        return (double) Math.sqrt((double) (a * a + b * b + c * c + d * d));
    }

    public static double dst2(double x1, double y1, double z1, double w1, double x2, double y2, double z2, double w2) {
        double a = x2 - x1;
        double b = y2 - y1;
        double c = z2 - z1;
        double d = w2 - w1;
        return a * a + b * b + c * c + d * d;
    }

    public double dst2(Vector4d point) {
        double a = point.x - this.x;
        double b = point.y - this.y;
        double c = point.z - this.z;
        double d = point.w - this.w;
        return a * a + b * b + c * c + d * d;
    }

    public double dst2(double x, double y, double z, double w) {
        double a = x - this.x;
        double b = y - this.y;
        double c = z - this.z;
        double d = w - this.w;
        return a * a + b * b + c * c + d * d;
    }

    public Vector4d nor() {
        double len2 = this.len2();
        return len2 != 0.0F && len2 != 1.0F ? this.scl(1.0F / (double) Math.sqrt((double) len2)) : this;
    }

    public static double dot(double x1, double y1, double z1, double w1, double x2, double y2, double z2, double w2) {
        return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2;
    }

    public double dot(Vector4d vector) {
        return this.x * vector.x + this.y * vector.y + this.z * vector.z + this.w * vector.w;
    }

    public double dot(double x, double y, double z, double w) {
        return this.x * x + this.y * y + this.z * z + this.w * w;
    }

    public boolean isUnit() {
        return this.isUnit(1.0E-9F);
    }

    public boolean isUnit(double margin) {
        return Math.abs(this.len2() - 1.0F) < margin;
    }

    public boolean isZero() {
        return this.x == 0.0F && this.y == 0.0F && this.z == 0.0F && this.w == 0.0F;
    }

    public boolean isZero(double margin) {
        return this.len2() < margin;
    }

    public boolean isOnLine(Vector4d other, double epsilon) {
        int flags = 0;
        double dx = 0.0F;
        double dy = 0.0F;
        double dz = 0.0F;
        double dw = 0.0F;
        if (MathUtilsDouble.isZero(this.x, epsilon)) {
            if (!MathUtilsDouble.isZero(other.x, epsilon)) {
                return false;
            }
        } else {
            dx = this.x / other.x;
            flags |= 1;
        }

        if (MathUtilsDouble.isZero(this.y, epsilon)) {
            if (!MathUtilsDouble.isZero(other.y, epsilon)) {
                return false;
            }
        } else {
            dy = this.y / other.y;
            flags |= 2;
        }

        if (MathUtilsDouble.isZero(this.z, epsilon)) {
            if (!MathUtilsDouble.isZero(other.z, epsilon)) {
                return false;
            }
        } else {
            dz = this.z / other.z;
            flags |= 4;
        }

        if (MathUtilsDouble.isZero(this.w, epsilon)) {
            if (!MathUtilsDouble.isZero(other.w, epsilon)) {
                return false;
            }
        } else {
            dw = this.w / other.w;
            flags |= 8;
        }

        return switch (flags) {
            case 0, 1, 2, 4, 8 -> true;
            case 3 -> MathUtilsDouble.isEqual(dx, dy, epsilon);
            case 5 -> MathUtilsDouble.isEqual(dx, dz, epsilon);
            case 6 -> MathUtilsDouble.isEqual(dy, dz, epsilon);
            case 7 -> MathUtilsDouble.isEqual(dx, dy, epsilon) && MathUtilsDouble.isEqual(dx, dz, epsilon);
            case 9 -> MathUtilsDouble.isEqual(dx, dw, epsilon);
            case 10 -> MathUtilsDouble.isEqual(dy, dw, epsilon);
            case 11 -> MathUtilsDouble.isEqual(dx, dy, epsilon) && MathUtilsDouble.isEqual(dx, dw, epsilon);
            case 12 -> MathUtilsDouble.isEqual(dz, dw, epsilon);
            case 13 -> MathUtilsDouble.isEqual(dx, dz, epsilon) && MathUtilsDouble.isEqual(dx, dw, epsilon);
            case 14 -> MathUtilsDouble.isEqual(dy, dz, epsilon) && MathUtilsDouble.isEqual(dy, dw, epsilon);
            default ->
                    MathUtilsDouble.isEqual(dx, dy, epsilon) && MathUtilsDouble.isEqual(dx, dz, epsilon) && MathUtilsDouble.isEqual(dx, dw, epsilon);
        };
    }

    public boolean isOnLine(Vector4d other) {
        return this.isOnLine(other, 1.0E-6F);
    }

    public boolean isCollinear(Vector4d other, double epsilon) {
        return this.isOnLine(other, epsilon) && this.hasSameDirection(other);
    }

    public boolean isCollinear(Vector4d other) {
        return this.isOnLine(other) && this.hasSameDirection(other);
    }

    public boolean isCollinearOpposite(Vector4d other, double epsilon) {
        return this.isOnLine(other, epsilon) && this.hasOppositeDirection(other);
    }

    public boolean isCollinearOpposite(Vector4d other) {
        return this.isOnLine(other) && this.hasOppositeDirection(other);
    }

    public boolean isPerpendicular(Vector4d vector) {
        return MathUtilsDouble.isZero(this.dot(vector));
    }

    public boolean isPerpendicular(Vector4d vector, double epsilon) {
        return MathUtilsDouble.isZero(this.dot(vector), epsilon);
    }

    public boolean hasSameDirection(Vector4d vector) {
        return this.dot(vector) > 0.0F;
    }

    public boolean hasOppositeDirection(Vector4d vector) {
        return this.dot(vector) < 0.0F;
    }

    public Vector4d lerp(Vector4d target, double alpha) {
        this.x += alpha * (target.x - this.x);
        this.y += alpha * (target.y - this.y);
        this.z += alpha * (target.z - this.z);
        this.w += alpha * (target.w - this.w);
        return this;
    }

    public Vector4d interpolate(Vector4d target, double alpha, InterpolationDouble interpolator) {
        return this.lerp(target, interpolator.apply(alpha));
    }

    public String toString() {
        return "(" + this.x + "," + this.y + "," + this.z + "," + this.w + ")";
    }

    public Vector4d fromString(String v) {
        int s0 = v.indexOf(44, 1);
        int s1 = v.indexOf(44, s0 + 1);
        int s2 = v.indexOf(44, s1 + 1);
        if (s0 != -1 && s1 != -1 && v.charAt(0) == '(' && v.charAt(v.length() - 1) == ')') {
            try {
                double x = Float.parseFloat(v.substring(1, s0));
                double y = Float.parseFloat(v.substring(s0 + 1, s1));
                double z = Float.parseFloat(v.substring(s1 + 1, s2));
                double w = Float.parseFloat(v.substring(s2 + 1, v.length() - 1));
                return this.set(x, y, z, w);
            } catch (NumberFormatException ignored) {
            }
        }

        throw new GdxRuntimeException("Malformed Vector4d: " + v);
    }

    public Vector4d limit(double limit) {
        return this.limit2(limit * limit);
    }

    public Vector4d limit2(double limit2) {
        double len2 = this.len2();
        if (len2 > limit2) {
            this.scl((double) Math.sqrt((double) (limit2 / len2)));
        }

        return this;
    }

    public Vector4d setLength(double len) {
        return this.setLength2(len * len);
    }

    public Vector4d setLength2(double len2) {
        double oldLen2 = this.len2();
        return oldLen2 != 0.0F && oldLen2 != len2 ? this.scl((double) Math.sqrt((double) (len2 / oldLen2))) : this;
    }

    public Vector4d clamp(double min, double max) {
        double len2 = this.len2();
        if (len2 == 0.0F) {
            return this;
        } else {
            double max2 = max * max;
            if (len2 > max2) {
                return this.scl((double) Math.sqrt((double) (max2 / len2)));
            } else {
                double min2 = min * min;
                return len2 < min2 ? this.scl((double) Math.sqrt((double) (min2 / len2))) : this;
            }
        }
    }

    public int hashCode() {
        final long prime = 31;
        long result = 1;
        result = prime * result + NumberUtils.doubleToLongBits(x);
        result = prime * result + NumberUtils.doubleToLongBits(y);
        result = prime * result + NumberUtils.doubleToLongBits(z);
        result = prime * result + NumberUtils.doubleToLongBits(w);
        return (int) result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        } else {
            Vector4d other = (Vector4d) obj;
            if (NumberUtils.doubleToLongBits(x) != NumberUtils.doubleToLongBits(other.x))
                return false;
            else if (NumberUtils.doubleToLongBits(y) != NumberUtils.doubleToLongBits(other.y))
                return false;
            else if (NumberUtils.doubleToLongBits(z) == NumberUtils.doubleToLongBits(other.z))
                return false;
            else
                return NumberUtils.doubleToLongBits(this.w) == NumberUtils.doubleToLongBits(other.w);
        }
    }

    public boolean epsilonEquals(Vector4d other, double epsilon) {
        if (other == null) {
            return false;
        } else if (Math.abs(other.x - this.x) > epsilon) {
            return false;
        } else if (Math.abs(other.y - this.y) > epsilon) {
            return false;
        } else if (Math.abs(other.z - this.z) > epsilon) {
            return false;
        } else {
            return !(Math.abs(other.w - this.w) > epsilon);
        }
    }

    public boolean epsilonEquals(double x, double y, double z, double w, double epsilon) {
        if (Math.abs(x - this.x) > epsilon) {
            return false;
        } else if (Math.abs(y - this.y) > epsilon) {
            return false;
        } else if (Math.abs(z - this.z) > epsilon) {
            return false;
        } else {
            return !(Math.abs(w - this.w) > epsilon);
        }
    }

    public boolean epsilonEquals(Vector4d other) {
        return this.epsilonEquals(other, 1.0E-6);
    }

    public boolean epsilonEquals(double x, double y, double z, double w) {
        return this.epsilonEquals(x, y, z, w, 1.0E-6);
    }

    public Vector4d setZero() {
        this.x = 0.0F;
        this.y = 0.0F;
        this.z = 0.0F;
        this.w = 0.0F;
        return this;
    }
}
