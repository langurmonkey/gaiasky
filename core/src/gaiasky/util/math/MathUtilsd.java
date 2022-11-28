/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;

import java.util.Random;

/**
 * Utility and fast math functions.
 * <p>
 * Thanks to Riven on JavaGaming.org for the basis of sin/cos/atan2/floor/ceil.
 *
 * @author Nathan Sweet
 */
public final class MathUtilsd {
    static public final double nanoToSec = 1 / 1000000000;

    // ---
    static public final double FLOAT_ROUNDING_ERROR = 0.000001; // 32 bits
    static public final double PI = 3.1415927;
    static public final double PI2 = PI * 2;

    static public final double E = 2.7182818;

    static private final int SIN_BITS = 14; // 16KB. Adjust for accuracy.
    static private final int SIN_MASK = ~(-1 << SIN_BITS);
    static private final int SIN_COUNT = SIN_MASK + 1;

    static private final int ACOS_RESOLUTION = 50;
    static private final int ACOS_COUNT = 360 * ACOS_RESOLUTION;
    static private final int ACOS_COUNT_1 = ACOS_COUNT - 1;

    static private final double radFull = PI * 2;
    static private final double degFull = 360;
    static private final double radToIndex = SIN_COUNT / radFull;
    static private final double degToIndex = SIN_COUNT / degFull;

    /** multiply by this to convert from radians to degrees */
    static public final double radiansToDegrees = 180 / PI;
    static public final double radDeg = radiansToDegrees;
    /** multiply by this to convert from degrees to radians */
    static public final double degreesToRadians = PI / 180;
    static public final double degRad = degreesToRadians;

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

    /** Returns the sine in radians from a lookup table. */
    static public double sin(double radians) {
        return Sin.table[(int) (radians * radToIndex) & SIN_MASK];
    }

    /** Returns the cosine in radians from a lookup table. */
    static public double cos(double radians) {
        return Sin.table[(int) ((radians + PI / 2) * radToIndex) & SIN_MASK];
    }

    /** Returns the sine in radians from a lookup table. */
    static public double sinDeg(double degrees) {
        return Sin.table[(int) (degrees * degToIndex) & SIN_MASK];
    }

    /** Returns the cosine in radians from a lookup table. */
    static public double cosDeg(double degrees) {
        return Sin.table[(int) ((degrees + 90) * degToIndex) & SIN_MASK];
    }

    // ---

