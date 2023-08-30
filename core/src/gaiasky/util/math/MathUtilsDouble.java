/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.RandomXS128;
import net.jafama.FastMath;
import org.apfloat.Apfloat;

import java.util.Random;

public final class MathUtilsDouble {

    // ---
    static public final double FLOAT_ROUNDING_ERROR = 0.000001; // 32 bits
    public static final double PI = 3.14159265358979323846;
    static public final double PI2 = PI * 2;

    /** multiply by this to convert from radians to degrees */
    static public final double radiansToDegrees = 180 / PI;
    static public final double radDeg = radiansToDegrees;
    /** multiply by this to convert from degrees to radians */
    static public final double degreesToRadians = PI / 180;
    static public final double degRad = degreesToRadians;
    static private final double degFull = 360;
    static private final int SIN_BITS = 14; // 16KB. Adjust for accuracy.
    static private final int SIN_MASK = ~(-1 << SIN_BITS);
    static private final int SIN_COUNT = SIN_MASK + 1;
    static private final double degToIndex = SIN_COUNT / degFull;
    static private final double radFull = PI * 2;
    static private final double radToIndex = SIN_COUNT / radFull;
    static public Random random = new RandomXS128();
    static Vector3d aux0, aux1, aux2, aux3, aux4, aux5;

    static {
        aux0 = new Vector3d();
        aux1 = new Vector3d();
        aux2 = new Vector3d();
        aux3 = new Vector3d();
        aux4 = new Vector3d();
        aux5 = new Vector3d();
    }

    /** Returns the sine in radians from a lookup table. */
    static public double sin(double radians) {
        return Sin.table[(int) (radians * radToIndex) & SIN_MASK];
    }

    /** Returns the cosine in radians from a lookup table. */
    static public double cos(double radians) {
        return Sin.table[(int) ((radians + PI / 2) * radToIndex) & SIN_MASK];
    }

    /**
     * Returns atan2 in radians, faster but less accurate than Math.atan2. Average error of 0.00231 radians (0.1323 degrees),
     * largest error of 0.00488 radians (0.2796 degrees).
     */
    static public double atan2(double y,
                               double x) {
        if (x == 0) {
            if (y > 0)
                return PI / 2;
            if (y == 0)
                return 0;
            return -PI / 2;
        }
        final double atan, z = y / x;
        if (Math.abs(z) < 1) {
            atan = z / (1 + 0.28 * z * z);
            if (x < 0)
                return atan + (y < 0 ? -PI : PI);
            return atan;
        }
        atan = PI / 2 - z / (z * z + 0.28);
        return y < 0 ? atan - PI : atan;
    }

    /** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
    static public int random(int range) {
        return random.nextInt(range + 1);
    }

    /** Returns a random number between start (inclusive) and end (inclusive). */
    static public int random(int start,
                             int end) {
        return start + random.nextInt(end - start + 1);
    }

    /** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
    static public long random(long range) {
        return (long) (random.nextDouble() * range);
    }

    /** Returns a random number between start (inclusive) and end (inclusive). */
    static public long random(long start,
                              long end) {
        return start + (long) (random.nextDouble() * (end - start));
    }

    /** Returns random number between 0.0 (inclusive) and 1.0 (exclusive). */
    static public double random() {
        return random.nextDouble();
    }

    /** Returns a random number between 0 (inclusive) and the specified value (exclusive). */
    static public double random(double range) {
        return random.nextDouble() * range;
    }

    /** Returns a random number between start (inclusive) and end (exclusive). */
    static public double random(double start,
                                double end) {
        return start + random.nextDouble() * (end - start);
    }

    static public int clamp(int value,
                            int min,
                            int max) {
        if (value < min)
            return min;
        return Math.min(value, max);
    }

    static public short clamp(short value,
                              short min,
                              short max) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    static public float clamp(float value,
                              float min,
                              float max) {
        if (value < min)
            return min;
        return Math.min(value, max);
    }

    static public double clamp(double value,
                               double min,
                               double max) {
        if (value < min)
            return min;
        return Math.min(value, max);
    }

    /**
     * Returns true if the value is zero (using the default tolerance as upper
     * bound)
     */
    static public boolean isZero(double value) {
        return Math.abs(value) <= FLOAT_ROUNDING_ERROR;
    }

    /**
     * Returns true if the value is zero.
     *
     * @param tolerance represent an upper bound below which the value is considered
     *                  zero.
     */
    static public boolean isZero(double value,
                                 double tolerance) {
        return Math.abs(value) <= tolerance;
    }

