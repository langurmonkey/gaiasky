/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.RandomXS128;
import net.jafama.FastMath;

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
    static Vector3D aux0, aux1, aux2, aux3, aux4, aux5;

    static {
        aux0 = new Vector3D();
        aux1 = new Vector3D();
        aux2 = new Vector3D();
        aux3 = new Vector3D();
        aux4 = new Vector3D();
        aux5 = new Vector3D();
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
     * Returns the sine in degrees from a lookup table. For optimal precision, use degrees between -360 and 360 (both
     * inclusive).
     */
    static public double sinDeg(double degrees) {
        return Sin.table[(int) (degrees * degToIndex) & SIN_MASK];
    }

    /**
     * Returns the cosine in degrees from a lookup table. For optimal precision, use degrees between -360 and 360 (both
     * inclusive).
     */
    static public double cosDeg(double degrees) {
        return Sin.table[(int) ((degrees + 90) * degToIndex) & SIN_MASK];
    }

    /**
     * Returns atan2 in radians, faster but less accurate than FastMath.atan2. Average error of 0.00231 radians (0.1323
     * degrees),
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
        return FastMath.toRange(min, max, value);
    }

    static public short clamp(short value,
                              short min,
                              short max) {
        return (short) FastMath.toRange(min, max, value);
    }

    static public long clamp(long value,
                             long min,
                             long max) {
        return FastMath.toRange(min, max, value);
    }

    static public float clamp(float value,
                              float min,
                              float max) {
        return FastMath.toRange(min, max, value);
    }

    static public double clamp(double value,
                               double min,
                               double max) {
        return FastMath.toRange(min, max, value);
    }

    static public float saturate(float value) {
        return FastMath.toRange(0.0f, 1.0f, value);
    }

    static public double saturate(double value) {
        return FastMath.toRange(0.0, 1.0, value);
    }

    static public double max(double... values) {
        double max = Double.MIN_VALUE;
        for (var v : values) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }
    static public float max(float... values) {
        float max = Float.MIN_VALUE;
        for (var v : values) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    static public int max(int... values) {
        int max = Integer.MIN_VALUE;
        for (var v : values) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    static public double min(double... values) {
        double max = Double.MAX_VALUE;
        for (var v : values) {
            if (v < max) {
                max = v;
            }
        }
        return max;
    }

    static public float min(float... values) {
        float max = Float.MAX_VALUE;
        for (var v : values) {
            if (v < max) {
                max = v;
            }
        }
        return max;
    }

    static public int min(int... values) {
        int max = Integer.MAX_VALUE;
        for (var v : values) {
            if (v < max) {
                max = v;
            }
        }
        return max;
    }

    /**
     * Returns true if the value is zero (using the default tolerance as upper
     * bound)
     */
    static public boolean isZero(double value) {
        return FastMath.abs(value) <= FLOAT_ROUNDING_ERROR;
    }

    /**
     * Returns true if the value is zero.
     *
     * @param tolerance represent an upper bound below which the value is considered
     *                  zero.
     */
    static public boolean isZero(double value,
                                 double tolerance) {
        return FastMath.abs(value) <= tolerance;
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
        return FastMath.abs(a - b) <= FLOAT_ROUNDING_ERROR;
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
        return FastMath.abs(a - b) <= tolerance;
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
     * GLSL mix function: performs a linear interpolation between {@code x} and {@code y} using {@code a} to weight between them.
     *
     * @param x start of the range in which to interpolate.
     * @param y end of the range in which to interpolate.
     * @param a value to use to interpolate between x and y.
     *
     * @return Interpolation between {@code x} and {@code y} using {@code a} as weight.
     */
    public static double mix(double x, double y, double a) {
        return x * (1.0 - a) + y * a;
    }

    /**
     * Returns a soft minimum between two values, biased toward the lower one.
     * The {@code weight} controls how strongly the result is pulled toward the minimum.
     *
     * @param a      First value.
     * @param b      Second value.
     * @param weight The interpolation factor. 0 returns {@code a}, 1 returns {@code min(a, b)}.
     *
     * @return A softly biased minimum of {@code a} and {@code b}.
     */
    public static double softMin(double a, double b, double weight) {
        return lerp(a, FastMath.min(a, b), weight);
    }

    /**
     * Linearly interpolates between two values.
     *
     * @param a The start value.
     * @param b The end value.
     * @param t The interpolation factor, typically in the range [0, 1].
     *          A value of 0 returns {@code a}, a value of 1 returns {@code b}, and values in between interpolate linearly.
     *
     * @return The interpolated value between {@code a} and {@code b}.
     */
    public static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    /**
     * Fast linear interpolation. If you can guarantee that x0 &le; x1, then use this.
     *
     * @param x  The value to interpolate.
     * @param x0 Inferior limit to the independent value.
     * @param x1 Superior limit to the independent value.
     * @param y0 Inferior limit to the dependent value.
     * @param y1 Superior limit to the dependent value.
     *
     * @return The interpolated value.
     */
    public static double flint(double x,
                               double x0,
                               double x1,
                               double y0,
                               double y1) {
        if (x < x0) {
            return y0;
        }
        if (x > x1) {
            return y1;
        }

        return y0 + (y1 - y0) * (x - x0) / (x1 - x0);
    }

    /**
     * Linear interpolation.
     *
     * @param x  The value to interpolate.
     * @param x0 Inferior limit to the independent value.
     * @param x1 Superior limit to the independent value.
     * @param y0 Inferior limit to the dependent value.
     * @param y1 Superior limit to the dependent value.
     *
     * @return The interpolated value.
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
     * Linear interpolation.
     *
     * @param x  The value to interpolate.
     * @param x0 Inferior limit to the independent value.
     * @param x1 Superior limit to the independent value.
     * @param y0 Inferior limit to the dependent value.
     * @param y1 Superior limit to the dependent value.
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
     * Linear interpolation.
     *
     * @param x  The value to interpolate.
     * @param x0 Inferior limit to the independent value.
     * @param x1 Superior limit to the independent value.
     * @param y0 Inferior limit to the dependent value.
     * @param y1 Superior limit to the dependent value.
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
        Vector3D v = aux0.set(x1, y1, z1);
        Vector3D w = aux1.set(x2, y2, z2);
        Vector3D p = aux2.set(x0, y0, z0);
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
        Vector3D projection = v.add(aux4.scl(t)); // Projection falls on the segment
        return p.dst(projection);
    }

    public static Vector3D getClosestPoint2(double x1,
                                            double y1,
                                            double z1,
                                            double x2,
                                            double y2,
                                            double z2,
                                            double x0,
                                            double y0,
                                            double z0,
                                            Vector3D result) {
        //           (P2-P1)dot(v)
        //Pr = P1 +  ------------- * v.
        //           (v)dot(v)

        Vector3D p1 = aux0.set(x1, y1, z1);
        Vector3D p2 = aux1.set(x0, y0, z0);
        Vector3D v = aux2.set(x2 - x1, y2 - y1, z2 - z1);

        double nomin = aux3.set(p2).sub(p1).dot(v);
        double denom = v.dot(v);
        Vector3D frac = aux4.set(v).scl(nomin / denom);

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
        double scale = FastMath.pow(10, places);
        return FastMath.round(value * scale) / scale;
    }

    // ---
    static private class Sin {
        static final double[] table = new double[SIN_COUNT];

        static {
            for (int i = 0; i < SIN_COUNT; i++)
                table[i] = FastMath.sin((i + 0.5) / SIN_COUNT * radFull);
            for (int i = 0; i < 360; i += 90)
                table[(int) (i * degToIndex) & SIN_MASK] = FastMath.sin(i * degreesToRadians);
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
        final double absA = FastMath.abs(a);
        final double absB = FastMath.abs(b);
        final double diff = FastMath.abs(a - b);

        if (a.equals(b)) {
            // shortcut, handles infinities
            return true;
        } else if (a == 0 || b == 0 || absA + absB < Double.MIN_NORMAL) {
            // a or b is zero or both are extremely close to it
            // relative error is less meaningful here
            // NOT SURE HOW RELATIVE EPSILON WORKS IN THIS CASE
            return diff < (epsilon * Double.MIN_NORMAL);
        } else {
            // use relative error
            return diff / FastMath.min((absA + absB), Double.MAX_VALUE) < epsilon;
        }
    }


    /**
     * Implements a low-pass filter to smooth the input values.
     *
     * @param newValue      The new value.
     * @param smoothedValue The previous smoothed value.
     * @param smoothing     The smoothing factor.
     *
     * @return The new value with a low-pass filter applied.
     */
    public static double lowPass(double newValue, double smoothedValue, double smoothing) {
        if (smoothing > 0) {
            smoothedValue += (newValue - smoothedValue) / smoothing;
            return smoothedValue;
        } else {
            return newValue;
        }
    }

    /**
     * Implements a low-pass filter to smooth the input values.
     *
     * @param newValue      The new value.
     * @param smoothedValue The previous smoothed value.
     * @param smoothing     The smoothing factor.
     *
     * @return The new value with a low-pass filter applied.
     */
    public static float lowPass(float newValue, float smoothedValue, float smoothing) {
        if (smoothing > 0) {
            smoothedValue += (newValue - smoothedValue) / smoothing;
            return smoothedValue;
        } else {
            return newValue;
        }
    }

    /**
     * Implements the logit function, defined as <code>logit(x) = log(x/(1-x))</code>. Note that logit(0) = -Infinity,
     * and logit(1) = Infinity.
     *
     * @param x The value to sample.
     */
    public static double logit(double x) {
        return FastMath.log(x / (1.0 - x));
    }

    /**
     * Implements the logistic sigmoid, defined as <code>expit(x) = 1/(1+exp(-x))</code>. It is the inverse of logit.
     *
     * @param x    The value to sample, in [0, 1].
     * @param span The span of the function. The value gets re-mapped using this span.
     *
     * @return The logistic sigmoid function.
     */
    public static double logisticSigmoid(double x, double span) {
        x = x * span - (span * 0.5);
        return FastMath.exp(x) / (1.0 + FastMath.exp(x));
    }

    /**
     * Double precision version
     */
    public static double smoothstep(double edge0, double edge1, double x) {
        if (edge0 >= edge1) {
            return 0.0;
        }

        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

}