    /**
     * Returns atan2 in radians, faster but less accurate than Math.atan2. Average error of 0.00231 radians (0.1323 degrees),
     * largest error of 0.00488 radians (0.2796 degrees).
     */
    static public double atan2(double y, double x) {
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

    static public Random random = new RandomXS128();

    /** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
    static public int random(int range) {
        return random.nextInt(range + 1);
    }

    /** Returns a random number between start (inclusive) and end (inclusive). */
    static public int random(int start, int end) {
        return start + random.nextInt(end - start + 1);
    }

    /** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
    static public long random(long range) {
        return (long) (random.nextDouble() * range);
    }

    /** Returns a random number between start (inclusive) and end (inclusive). */
    static public long random(long start, long end) {
        return start + (long) (random.nextDouble() * (end - start));
    }

    /** Returns a random boolean value. */
    static public boolean randomBoolean() {
        return random.nextBoolean();
    }

    /** Returns true if a random value between 0 and 1 is less than the specified value. */
    static public boolean randomBoolean(double chance) {
        return MathUtils.random() < chance;
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
    static public double random(double start, double end) {
        return start + random.nextDouble() * (end - start);
    }

    /** Returns -1 or 1, randomly. */
    static public int randomSign() {
        return 1 | (random.nextInt() >> 31);
    }

    /**
     * Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values around zero are
     * more likely.
     * <p>
     * This is an optimized version of {@link #randomTriangular(double, double, double) randomTriangular(-1, 1, 0)}
     */
    public static double randomTriangular() {
        return random.nextDouble() - random.nextDouble(); //-V6001
    }

    /**
     * Returns a triangularly distributed random number between {@code -max} (exclusive) and {@code max} (exclusive), where values
     * around zero are more likely.
     * <p>
     * This is an optimized version of {@link #randomTriangular(double, double, double) randomTriangular(-max, max, 0)}
     *
     * @param max the upper limit
     */
    public static double randomTriangular(double max) {
        return (random.nextDouble() - random.nextDouble()) * max; //-V6001
    }

    /**
     * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where the
     * {@code mode} argument defaults to the midpoint between the bounds, giving a symmetric distribution.
     * <p>
     * This method is equivalent of {@link #randomTriangular(double, double, double) randomTriangular(min, max, (min + max) * .5f)}
     *
     * @param min the lower limit
     * @param max the upper limit
     */
    public static double randomTriangular(double min, double max) {
        return randomTriangular(min, max, (min + max) * 0.5f);
    }

    /**
     * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where values
     * around {@code mode} are more likely.
     *
     * @param min  the lower limit
     * @param max  the upper limit
     * @param mode the point around which the values are more likely
     */
    public static double randomTriangular(double min, double max, double mode) {
        double u = random.nextDouble();
        double d = max - min;
        if (u <= (mode - min) / d)
            return min + Math.sqrt(u * d * (mode - min));
        return max - Math.sqrt((1 - u) * d * (max - mode));
    }

    static public int clamp(int value, int min, int max) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    static public short clamp(short value, short min, short max) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    static public float clamp(float value, float min, float max) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    static public double clamp(double value, double min, double max) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
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
    static public boolean isZero(double value, double tolerance) {
        return Math.abs(value) <= tolerance;
    }

    /**
     * Returns true if a is nearly equal to b. The function uses the default
     * doubleing error tolerance.
     *
     * @param a the first value.
     * @param b the second value.
     */
    static public boolean isEqual(double a, double b) {
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
    static public boolean isEqual(double a, double b, double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    /**
     * Fast sqrt method. Default passes it through one round of Newton's method
     *
     * @param value The value
     * @return The square root value
     */
    static public double sqrt(double value) {
        double sqrt = Double.longBitsToDouble(((Double.doubleToLongBits(value) - (1l << 52)) >> 1) + (1l << 61));
        return (sqrt + value / sqrt) / 2.0;
    }

    /**
     * Does an exponential interpolation:
     * y = y0 + (y1-y0) * 10 ^ (exp * (x-x0)/(x1-x0))
     *
     * @param x
     * @param x0
     * @param x1
     * @param y0
     * @param y1
     * @param exp
     * @return
     */
    public static double eint(double x, double x0, double x1, double y0, double y1, double exp) {
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

        return y0 + (y1 - y0) * Math.pow(10, exp * (x - rx0) / (rx1 - rx0));
    }

    /**
     * Linear interpolation
     *
     * @param x  The value to interpolate
     * @param x0 Inferior limit to the independent value
     * @param x1 Superior limit to the independent value
     * @param y0 Inferior limit to the dependent value
     * @param y1 Superior limit to the dependent value
     * @return The interpolated value
     */
    public static double lint(double x, double x0, double x1, double y0, double y1) {
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

    public static double lerp(double x, double y, double a) {
        return x * (1.0 - a) + y * a;
    }

    /**
     * Linear interpolation
     *
     * @param x  The value to interpolate
     * @param x0 Inferior limit to the independent value
     * @param x1 Superior limit to the independent value
     * @param y0 Inferior limit to the dependent value
     * @param y1 Superior limit to the dependent value
     * @return The interpolated value
     */
    public static float lint(float x, float x0, float x1, float y0, float y1) {
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

    public static float lerp(float x, float y, float a) {
        return x * (1 - a) + y * a;
    }

    /**
     * Linear interpolation
     *
     * @param x  The value to interpolate
     * @param x0 Inferior limit to the independent value
     * @param x1 Superior limit to the independent value
     * @param y0 Inferior limit to the dependent value
     * @param y1 Superior limit to the dependent value
     * @return The interpolated value
     */
    public static float lint(long x, long x0, long x1, float y0, float y1) {
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

    public static long lerp(long x, long y, long a) {
        return x * (1 - a) + y * a;
    }

    static Vector3d aux0, aux1, aux2, aux3, aux4, aux5;

    static {
        aux0 = new Vector3d();
        aux1 = new Vector3d();
        aux2 = new Vector3d();
        aux3 = new Vector3d();
        aux4 = new Vector3d();
        aux5 = new Vector3d();
    }

    /**
     * Gets the distance from the point x0 to the line denoted by x1-x2.<br/>
     * Check <a href=
     * "http://mathworld.wolfram.com/Point-LineDistance3-Dimensional.html">this
     * link</a>
     *
     * @param x1 The first point in the line
     * @param x2 The second point in the line
     * @param x0 The point
     * @return The Euclidean distance between the line (x1, x2) and x0
     */
    public static double distancePointLine(double x1, double y1, double z1, double x2, double y2, double z2, double x0, double y0, double z0) {

        // d = mod((x0-x1).crs(x0-x2)) / mod(x2-x1)
        aux0.set(x0, y0, z0);
        aux1.set(x1, y1, z1);
        aux2.set(x2, y2, z2);

        return aux3.set(aux0).sub(aux1).crs(aux4.set(aux0).sub(aux2)).len() / aux3.set(aux2).sub(aux1).len();
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
     * @return The Euclidean distance between the segment (x1, x2)
     */
    public static double distancePointSegment(double x1, double y1, double z1, double x2, double y2, double z2, double x0, double y0, double z0) {
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

    /**
     * Gets the closest point on the line p1-p2 from p0.<br/>
     * Check <a href=
     * "http://stackoverflow.com/questions/9368436/3d-perpendicular-point-on-line-from-3d-point">
     * this link</a>.
     *
     * @param x1 The first segment delimiter.
     * @param x2 The second segment delimiter.
     * @param x0 The point.
     * @return The vector with the closest point on the line to p0
     */
    public static Vector3d getClosestPoint(double x1, double y1, double z1, double x2, double y2, double z2, double x0, double y0, double z0) {
        //        Vector3 p1 = new Vector3(x1, y1, z1);
        //        Vector3 p2 = new Vector3(x2, y2, z2);
        //        Vector3 q = new Vector3(x3, y3, z3);
        //
        //        Vector3 u = p2 - p1;
        //        Vector3 pq = q - p1;
        //        Vector3 w2 = pq - Vector3.Multiply(u, Vector3.Dot(pq, u) / u.LengthSquared);
        //
        //        Vector3 point = q - w2;
        Vector3d p1 = aux0.set(x1, y1, z1);
        Vector3d p2 = aux1.set(x2, y2, z2);
        Vector3d q = aux2.set(x0, y0, z0);

        Vector3d u = aux3.set(p2).sub(p1);
        Vector3d pq = aux4.set(q).sub(p1);
        double scale = pq.dot(u) / u.len2();
        Vector3d w2 = aux5.set(pq).sub(u.scl(scale));

        Vector3d result = new Vector3d(q);
        return result.sub(w2);
    }

    public static Vector3d getClosestPoint2(double x1, double y1, double z1, double x2, double y2, double z2, double x0, double y0, double z0, Vector3d result) {
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
     * Gets perpendicular vector to in vector
     *
     * @param in
     * @return
     */
    public static Vector3d perpendicular(Vector3d in, Vector3d out) {
        /*
        arr[0] = (c,c,-a-b) arr[1] = (-b-c, a,a)
        int selectIndex = ((c != 0) && (-a != b)) // this is not a branch
        perpendicularVector = arr[selectIndex]
        
        If (c, c, -a-b) is zero, selectIndex is 1 and the other vector will be selected.
        */
        boolean si = in.z != 0 && -in.x != in.y;
        if (si) {
            out.set(-in.y - in.z, in.x, in.x);
        } else {
            out.set(in.z, in.z, -in.x - in.y);
        }

        return out;
    }

    /**
     * Rounds the double value to a number of decimal places
     *
     * @param value  The value to round
     * @param places The number of decimal places
     * @return The rounded value
     */
    public static double roundAvoid(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}