    /**
     * Returns true if a is nearly equal to b. The function uses the default
     * doubleing error tolerance.
     *
     * @param a the first value.
     * @param b the second value.
     */
    static public boolean isEqual(double a,
                                  double b) {
        return Math.abs(a - b) <= FLOAT_ROUNDING_ERROR;
    }

    /**
     * Returns true if a is nearly equal to b.
     *
     * @param a         the first value.
     * @param b         the second value.
     * @param tolerance represent an upper bound below which the two values are
     *                  considered equal.
     */
    static public boolean isEqual(double a,
                                  double b,
                                  double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    /**
     * Fast sqrt method. Default passes it through one round of Newton's method
     *
     * @param value The value
     *
     * @return The square root value
     */
    static public double sqrt(double value) {
        double sqrt = Double.longBitsToDouble(((Double.doubleToLongBits(value) - (1L << 52)) >> 1) + (1L << 61));
        return (sqrt + value / sqrt) / 2.0;
    }

    /**
     * Linear interpolation
     *
     * @param x  The value to interpolate
     * @param x0 Inferior limit to the independent value
     * @param x1 Superior limit to the independent value
     * @param y0 Inferior limit to the dependent value
     * @param y1 Superior limit to the dependent value
     *
     * @return The interpolated value
     */
    public static double lint(double x,
                              double x0,
                              double x1,
                              double y0,
                              double y1) {
        double rx0 = x0;
        double rx1 = x1;
        if (x0 > x1) {
            rx0 = x1;
            rx1 = x0;
        }

        if (x < rx0) {
            return y0;
        }
        if (x > rx1) {
            return y1;
        }

        return y0 + (y1 - y0) * (x - rx0) / (rx1 - rx0);
    }

    /**
     * Linear interpolation
     *
     * @param x  The value to interpolate
     * @param x0 Inferior limit to the independent value
     * @param x1 Superior limit to the independent value
     * @param y0 Inferior limit to the dependent value
     * @param y1 Superior limit to the dependent value
     *
     * @return The interpolated value
     */
    public static float lint(float x,
                             float x0,
                             float x1,
                             float y0,
                             float y1) {
        float rx0 = x0;
        float rx1 = x1;
        if (x0 > x1) {
            rx0 = x1;
            rx1 = x0;
        }

        if (x < rx0) {
            return y0;
        }
        if (x > rx1) {
            return y1;
        }

        return y0 + (y1 - y0) * (x - rx0) / (rx1 - rx0);
    }

    /**
     * Linear interpolation
     *
     * @param x  The value to interpolate
     * @param x0 Inferior limit to the independent value
     * @param x1 Superior limit to the independent value
     * @param y0 Inferior limit to the dependent value
     * @param y1 Superior limit to the dependent value
     *
     * @return The interpolated value
     */
    public static float lint(long x,
                             long x0,
                             long x1,
                             float y0,
                             float y1) {
        double rx0 = x0;
        double rx1 = x1;
        if (x0 > x1) {
            rx0 = x1;
            rx1 = x0;
        }

        if (x < rx0) {
            return y0;
        }
        if (x > rx1) {
            return y1;
        }

        return (float) (y0 + (y1 - y0) * (x - rx0) / (rx1 - rx0));
    }

    /**
     * Gets the distance from the point p0 to the segment denoted by p1-p2.<br/>
     * Check <a href=
     * "http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html">this
     * link</a>.
     *
     * @param x1 The first segment delimiter.
     * @param x2 The second segment delimiter.
     * @param x0 The point.
     *
     * @return The Euclidean distance between the segment (x1, x2)
     */
    public static double distancePointSegment(double x1,
                                              double y1,
                                              double z1,
                                              double x2,
                                              double y2,
                                              double z2,
                                              double x0,
                                              double y0,
                                              double z0) {
        Vector3d v = aux0.set(x1, y1, z1);
        Vector3d w = aux1.set(x2, y2, z2);
        Vector3d p = aux2.set(x0, y0, z0);
        aux3.set(p).sub(v);
        aux4.set(w).sub(v);

        // Return minimum distance between line segment vw and point p
        double l2 = v.dst2(w);
        if (l2 == 0.0)
            return p.dst(v); // v == w case
        // Consider the line extending the segment, parameterized as v + t (w - v).
        // We find projection of point p onto the line.
        // It falls where t = [(p-v) . (w-v)] / |w-v|^2
        double t = aux3.dot(aux4) / l2;
        if (t < 0.0)
            return p.dst(v); // Beyond the 'v' end of the segment
        else if (t > 1.0)
            return p.dst(w); // Beyond the 'w' end of the segment
        Vector3d projection = v.add(aux4.scl(t)); // Projection falls on the segment
        return p.dst(projection);
    }

    public static Vector3d getClosestPoint2(double x1,
                                            double y1,
                                            double z1,
                                            double x2,
                                            double y2,
                                            double z2,
                                            double x0,
                                            double y0,
                                            double z0,
                                            Vector3d result) {
        //           (P2-P1)dot(v)
        //Pr = P1 +  ------------- * v.
        //           (v)dot(v)

        Vector3d p1 = aux0.set(x1, y1, z1);
        Vector3d p2 = aux1.set(x0, y0, z0);
        Vector3d v = aux2.set(x2 - x1, y2 - y1, z2 - z1);

        double nomin = aux3.set(p2).sub(p1).dot(v);
        double denom = v.dot(v);
        Vector3d frac = aux4.set(v).scl(nomin / denom);

        result.set(p1).add(frac);
        return result;
    }

    /**
     * Rounds the double value to a number of decimal places
     *
     * @param value  The value to round
     * @param places The number of decimal places
     *
     * @return The rounded value
     */
    public static double roundAvoid(double value,
                                    int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    // ---
    static private class Sin {
        static final double[] table = new double[SIN_COUNT];

        static {
            for (int i = 0; i < SIN_COUNT; i++)
                table[i] = Math.sin((i + 0.5) / SIN_COUNT * radFull);
            for (int i = 0; i < 360; i += 90)
                table[(int) (i * degToIndex) & SIN_MASK] = Math.sin(i * degreesToRadians);
        }
    }

    /**
     * Normalize an angle in a 2&pi; wide interval around a center value.
     * <p>This method has three main uses:</p>
     * <ul>
     *   <li>normalize an angle between 0 and 2&pi;:<br/>
     *       {@code a = MathUtils.normalizeAngle(a, FastMath.PI);}</li>
     *   <li>normalize an angle between -&pi; and +&pi;<br/>
     *       {@code a = MathUtils.normalizeAngle(a, 0.0);}</li>
     *   <li>compute the angle between two defining angular positions:<br>
     *       {@code angle = MathUtils.normalizeAngle(end, start) - start;}</li>
     * </ul>
     * <p>Note that due to numerical accuracy and since &pi; cannot be represented
     * exactly, the result interval is <em>closed</em>, it cannot be half-closed
     * as would be more satisfactory in a purely mathematical view.</p>
     *
     * @param a      angle to normalize
     * @param center center of the desired 2&pi; interval for the result
     *
     * @return a-2k&pi; with integer k and center-&pi; &lt;= a-2k&pi; &lt;= center+&pi;
     *
     * @since 1.2
     */
    public static double normalizeAngle(double a,
                                        double center) {
        return a - PI2 * FastMath.floor((a + FastMath.PI - center) / PI2);
    }

    /**
     * Compares two doubles, with an epsilon.
     *
     * @param a       first nullable element
     * @param b       second nullable element
     * @param epsilon absolute difference tolerance.
     */
    public static boolean fuzzyEquals(Double a,
                                      Double b,
                                      double epsilon) {
        final double absA = Math.abs(a);
        final double absB = Math.abs(b);
        final double diff = Math.abs(a - b);

        if (a == b) {
            // shortcut, handles infinities
            return true;
        } else if (a == 0 || b == 0 || absA + absB < Double.MIN_NORMAL) {
            // a or b is zero or both are extremely close to it
            // relative error is less meaningful here
            // NOT SURE HOW RELATIVE EPSILON WORKS IN THIS CASE
            return diff < (epsilon * Double.MIN_NORMAL);
        } else {
            // use relative error
            return diff / Math.min((absA + absB), Double.MAX_VALUE) < epsilon;
        }
    }

    /**
     * Checks whether a is divisible by b, using the default precision.
     *
     * @param a First decimal number, represented by a string.
     * @param b Second decimal number, represented by a string.
     *
     * @return Whether a % b == 0 at the default precision.
     */
    public static boolean divisible(String a,
                                    String b) {
        return divisible(a, b, Apfloat.DEFAULT);
    }

    /**
     * Checks whether a is divisible by b, using the given floating-point precision.
     *
     * @param a         First decimal number, represented by a string.
     * @param b         Second decimal number, represented by a string.
     * @param precision The precision.
     *
     * @return Whether a % b == 0 at the given precision.
     */
    public static boolean divisible(String a,
                                    String b,
                                    long precision) {
        Apfloat af = new Apfloat(a, precision);
        Apfloat bf = new Apfloat(b, precision);
        return af.mod(bf).doubleValue() == 0;
    }
}



