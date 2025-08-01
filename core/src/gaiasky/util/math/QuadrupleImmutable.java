/*

 Copyright 2021 M.Vokhmentsev

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

*/
package gaiasky.util.math;

import java.util.Arrays;

/**
 * Immutable, thread-safe version of {@link Quadruple} implemented as a Java record.
 * <p>
 * A floating-point number with a 128-bit fractional part of the mantissa and 32-bit
 * exponent. Normal values range from approximately {@code 2.271e-646456993}
 * to {@code 1.761e+646456993} with precision not worse than {@code 1,469368e-39}
 * ({@code 2^-129}, half the less significant bit).
 * <p>
 * Like standard {@code double}, it can store and process subnormal values,
 * with lower precision. The subnormal values range from {@code 6.673e-646457032}
 * to {@code 2.271e-646456993}, they use less than 129 bits of the mantissa and their
 * precision depends on the number of used bits, the less the value the worst the precision.
 * <p>
 * Implements conversions from/to other numeric types, conversions from/to strings,
 * formatting, arithmetic operations and square root.
 * <p>
 * The biased exponent values stored in the {@code exponent} field are as following:
 *
 * <table class="memberSummary" border="1">
 * <tr>
 * <th class="colLast" scope="col">biased value</th>
 * <th class="colLast" scope="col">const name</th>
 * <th class="colLast" scope="col">means</th>
 * <th class="colLast" scope="col">unbiased exponent (power of 2)</th>
 * </tr>
 * <tr class="altColor">
 * <td>{@code 0x0000_0000}</td>
 * <td>{@code EXPONENT_OF_SUBNORMAL}</td>
 * <td>subnormal values</td>
 * <td>{@code 0x8000_0001 = -2147483647 =  Integer.MIN_VALUE + 1}</td>
 * </tr>
 * <tr class="rowColor">
 * <td>{@code 0x0000_0001}</td>
 * <td>{@code EXPONENT_OF_MIN_NORMAL}</td>
 * <td>{@code MIN_NORMAL}</td>
 * <td>{@code 0x8000_0002 = -2147483646 =  Integer.MIN_VALUE + 2}</td>
 * </tr>
 * <tr class="altColor">
 * <td>{@code 0x7FFF_FFFE}</td>
 * <td>&nbsp;</td>
 * <td>{@code -1}</td>
 * <td>{@code 0xFFFF_FFFF}</td>
 * </tr>
 * <tr class="rowColor">
 * <td>{@code 0x7FFF_FFFF}</td>
 * <td>{@code EXPONENT_OF_ONE}</td>
 * <td>{@code 0}</td>
 * <td>{@code 0x0000_0000}</td>
 * </tr>
 * <tr class="altColor">
 * <td>{@code 0x8000_0000}</td>
 * <td>&nbsp;</td>
 * <td>{@code 1}</td>
 * <td>{@code 0x0000_0001}</td>
 * </tr>
 * <tr class="rowColor">
 * <td>{@code 0xFFFF_FFFE}</td>
 * <td>{@code EXPONENT_OF_MAX_VALUE}</td>
 * <td>{@code MAX_VALUE}</td>
 * <td>{@code 0x7fff_ffff =  2147483647 =  Integer.MAX_VALUE}</td>
 * </tr>
 * <tr class="altColor">
 * <td>{@code 0xFFFF_FFFF}</td>
 * <td>{@code EXPONENT_OF_INFINITY}</td>
 * <td>{@code Infinity}</td>
 * <td>{@code 0x8000_0000 =  2147483648 =  Integer.MIN_VALUE}</td>
 * </tr>
 * <caption>Quadruple constants and information.</caption>
 * </table>
 * <br>The boundaries of the range are:
 * <pre>{@code
 * MAX_VALUE:  2^2147483647 * (2 - 2^-128) =
 *             = 1.76161305168396335320749314979184028566452310e+646456993
 * MIN_NORMAL: 2^-2147483646 =
 *             = 2.27064621040149253752656726517958758124747730e-646456993
 * MIN_VALUE:  2^-2147483774 =
 *             = 6.67282948260747430814835377499134611597699952e-646457032
 * }</pre>
 * <p>
 * Original code by M. Vokhmentsev, see this <a href='https://github.com/m-vokhm/Quadruple'>repository</a>.
 *
 * @param negative The sign of the value ({@code true} signifies negative values).
 * @param exponent The binary exponent (unbiased).
 * @param mantHi   The most significant 64 bits of fractional part of the mantissa.
 * @param mantLo   The least significant 64 bits of fractional part of the mantissa.
 */
public record QuadrupleImmutable(boolean negative, int exponent, long mantHi,
                                 long mantLo) implements Comparable<QuadrupleImmutable> {

    private static final int HASH_CODE_OF_NAN = -441827835;

    /**
     * The value of the exponent (biased) corresponding to {@code 1.0 == 2^0};
     * equals to 2_147_483_647 ({@code 0x7FFF_FFFF}).
     * The same as {@link #EXPONENT_BIAS}
     */
    public static final int EXPONENT_OF_ONE = 0x7FFF_FFFF;
    /**
     * The value of the exponent (biased) corresponding to {@code 1.0 == 2^0};
     * equals to 2_147_483_647 ({@code 0x7FFF_FFFF})
     * The same as {@link #EXPONENT_OF_ONE}
     */
    public static final int EXPONENT_BIAS = 0x7FFF_FFFF;

    /**
     * The value of the exponent (biased), corresponding to {@code MAX_VALUE};
     * equals to 4_294_967_294L ({@code 0xFFFF_FFFEL})
     */
    public static final long EXPONENT_OF_MAX_VALUE = 0xFFFF_FFFEL;

    /**
     * The value of the exponent (biased), corresponding to {@code Infinity},
     * {@code -Infinity}, and {@code NaN};
     * equals to -1 ({@code 0xFFFF_FFFF})
     */
    public static final int EXPONENT_OF_INFINITY = 0xFFFF_FFFF;

    /** Value of one. **/
    public static final QuadrupleImmutable ONE = from(1d);
    /** Value of zero. **/
    public static final QuadrupleImmutable ZERO = new QuadrupleImmutable(false, 0, 0, 0);
    /** Value of negative zero. **/
    public static final QuadrupleImmutable NEGATIVE_ZERO = new QuadrupleImmutable(true, 0, 0, 0);
    /** Value of {@code +Infinity}. **/
    public static final QuadrupleImmutable POSITIVE_INFINITY = new QuadrupleImmutable(false, EXPONENT_OF_INFINITY, 0, 0);
    /** Value of {@code -Infinity}. **/
    public static final QuadrupleImmutable NEGATIVE_INFINITY = new QuadrupleImmutable(true, EXPONENT_OF_INFINITY, 0, 0);
    /** Value of {@code NaN}. **/
    public static final QuadrupleImmutable NAN = new QuadrupleImmutable(false, EXPONENT_OF_INFINITY, 0x8000_0000_0000_0000L, 0);
    /** Value of {@code PI}. **/
    public static final QuadrupleImmutable PI = new QuadrupleImmutable(0x8000_0000, 0x921f_b544_42d1_8469L,
                                                                       0x898c_c517_01b8_39a2L);

    public static QuadrupleImmutable infinity(boolean negative) {
        return negative ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
    }

    public static QuadrupleImmutable zero(boolean negative) {
        return negative ? NEGATIVE_ZERO : ZERO;
    }

    // Buffers used internally
    private static class Buffers {
        final long[] BUFFER_4x64_A = new long[4];
        final long[] BUFFER_3x64_A = new long[3];
        final long[] BUFFER_3x64_B = new long[3];
        final long[] BUFFER_3x64_C = new long[3];
        final long[] BUFFER_3x64_D = new long[3];
        final long[] BUFFER_5x32_A = new long[5];
        final int[] BUFFER_5x32_A_INT = new int[5];
        final long[] BUFFER_5x32_B = new long[5];
        final int[] BUFFER_5x32_B_INT = new int[5];
        final long[] BUFFER_6x32_A = new long[6];
        final long[] BUFFER_6x32_B = new long[6];
        final long[] BUFFER_10x32_A = new long[10];
        final int[] BUFFER_10x32_A_INT = new int[10];
        final long[] BUFFER_12x32 = new long[12];
        /**
         * The mantissa of the Sqrt(2) in a format convenient for multiplying,
         * SQRT_2_AS_LONGS[1] .. SQRT_2_AS_LONGS[3] contains the mantissa including the implied unity
         * that is in the high bit of SQRT_2_AS_LONGS[1]. The other bits contain the fractional part of the mantissa.
         * Used by multBySqrt2()
         */
        final long[] SQRT_2_AS_LONGS = new long[]{
                0, 0xb504_f333_f9de_6484L, 0x597d_89b3_754a_be9fL, 0x1d6f_60ba_893b_a84dL,
        };
    }

    /**
     * Static thread-local buffers instance. These are working buffers which need to be instantiated
     * locally in every thread.
     */
    private static final ThreadLocal<Buffers> buffers = ThreadLocal.withInitial(Buffers::new);


    private static final int[] SQUARE_BYTES = {
            //   0:
            0x0000,
            0x0201,
            0x0404,
            0x0609,
            0x0810,
            0x0a19,
            0x0c24,
            0x0e31,
            0x1040,
            0x1251,
            //  10:
            0x1464,
            0x1679,
            0x1890,
            0x1aa9,
            0x1cc4,
            0x1ee1,
            0x2100,
            0x2321,
            0x2544,
            0x2769,
            //  20:
            0x2990,
            0x2bb9,
            0x2de4,
            0x3011,
            0x3240,
            0x3471,
            0x36a4,
            0x38d9,
            0x3b10,
            0x3d49,
            //  30:
            0x3f84,
            0x41c1,
            0x4400,
            0x4641,
            0x4884,
            0x4ac9,
            0x4d10,
            0x4f59,
            0x51a4,
            0x53f1,
            //  40:
            0x5640,
            0x5891,
            0x5ae4,
            0x5d39,
            0x5f90,
            0x61e9,
            0x6444,
            0x66a1,
            0x6900,
            0x6b61,
            //  50:
            0x6dc4,
            0x7029,
            0x7290,
            0x74f9,
            0x7764,
            0x79d1,
            0x7c40,
            0x7eb1,
            0x8124,
            0x8399,
            //  60:
            0x8610,
            0x8889,
            0x8b04,
            0x8d81,
            0x9000,
            0x9281,
            0x9504,
            0x9789,
            0x9a10,
            0x9c99,
            //  70:
            0x9f24,
            0xa1b1,
            0xa440,
            0xa6d1,
            0xa964,
            0xabf9,
            0xae90,
            0xb129,
            0xb3c4,
            0xb661,
            //  80:
            0xb900,
            0xbba1,
            0xbe44,
            0xc0e9,
            0xc390,
            0xc639,
            0xc8e4,
            0xcb91,
            0xce40,
            0xd0f1,
            //  90:
            0xd3a4,
            0xd659,
            0xd910,
            0xdbc9,
            0xde84,
            0xe141,
            0xe400,
            0xe6c1,
            0xe984,
            0xec49,
            // 100:
            0xef10,
            0xf1d9,
            0xf4a4,
            0xf771,
            0xfa40,
            0xfd11,
            0xffe4,
    };

    private static final int[] ROOT_BYTES = {
            //   0:
            0x0000,
            0x0001,
            0x0002,
            0x0003,
            0x0004,
            0x0005,
            0x0006,
            0x0007,
            0x0008,
            0x0009,
            //  10:
            0x000a,
            0x000b,
            0x000c,
            0x000d,
            0x000e,
            0x000f,
            0x0010,
            0x0011,
            0x0012,
            0x0013,
            //  20:
            0x0014,
            0x0015,
            0x0016,
            0x0017,
            0x0018,
            0x0019,
            0x001a,
            0x001b,
            0x001c,
            0x001d,
            //  30:
            0x001e,
            0x001f,
            0x0020,
            0x0021,
            0x0022,
            0x0023,
            0x0024,
            0x0025,
            0x0026,
            0x0027,
            //  40:
            0x0028,
            0x0029,
            0x002a,
            0x002b,
            0x002c,
            0x002d,
            0x002e,
            0x002f,
            0x0030,
            0x0031,
            //  50:
            0x0032,
            0x0033,
            0x0034,
            0x0035,
            0x0036,
            0x0037,
            0x0038,
            0x0039,
            0x003a,
            0x003b,
            //  60:
            0x003c,
            0x003d,
            0x003e,
            0x003f,
            0x0040,
            0x0041,
            0x0042,
            0x0043,
            0x0044,
            0x0045,
            //  70:
            0x0046,
            0x0047,
            0x0048,
            0x0049,
            0x004a,
            0x004b,
            0x004c,
            0x004d,
            0x004e,
            0x004f,
            //  80:
            0x0050,
            0x0051,
            0x0052,
            0x0053,
            0x0054,
            0x0055,
            0x0056,
            0x0057,
            0x0058,
            0x0059,
            //  90:
            0x005a,
            0x005b,
            0x005c,
            0x005d,
            0x005e,
            0x005f,
            0x0060,
            0x0061,
            0x0062,
            0x0063,
            // 100:
            0x0064,
            0x0065,
            0x0066,
            0x0067,
            0x0068,
            0x0069,
            0x006a,
    };

    /**
     * Creates a new {@link QuadrupleImmutable} with a positive value built from the given parts.
     *
     * @param exponent The binary exponent (unbiased).
     * @param mantHi   The most significant 64 bits of fractional part of the mantissa.
     * @param mantLo   The least significant 64 bits of fractional part of the mantissa.
     */
    public QuadrupleImmutable(int exponent, long mantHi, long mantLo) {
        this(false, exponent, mantHi, mantLo);
    }

    /**
     * Creates a new {@link QuadrupleImmutable} instance with the value of the given {@link QuadrupleImmutable} instance.<br>
     * First creates an empty (zero) instance, then copies the fields of the parameter.
     * to the fields of the new instance
     *
     * @param value the {@link QuadrupleImmutable} value to be assigned to the new instance.
     */
    public QuadrupleImmutable(QuadrupleImmutable value) {
        this(value.negative, value.exponent, value.mantHi, value.mantLo);
    }

    /**
     * Creates a new {@link QuadrupleImmutable} instance with the given double value.
     *
     * @param value the double value to be assigned.
     */
    public static QuadrupleImmutable from(double value) {
        boolean negative;
        int exponent;
        long mantLo, mantHi;

        long doubleAsLong = Double.doubleToLongBits(value);
        negative = (doubleAsLong & DOUBLE_SIGN_MASK) != 0;
        doubleAsLong &= ~DOUBLE_SIGN_MASK;

        if (doubleAsLong == 0)
            return ZERO;

        mantLo = 0L;
        if ((doubleAsLong & DOUBLE_EXP_MASK) == 0) {
            final int numOfZeros = Long.numberOfLeadingZeros(
                    doubleAsLong);
            exponent = EXPONENT_BIAS - EXP_0D - (numOfZeros - 12);
            mantHi = 0L;
            if (numOfZeros < 63)
                // when numOfZeros == 63, since d << 64 does nothing
                mantHi = doubleAsLong << numOfZeros + 1;
            return new QuadrupleImmutable(negative, exponent, mantHi, mantLo);
        }

        if ((doubleAsLong & DOUBLE_EXP_MASK) == DOUBLE_EXP_MASK) {
            exponent = EXPONENT_OF_INFINITY;
            if ((doubleAsLong & DOUBLE_MANT_MASK) == 0)
                mantHi = 0;
            else
                mantHi = DOUBLE_SIGN_MASK;
            return new QuadrupleImmutable(negative, exponent, mantHi, mantLo);
        }

        // Normal case
        exponent = (int) ((doubleAsLong & DOUBLE_EXP_MASK) >>> 52) - EXP_0D + EXPONENT_BIAS;
        mantHi = (doubleAsLong & DOUBLE_MANT_MASK) << 12;
        return new QuadrupleImmutable(negative, exponent, mantHi, mantLo);
    }

    /**
     * Creates a new {@link QuadrupleImmutable} instance with the given long value.
     *
     * @param value the long value to be assigned.
     */
    public static QuadrupleImmutable from(long value) {
        if (value == 0)
            return ZERO;

        boolean negative;

        if (value == HIGH_BIT)
            fromUnbiasedExponent(true, 63, 0, 0);

        if (value < 0) {
            negative = true;
            value = -value;
        } else negative = false;

        // Shift the value left so that its highest non-zero bit get shifted out
        // it should become the implied unity
        final int bitsToShift = Long.numberOfLeadingZeros(value) + 1;
        value = (bitsToShift == 64) ? 0 : value << bitsToShift;
        return fromUnbiasedExponent(negative, 64 - bitsToShift, value, 0);
    }

    /**
     * Builds a Float128 value from the given low-level parts.<br>
     * Treats the {@code exponent} parameter as the unbiased exponent value,
     * whose {@code 0} value corresponds to the {@link QuadrupleImmutable} value of 1.0.
     *
     * @param negative the sign of the value ({@code true} for negative)
     * @param exponent Binary exponent (unbiased, 0 means 2^0)
     * @param mantHi   The higher 64 bits of the fractional part of the mantissa
     * @param mantLo   The lower 64 bits of the fractional part of the mantissa
     *
     * @return A Float128 containing the value built of the given parts
     */
    public static QuadrupleImmutable fromUnbiasedExponent(boolean negative, int exponent, long mantHi, long mantLo) {
        return new QuadrupleImmutable(negative, exponent + EXPONENT_BIAS, mantHi, mantLo);
    }

    private MutableBag toF128() {
        return new MutableBag(negative, exponent, mantHi, mantLo);
    }


    /**
     * Converts the value of this {@link QuadrupleImmutable} to an {@code int} value in a way
     * similar to standard narrowing conversions (e.g., from {@code double} to {@code int}).
     *
     * @return the value of this {@link QuadrupleImmutable} instance converted to an {@code int}.
     */
    public int intValue() {
        final long exp = (exponent & LOWER_32_BITS) - EXPONENT_BIAS; // Unbiased exponent
        if (exp < 0 || isNaN()) return 0;
        if (exp >= 31)                                              // value <= Integer.MIN_VALUE || value > Integer.MAX_VALUE
            return negative ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        final int intValue = exp == 0 ? 1 : (1 << exp) | (int) (mantHi >>> 64 - exp);  // implicit unity | fractional part of the mantissa, shifted rightwards
        return negative ? -intValue : intValue;
    }

    /**
     * Converts the value of this {@link QuadrupleImmutable} to a {@code long} value in a way
     * similar to standard narrowing conversions (e.g., from {@code double} to {@code long}).
     *
     * @return the value of this {@link QuadrupleImmutable} instance converted to a {@code long}.
     */
    public long longValue() {
        final long exp = (exponent & LOWER_32_BITS) - EXPONENT_BIAS;
        if (exp < 0 || isNaN()) return 0;
        if (exp >= 63)
            return negative ? Long.MIN_VALUE : Long.MAX_VALUE;

        final long longValue = exp == 0 ? 1 : (1L << exp) | (mantHi >>> 64 - exp);
        return negative ? -longValue : longValue;
    }

    /**
     * Converts the value of this {@link QuadrupleImmutable} to a {@code float} value in a way
     * similar to standard narrowing conversions (e.g., from {@code double} to {@code float}).
     *
     * @return the value of this {@link QuadrupleImmutable} instance converted to a {@code float}.
     */
    public float floatValue() {
        return (float) doubleValue();
    }

    /**
     * Converts the value of this {@link QuadrupleImmutable} to a {@code double} value in a way
     * similar to standard narrowing conversions (e.g., from {@code double} to {@code float}).
     * Uses 'half-even' approach to the rounding, like {@code BigDecimal.doubleValue()}
     *
     * @return the value of this {@link QuadrupleImmutable} instance converted to a {@code double}.
     */
    public double doubleValue() {
        if (exponent == 0)
            return negative ? -0.0d : 0.0d;

        if (exponent == EXPONENT_OF_INFINITY)
            return (mantHi != 0 || mantLo != 0) ? Double.NaN :
                    negative ?
                            Double.NEGATIVE_INFINITY :
                            Double.POSITIVE_INFINITY;

        int expD = exponent - EXPONENT_BIAS;
        if (expD > EXP_0D)
            return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;

        if (expD < -(EXP_0D + 52))
            return negative ? -0.0d : 0.0d;

        if (expD < -(EXP_0D - 1)) {
            long lValue = (mantHi >>> 12) | 0x0010_0000_0000_0000L;
            lValue = lValue + (1L << -EXP_0D - expD) >>> -EXP_0D - expD + 1;
            if (negative) lValue |= DOUBLE_SIGN_MASK;
            return Double.longBitsToDouble(lValue);
        }

        // normal case
        long dMant = mantHi >>> 12;
        if ((mantHi & HALF_DOUBLES_LSB) != 0)
            if ((((mantHi & (HALF_DOUBLES_LSB - 1)) | mantLo) != 0)
                    || (dMant & 1) != 0) {
                dMant++;
                if ((dMant & DOUBLE_EXP_MASK) != 0) {
                    dMant = (dMant & ~DOUBLE_IMPLIED_MSB) >>> 1;
                    expD++;
                }
            }

        if (expD > EXP_0D)
            return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        final long lValue = ((long) (expD + EXP_0D) << 52) | dMant | (negative ? DOUBLE_SIGN_MASK : 0);
        return Double.longBitsToDouble(lValue);
    }

    /**
     * Adds the value of the given {@link QuadrupleImmutable} summand to the value of this Float128.
     * The instance acquires a new value that equals the sum of the previous value and the value of the summand.
     *
     * @param summand the value to add
     *
     * @return the reference to this object, which holds a new value that equals
     * the sum of its previous value and the value of the summand
     */
    public QuadrupleImmutable add(final QuadrupleImmutable summand) {
        if (isNaN() || summand.isNaN()) return NAN;

        if (isInfinite()) {
            if (summand.isInfinite() && (negative != summand.negative))
                return NAN;
            else return this;
        }

        if (summand.isInfinite()) return new QuadrupleImmutable(summand);

        if (summand.isZero()) {
            if (isZero())
                if (summand.isNegative() && isNegative())
                    return new QuadrupleImmutable(true, exponent, mantHi, mantLo);
                else
                    return new QuadrupleImmutable(false, exponent, mantHi, mantLo);
        }

        if (isZero()) return new QuadrupleImmutable(summand);

        // Both are regular numbers
        var f = new MutableBag(this);
        if (negative == summand.negative)
            return f.addUnsigned(summand.toF128())
                    .toFloat128();
        else {
            f.subtractUnsigned(
                    summand.toF128());
            f.negative ^= negative;
        }
        return f.toFloat128();
    }

    /**
     * Adds the value of the given {@code long} summand to the value of this Float128.
     * The value of the {@code long} operand is preliminarily converted to a {@link QuadrupleImmutable} value.
     * The instance acquires the new value that equals the sum of the previous value and the value of the summand.
     *
     * @param summand the value to add
     *
     * @return the reference to this object, which holds a new value that equals
     * the sum of its previous value and the value of the summand
     */
    public QuadrupleImmutable add(long summand) {
        return add(from(summand));
    }

    /**
     * Adds the value of the given {@code double} summand to the value of this Float128.
     * The value of the {@code double} operand is preliminarily converted to a {@link QuadrupleImmutable} value.
     * The instance acquires the new value that equals the sum of the previous value and the value of the summand.
     *
     * @param summand the value to add
     *
     * @return the reference to this object, which holds a new value that equals
     * the sum of its previous value and the value of the summand
     */
    public QuadrupleImmutable add(double summand) {
        return add(from(summand));
    }

    /**
     * Subtracts the value of the given {@link QuadrupleImmutable} subtrahend from the value of this Float128.
     * The instance acquires a new value that equals the difference between the previous value and the value of the subtrahend.
     *
     * @param subtrahend the value to be subtracted from the current value of this Float128
     *
     * @return the reference to this object, which holds a new value that equals
     * the difference between its previous value and the value of the subtrahend
     */
    public QuadrupleImmutable subtract(QuadrupleImmutable subtrahend) {
        if (isNaN() || subtrahend.isNaN()) return NAN;

        if (isInfinite()) {
            if (subtrahend.isInfinite() && (negative == subtrahend.negative))
                return NAN;
            else return this;
        }

        if (subtrahend.isInfinite())
            return new QuadrupleImmutable(!subtrahend.negative, subtrahend.exponent, subtrahend.mantHi, subtrahend.mantLo);

        if (subtrahend.isZero()) {
            var negative = this.negative;
            if (isZero()) {
                if (!isNegative() || subtrahend.isNegative())
                    negative = false;
            }
            return new QuadrupleImmutable(negative, exponent, mantHi, mantLo);                                      // X - 0 = X
        }

        if (isZero())
            return new QuadrupleImmutable(!subtrahend.negative, subtrahend.exponent, subtrahend.mantHi, subtrahend.mantLo);

        // Both are regular numbers
        MutableBag f = this.toF128();
        if (negative != subtrahend.negative)
            return f.addUnsigned(subtrahend.toF128())
                    .toFloat128();
        else {
            // same sign
            f.subtractUnsigned(
                    subtrahend.toF128());
            f.negative ^= negative;
        }
        return f.toFloat128();
    }

    /**
     * Subtracts the value of the given {@code long} subtrahend from the value of this Float128.
     * The value of the {@code long} subtrahend is preliminarily converted to a {@link QuadrupleImmutable} value.
     * The instance acquires a new value that equals the difference between the previous value and the value of the subtrahend.
     *
     * @param subtrahend the value to be subtracted from the current value of this Float128
     *
     * @return the reference to this object, which holds a new value that equals
     * the difference between its previous value and the value of the subtrahend
     */
    public QuadrupleImmutable subtract(long subtrahend) {
        return subtract(from(subtrahend));
    }

    /**
     * Subtracts the value of the given {@code double} subtrahend from the value of this Float128.
     * The value of the {@code double} subtrahend is preliminarily converted to a {@link QuadrupleImmutable} value.
     * The instance acquires a new value that equals the difference between the previous value and the value of the subtrahend.
     *
     * @param subtrahend the value to be subtracted from the current value of this Float128
     *
     * @return the reference to this object, which holds a new value that equals
     * the difference between its previous value and the value of the subtrahend
     */
    public QuadrupleImmutable subtract(double subtrahend) {
        return subtract(from(subtrahend));
    }

    /**
     * Multiplies the value of this Float128 by the value of the given {@link QuadrupleImmutable} factor.
     * The instance acquires a new value that equals the product of the previous value and the value of the factor.
     *
     * @param factor the value to multiply the current value of this Float128 by.
     *
     * @return the reference to this object, which holds a new value that equals
     * the product of its previous value and the value of the factor
     */
    public QuadrupleImmutable multiply(QuadrupleImmutable factor) {
        if (isNaN() || factor.isNaN()) return NAN;

        if (isInfinite()) {
            if (factor.isZero()) return NAN;
            return infinity(factor.negative);
        }

        if (isZero()) {
            if (factor.isInfinite()) return NAN;
            return zero(factor.negative);
        }

        // This is a normal number, non-zero, non-infinity, and factor != NaN
        if (factor.isInfinite()) return infinity(factor.negative);
        if (factor.isZero()) return zero(factor.negative);

        // Both are regular numbers
        var f = new MutableBag(this);
        f.multUnsigned(factor);
        f.negative ^= factor.negative;
        return f.toFloat128();
    }

    /**
     * Multiplies the value of this Float128 by the value of the given {@code long} factor.
     * The value of the {@code long} factor is preliminarily converted to a {@link QuadrupleImmutable} value.
     * The instance acquires a new value that equals the product of the previous value and the value of the factor.
     *
     * @param factor the value to multiply the current value of this Float128 by.
     *
     * @return the reference to this object, which holds a new value that equals
     * the product of its previous value and the value of the factor
     */
    public QuadrupleImmutable multiply(long factor) {
        return multiply(from(factor));
    }

    /**
     * Multiplies the value of this Float128 by the value of the given {@code double} factor.
     * The value of the {@code double} factor is preliminarily converted to a {@link QuadrupleImmutable} value.
     * The instance acquires a new value that equals the product of the previous value and the value of the factor.
     *
     * @param factor the value to multiply the current value of this Float128 by.
     *
     * @return the reference to this object, which holds a new value that equals
     * the product of its previous value and the value of the factor
     */
    public QuadrupleImmutable multiply(double factor) {
        return multiply(from(factor));
    }

    /**
     * Divides the value of this Float128 by the value of the given {@link QuadrupleImmutable} divisor.
     * The instance acquires a new value that equals the quotient.
     *
     * @param divisor the divisor to divide the current value of this Float128 by
     *
     * @return the reference to this object, which holds a new value that equals
     * the quotient of the previous value of this Float128 divided by the given divisor
     */
    public QuadrupleImmutable divide(QuadrupleImmutable divisor) {
        if (isNaN() || divisor.isNaN()) return NAN;

        if (isInfinite()) {
            if (divisor.isInfinite()) return NAN;
            return infinity(divisor.negative);
        }

        if (isZero()) {
            if (divisor.isZero()) return NAN;
            return zero(divisor.negative);
        }

        // This is a normal number, not a zero, not an infinity, and divisor != NaN
        if (divisor.isInfinite())
            return zero(divisor.negative);

        if (divisor.isZero())
            return infinity(divisor.negative);

        // Both are regular numbers, do divide
        var f = new MutableBag(this);
        f.divideUnsigned(divisor.toF128());

        f.negative ^= divisor.negative;
        return f.toFloat128();
    }

    /**
     * Divides the value of this Float128 by the value of the given {@code long} divisor.
     * The instance acquires a new value that equals the quotient.
     * The value of the {@code long} divisor is preliminarily converted to a {@link QuadrupleImmutable} value.
     *
     * @param divisor the divisor to divide the current value of this Float128 by
     *
     * @return the reference to this object, which holds a new value that equals
     * the quotient of the previous value of this Float128 divided by the given divisor
     */
    public QuadrupleImmutable divide(long divisor) {
        return divide(from(divisor));
    }

    /**
     * Divides the value of this Float128 by the value of the given {@code double} divisor.
     * The instance acquires a new value that equals the quotient.
     * The value of the {@code double} divisor is preliminarily converted to a {@link QuadrupleImmutable} value.
     *
     * @param divisor the divisor to divide the current value of this Float128 by
     *
     * @return the reference to this object, which holds a new value that equals
     * the quotient of the previous value of this Float128 divided by the given divisor
     */
    public QuadrupleImmutable divide(double divisor) {
        return divide(from(divisor));
    }

    /**
     * Computes a square root of the value of this {@link QuadrupleImmutable}
     * and replaces the old value of this instance with the newly-computed value.
     *
     * @return the reference to this instance, which holds a new value that equals
     * to the square root of its previous value
     */
    public QuadrupleImmutable sqrt() {
        if (negative) return NAN;
        if (isNaN() || isInfinite()) return this;

        var f = new MutableBag(this);
        long absExp = (exponent & LOWER_32_BITS) - EXPONENT_BIAS;
        if (f.exponent == 0)
            absExp -= f.normalizeMantissa();
        f.exponent = (int) (absExp / 2 + EXPONENT_BIAS);

        long thirdWord = f.sqrtMant();                        // puts 128 bit of the root into mantHi, mantLo
        // and returns additional 64 bits of the root

        if (absExp % 2 != 0) {                              // Exponent is odd,
            final long[] multed = multBySqrt2(f.mantHi, f.mantLo,
                                              thirdWord); // multiply this value by sqrt(2), fill mantissa with the new value
            f.mantHi = multed[0];
            f.mantLo = multed[1];
            thirdWord = multed[2];
            if (absExp < 0)
                f.exponent--;                       // for negative odd powers of two, exp = floor(exp / 2), e.g sqrt(0.64) = 0.8, sqrt(0.36) = 0.6
        }

        if ((thirdWord & HIGH_BIT) != 0)                    // The rest of the root >= a half of the lowest bit, round up
            if (++f.mantLo == 0)
                if (++f.mantHi == 0)
                    f.exponent++;        // 21.01.08 18:04:02: Actually, this branch can never be executed,
        // since derivative of sqrt(x) at point x = 4 equals 1/4
        // and is less than 1/4 if x < 4, so
        // sqrt(-1, -1, EXP_0Q + 1) is a little more than (-1, -1, EXP_0Q),
        // but less than (-1, -1, EXP_0Q) + 0.5 LSB, so gets rounded down to (-1, -1, EXP_0Q)
        // (0xFFFF_FFFF_FFFF_FFFFL, 0xFFFF_FFFF_FFFF_FFFFL, and no carry to the position of the implicit unity).
        // Nevertheless let it remain as a safety net


        return f.toFloat128();
    }

    /**
     * Computes a square root of the value of the given {@link QuadrupleImmutable},
     * creates and returns a new instance of Float128 containing the value of the square root.
     * The parameter remains unchanged.
     *
     * @param square the value to find the square root of
     *
     * @return a new instance of Float128 containing the value of the square root of the given argument
     */
    public static QuadrupleImmutable sqrt(QuadrupleImmutable square) {
        return square.sqrt();
    }

    /**
     * Bean used to transport a mutable state in some operations.
     */
    private static class MutableBag {
        boolean negative;
        int exponent;
        long mantHi, mantLo;

        private static final MutableBag ONE = new MutableBag(QuadrupleImmutable.ONE);

        public MutableBag(boolean negative, int exponent, long mantHi, long mantLo) {
            this.negative = negative;
            this.exponent = exponent;
            this.mantHi = mantHi;
            this.mantLo = mantLo;
        }

        public MutableBag(QuadrupleImmutable f) {
            this(f.negative, f.exponent, f.mantHi, f.mantLo);
        }

        public QuadrupleImmutable toFloat128() {
            return new QuadrupleImmutable(negative, exponent, mantHi, mantLo);
        }

        /**
         * Checks if the value is negative.
         *
         * @return {@code true}, if the value is negative, {@code false} otherwise
         */
        public boolean isNegative() {
            return negative;
        }

        /**
         * Checks if the value is infinite (i.e {@code NEGATIVE_INFINITY} or {@code POSITIVE_INFINITY}).
         *
         * @return {@code true}, if the value is infinity (either positive or negative), {@code false} otherwise
         */
        public boolean isInfinite() {
            return (exponent == EXPONENT_OF_INFINITY) && ((mantHi | mantLo) == 0);
        }

        /**
         * Checks if the value is not a number (i.e. has the value of {@code NaN}).
         *
         * @return {@code true}, if the value is not a number (NaN), {@code false} otherwise
         */
        public boolean isNaN() {
            return (exponent == EXPONENT_OF_INFINITY) && ((mantHi | mantLo) != 0);
        }

        /**
         * Assigns the value of +0 to this instance.
         */
        private void assignZero() {
            negative = false;
            mantHi = mantLo = exponent = 0;
        }

        /**
         * Assigns the value of -0 to this instance.
         *
         * @return this instance with the new value (-0)
         */
        private MutableBag assignMinusZero() {
            negative = true;
            mantHi = mantLo = exponent = 0;
            return this;
        }

        /**
         * Assigns the value of +1 or -1 to this instance,
         * depending on the sign of the previous value of the instance and the {@code changeSign} parameter.
         */
        private void assignOne() {
            exponent = EXPONENT_OF_ONE;
            mantHi = 0;
            mantLo = 0;
        }

        /**
         * Assigns the value of Infinity or -Infinity,
         * depending on the sign of the previous value of the instance and the {@code changeSign} parameter.
         */
        private void assignInfinity() {
            exponent = EXPONENT_OF_INFINITY;
            mantHi = 0;
            mantLo = 0;
        }


        /**
         * For a Float128 with a normal mantissa (with implied unity)
         * and non-positive biased exponent, returns a copy of the conventional subnormal form, with the exponent = 0
         * and the mantissa shifted rightwards with explicit 1 in the appropriate position.<br>
         * Shifts mantissa rightwards by |exp2| + 1 bits, sets explicit 1, and rounds it up, taking into account the bits having been shifted-out
         *
         * @param exp2 the exponent of the newly-found subnormal value (always negative)
         *
         * @return This Float128 in subnormal form.
         */
        private long makeSubnormal(long exp2) {
            exp2 = -exp2;
            if (exp2 > 127) {
                mantLo = mantHi = 0;
                if (exp2 == 128) mantLo++;
                return 0;
            }

            final long shiftedOutBit = shiftMantissa(exp2);

            exp2 = 0;
            if (shiftedOutBit != 0)
                if (++mantLo == 0 && ++mantHi == 0)
                    exp2++;

            return exp2;
        }

        /**
         * Shifts the mantissa by exp2 + 1 bits rightwards, to make a conventional subnormal value
         *
         * @param exp2 unbiased exponent of the value (negated)
         *
         * @return the highest bit that has been shifted out beyond the two longs of mantissa (1L if it was 1, 0 otherwise)
         */
        private long shiftMantissa(long exp2) {
            long shiftedOut = mantLo & 1;                // The highest of shifted out bits to evaluate carry
            mantLo = (mantLo >>> 1) | (mantHi << 63);
            mantHi = (mantHi >>> 1) | HIGH_BIT;          // move 1 bit right and set unity that was implied

            if (exp2 >= 64) {                            // the higher word move into the lower
                if (exp2 == 64)
                    shiftedOut = mantLo >>> 63;              // former lowest bit of mantHi now is the highest bit of mantLo
                else
                    shiftedOut = (mantHi >>> (exp2 - 65)) & 1; // one of the bits of the high word
                mantLo = mantHi >>> exp2 - 64;
                mantHi = 0;
            } else if (exp2 > 0) {                      // Shift both words
                shiftedOut = (mantLo >>> exp2 - 1) & 1;
                mantLo = (mantLo >>> exp2) | (mantHi << 64 - exp2);
                mantHi = mantHi >>> exp2;
            }
            return shiftedOut;
        }

        /**
         * Adds a regular number (not NaN, not Infinity) to this instance, that also contains a regular number.
         * The signs are ignored and don't change (both summands are expected to have the same sign).
         *
         * @param summand a Float128 to add to this instance
         *
         * @return this instance with the new value (the sum of the two summands)
         */
        private MutableBag addUnsigned(final MutableBag summand) {
            if (exponent != 0 && summand.exponent != 0) {
                if (exponent == summand.exponent)
                    return addWithSameExps(summand);
                return addWitDifferentExps(summand);
            }
            // At least one of the summands is subnormal
            if ((exponent | summand.exponent) != 0)
                return addNormalAndSubnormal(summand);

            // Both are subnormals. It's the simplest case
            exponent = (int) addMant(summand.mantHi, summand.mantLo);
            return this;
        }

        /**
         * Adds a summand to this instance in case when both summands are normal
         * and have the same exponent
         *
         * @param summand a Float128 to add to this instance
         *
         * @return this instance with the new value (the sum of the two summands)
         */
        private MutableBag addWithSameExps(final MutableBag summand) {
            final long carryUp = addMant(summand.mantHi, summand.mantLo);
            final long shiftedOutBit = mantLo & 1;
            shiftMantissaRight(1);

            if (shiftedOutBit != 0 && ++mantLo == 0)
                mantHi++;

            if (carryUp != 0) mantHi |= BIT_63;
            if (++exponent == EXPONENT_OF_INFINITY)
                mantHi = mantLo = 0;
            return this;
        }

        /**
         * Adds a summand to this instance
         * in case when both summands and this are normal and have different exponents
         *
         * @param summand a Float128 to add to this instance
         *
         * @return this instance with the new value (the sum of the two summands)
         */
        private MutableBag addWitDifferentExps(final MutableBag summand) {
            long greaterHi, greaterLo, exp2;

            if (Integer.compareUnsigned(exponent, summand.exponent) < 0) {
                greaterHi = summand.mantHi;
                greaterLo = summand.mantLo;
                exp2 = exponent;
                exponent = summand.exponent;
            } else {
                greaterHi = mantHi;
                greaterLo = mantLo;
                mantHi = summand.mantHi;
                mantLo = summand.mantLo;
                exp2 = summand.exponent;
            }

            final int shift = exponent - (int) exp2;
            if (Integer.compareUnsigned(shift,
                                        129) > 0) {
                mantHi = greaterHi;
                mantLo = greaterLo;
                return this;
            }

            if (shift == 129)
                return greaterPlusLowerBit(greaterHi, greaterLo);

            final long shiftedOutBit = shiftAndSetUnity(shift);
            final long carryUp = addAndRoundUp(greaterHi, greaterLo, shiftedOutBit);
            if (carryUp != 0)
                shiftAndCorrectExponent(shiftedOutBit);

            return this;
        }

        /**
         * Adds a summand to this instance in case when exactly one of the summands is subnormal
         *
         * @param summand a Float128 to add to this instance
         *
         * @return this instance with the new value (the sum of the two summands)
         */
        private MutableBag addNormalAndSubnormal(final MutableBag summand) {
            long greaterHi;
            long greaterLo;
            long shiftedOutBit;
            // Put the subnormal mantissa, that will be shifted, into the instance fields,
            // the mantissa of the greater (normal) value into local variables,
            // And the exponent of the normal value in the exponent field of this
            if (exponent == 0) {
                greaterHi = summand.mantHi;
                greaterLo = summand.mantLo;
                exponent = summand.exponent;
            } else {
                greaterHi = mantHi;
                greaterLo = mantLo;
                mantHi = summand.mantHi;
                mantLo = summand.mantLo;
            }

            final int shift = exponent - 1;
            int lz = Long.numberOfLeadingZeros(mantHi);
            if (lz == 64) lz = 64 + Long.numberOfLeadingZeros(mantLo);
            if (shift + lz > 128) {
                mantHi = greaterHi;
                mantLo = greaterLo;
                return this;
            }

            shiftedOutBit = highestShiftedOutBit(shift);
            shiftMantissaRight(shift);
            final long carryUp = addAndRoundUp(greaterHi, greaterLo, shiftedOutBit);

            if (carryUp != 0)
                shiftAndCorrectExponent(shiftedOutBit);

            return this;
        }

        /**
         * Adds the given 128-bit value to the mantissa of this instance
         *
         * @param mantHi2 the higher 64 bits of the 128-bit summand to be added
         * @param mantLo2 the lower 64 bits of the 128-bit summand to be added
         *
         * @return the carry (1 if the addition has resulted in overflow, 0 otherwise)
         */
        private long addMant(long mantHi2, long mantLo2) {
            mantLo += mantLo2;
            long carry = Long.compareUnsigned(mantLo, mantLo2) < 0 ? 1 : 0;
            if (carry != 0 && (mantHi += carry) == 0) {
                mantHi = mantHi2;
            } else {
                mantHi += mantHi2;
                carry = Long.compareUnsigned(mantHi, mantHi2) < 0 ? 1 : 0;
            }
            return carry;
        }

        /**
         * Shifts the mantissa of this instance by {@code shift} bits to the right,
         * without setting the implicit unity,
         * and returns the bits of the former mantissa that don't fit in 128 bits after the shift (shifted-out bits).
         * (e.g. if the value of {@code shift} was 3, the lowest 3 bits of {@code mantLo} will be returned in bits 63-61 of the result, the other bits will be 0)
         *
         * @param shift the distance to shift the mantissa
         *
         * @return the bits of mantissa that was shifted out
         */
        private long shiftMantissaRight(int shift) {
            long shiftedOutBits;
            if (shift == 0) return 0L;

            if (shift == 128) {
                shiftedOutBits = mantHi;
                mantHi = mantLo = 0;
                return shiftedOutBits;
            }

            shiftedOutBits = (shift <= 64) ?
                    (mantLo << 64 - shift) :
                    (mantHi << 128 - shift | mantLo >>> shift - 64);

            if (shift >= 64) {
                mantLo = mantHi >>> shift - 64;
                mantHi = 0;
            } else {
                mantLo = mantLo >>> shift | mantHi << (64 - shift);
                mantHi = mantHi >>> shift;
            }
            return shiftedOutBits;
        }

        /**
         * Increments the value passed in the parameters and assigns it to the mantissa.
         * If the mantissa becomes 0 (that indicates its overflow), increments the exponent.
         *
         * @param greaterHi the upper 64 bits of the given value
         * @param greaterLo the lower 64 bits of the given value
         *
         * @return this instance with the new value
         */
        private MutableBag greaterPlusLowerBit(long greaterHi, long greaterLo) {
            if ((mantLo = ++greaterLo) == 0) {
                if ((mantHi = ++greaterHi) == 0)
                    exponent++;                    // If it becomes infinity, the mantissa is already 0
            } else
                mantHi = greaterHi;
            return this;
        }

        /**
         * Shifts the mantissa of this instance by {@code shift} bits right
         * and sets the implicit unity in the correspondent position
         *
         * @param shift the distance to shift the mantissa
         *
         * @return the value of the highest bit that was shifted out
         */
        private long shiftAndSetUnity(int shift) {
            final long shiftedOutBit = (shift == 0) ? 0 :
                    (shift <= 64) ? 1 & (mantLo >>> shift - 1) :
                            1 & (mantHi >>> shift - 65);
            shiftMantissaRight(shift);

            if (shift > 64) mantLo |= 1L << (128 - shift);
            else mantHi |= 1L << (64 - shift);

            return shiftedOutBit;
        }

        /**
         * Adds the given 128-bit value to the mantissa, taking into account the carry from
         * the lower part of the summand (that may have been shifted out beforehand)
         *
         * @param summandHi the higher part of the 128-bit summand
         * @param summandLo the lower part of the 128-bit summand
         * @param carry     the carry from the lower bits (maybe 0 or 1)
         *
         * @return the carry (1 if the addition has led to overflow, 0 otherwise)
         */
        private long addAndRoundUp(long summandHi, long summandLo, long carry) {
            if (carry != 0 && (mantLo += carry) == 0) {
                mantLo = summandLo;
                carry = 1;
            } else {
                mantLo += summandLo;
                carry = Long.compareUnsigned(mantLo, summandLo) < 0 ? 1 : 0;
            }
            if (carry != 0 && (mantHi += carry) == 0) {
                mantHi = summandHi;
            } else {
                mantHi += summandHi;
                carry = Long.compareUnsigned(mantHi, summandHi) < 0 ? 1 : 0;
            }
            return carry;
        }

        /**
         * Shifts the mantissa one bit right and rounds it up (unless rounding is forbidden by non-null value
         * of the {@code dontRoundUpAnyMore} parameter) and increments the exponent.
         * If the exponent becomes equal to {@code EXPONENT_OF_INFINITY}, clears the mantissa to force Infinity.
         *
         * @param dontRoundUpAnyMore non-zero value
         */
        private void shiftAndCorrectExponent(long dontRoundUpAnyMore) {
            final long shiftedOutBit = dontRoundUpAnyMore != 0 ? 0 : (mantLo & 1);
            shiftMantissaRight(1);
            if (shiftedOutBit != 0)
                addMant(0, 1);
            if (++exponent == EXPONENT_OF_INFINITY)
                mantHi = mantLo = 0;
        }

        /**
         * Returns the highest bit of the mantissa that will be shifter out
         * during shift right by {@code shift} bits
         *
         * @param shift the distance the value will be shifted
         *
         * @return 1, if the highest shifted-out bit is 1, 0 otherwise
         */
        private long highestShiftedOutBit(int shift) {
            if (shift == 0) return 0;
            if (shift <= 64) return 1 & (mantLo >>> shift - 1);
            return 1 & (mantHi >>> shift - 65);
        }

        /**
         * Subtracts a regular number (Non-NaN, non-infinity) from this instance, ignoring sings.
         * returns a negative value of the difference if the subtrahend is greater in magnitude than the minuend (this),
         * and a positive one otherwise.
         *
         * @param subtrahend the value to be subtracted
         */
        private void subtractUnsigned(final MutableBag subtrahend) {
            long minuendLo, minuendHi;
            int lesserExp;

            final int thisIsGreater = compareMagnitudeTo(subtrahend); // ignores signs
            if (thisIsGreater == 0)                             // operands are equal in magnitude
            {
                assignZero();
                return;
            }

            // Swap minuend and subtrahend, if minuend is less in magnitude than subtrahend
            // so that this.mantHi, this.mantLo and lesserExp contain respectively mantissa and exponent
            // of the subtrahend (the lesser of the operands),
            // and minuendHi, minuendLo and this.exponent contain ones of the minuend (the greater one)
            if (thisIsGreater > 0) {
                minuendLo = mantLo;
                minuendHi = mantHi;
                mantHi = subtrahend.mantHi;
                mantLo = subtrahend.mantLo;
                lesserExp = subtrahend.exponent;
                negative = false;
            } else {
                minuendLo = subtrahend.mantLo;
                minuendHi = subtrahend.mantHi;
                lesserExp = exponent;
                exponent = subtrahend.exponent;
                negative = true;
            }

            if (exponent != 0 && lesserExp != 0) {
                subtractNormals(minuendLo, minuendHi, lesserExp);
                return;
            }

            if ((exponent | lesserExp) == 0) {
                subtractSubnormals(minuendLo, minuendHi);
                return;
            }

            subtractSubnormalFromNormal(minuendLo, minuendHi);
        }

        /**
         * Compares the magnitude (absolute value) of this instance
         * with the magnitude of the other instance.
         *
         * @param other the Float128 to compare with
         *
         * @return 1 if this instance is greater in magnitude than the {@code other} instance,
         * 0 if the argument is equal in magnitude to this instance, -1 if this instance is less in magnitude, than the argument
         */
        public int compareMagnitudeTo(final MutableBag other) {
            // 20.10.24 18:44:39 Regarding NaNs, behave like doubles
            if (isNaN())
                return other.isNaN() ? 0 : 1;
            if (other.isNaN())
                return -1;

            if (isInfinite())
                return other.isInfinite() ? 0 : 1;
            if (other.isInfinite())
                return -1;

            int result;
            if ((result = Integer.compareUnsigned(exponent, other.exponent)) != 0)
                return result;
            if ((result = Long.compareUnsigned(mantHi, other.mantHi)) != 0)
                return result;
            return Long.compareUnsigned(mantLo, other.mantLo);
        }

        /**
         * Subtracts a normal value, whose mantissa is contained by this
         * instance and exponent is passed in as the {@code lesserExp} parameter, from another
         * normal value, whose mantissa is contained in {@code minuendLo} and {@code minuendLo}
         * parameters and exponent is in the {@code exponent} field of this instance.
         *
         * @param minuendLo the lower 64 bits of the minuend
         * @param minuendHi the higher 64 bits of the minuend
         * @param lesserExp the exponent of the subtrahend
         */
        private void subtractNormals(long minuendLo, long minuendHi, int lesserExp) {
            final int shift = exponent - lesserExp;
            if (shift > 130) {
                mantHi = minuendHi;
                mantLo = minuendLo;
                return;
            }

            if (shift == 130) {
                subtract_1e_130(minuendLo, minuendHi);
                return;
            }

            if (shift == 129) {
                subtract_1e_129(minuendLo, minuendHi);
                return;
            }

            if (shift != 0) {
                subtractDifferentExp(minuendLo, minuendHi, shift);
                return;
            }

            subtractSameExp(minuendLo, minuendHi);
        }

        /**
         * Subtracts a value whose exponent is lower than the exponent of the minuend by 130.
         * The only case when the subtrahend affects the result is
         * when the minuend is 2^n and the subtrahend is greater than 2^(n-130).
         * The result is 1.FF..FF * 2^(n-1) in this case, otherwise it equals the minuend.
         * The result is assigned to this instance.
         *
         * @param minuendLo the lower 64 bits of the minuend
         * @param minuendHi the higher 64 bits of the minuend
         */
        private void subtract_1e_130(long minuendLo, long minuendHi) {
            if ((mantHi | mantLo) != 0 && (minuendLo | minuendHi) == 0) {
                mantHi = mantLo = -1;
                exponent--;
            } else {
                mantHi = minuendHi;
                mantLo = minuendLo;
            }
        }

        /**
         * Subtracts unity from the LSB of the mantissa of the minuend passed in,
         * if the subtrahend is greater than 1/2 LSB of the minuend.
         * if the minuend == 2^n (1.00..00*2^n), the result is 1.FF..FF * 2^(n-1) in case if the subtrahend <= 3/4 LSB,
         * and 1.FF..FE * 2^(n-1) in case if the subtrahend > 3/4 LSB.
         * Assigns the result to the {@code mantHi, mantLo} fields of this instance
         *
         * @param minuendLo the lower 64 bits of the minuend
         * @param minuendHi the higher 64 bits of the minuend
         */
        private void subtract_1e_129(long minuendLo, long minuendHi) {
            final long subtrahendHi = mantHi, subtrahendLo = mantLo;

            if ((minuendHi | minuendLo) == 0) {
                mantHi = mantLo = -1;
                if (((subtrahendHi & HIGH_BIT) != 0)
                        && (((subtrahendHi & ~HIGH_BIT) | subtrahendLo) != 0))
                    mantLo--;
                exponent--;
            } else {
                mantLo = minuendLo;
                mantHi = minuendHi;
                if ((subtrahendHi | subtrahendLo) != 0)
                    if (--mantLo == -1) mantHi--;
            }
        }

        /**
         * Subtracts a normal value, whose mantissa is contained by this instance
         * and exponent is less than exponent of {@code this} by the amount
         * passed in the {@code shift} parameter, from another normal value,
         * whose mantissa is contained in {@code minuendLo} and {@code minuendLo} parameters
         * and exponent is contained in the {@code exponent} field of this instance
         *
         * @param minuendLo the lower 64 bits of the mantissa of the minuend
         * @param minuendHi the higher 64 bits of the mantissa of the minuend
         * @param shift     the difference between the exponents
         */
        private void subtractDifferentExp(long minuendLo, long minuendHi, int shift) {
            final long shiftedOutBits = shiftMantissaRight(
                    shift);
            setUnity(shift);
            long borrow =
                    Long.compareUnsigned(shiftedOutBits, HIGH_BIT) > 0 ? 1 : 0;
            borrow = subtrMant(minuendHi, minuendLo, borrow);

            if (borrow != 0) {
                if (shift == 1)
                    normalizeShiftedByOneBit(shiftedOutBits);
                else
                    normalizeShiftedByAFewBits(
                            shiftedOutBits);
            } else if ((mantHi | mantLo) == 0 && shiftedOutBits == HIGH_BIT) {
                exponent--;
                mantHi = mantLo = 0xFFFF_FFFF_FFFF_FFFFL;
            }
        }

        /**
         * Normalizes the mantissa after subtraction, in case when subtrahend was shifted right by one bit.
         * There may be zeros in higher bits of the result, so we shift the mantissa left by (leadingZeros + 1) bits
         * and take into account borrow that possibly has to be propagated from the shifted-out bit of the subtrahend.
         *
         * @param shiftedOutBits the bits that were shifted out (in the leftmost position of the parameter)
         *                       <br>Covered
         */
        private void normalizeShiftedByOneBit(long shiftedOutBits) {
            int lz = numberOfLeadingZeros();
            if (lz == 0) {
                shiftMantLeft(1);
                exponent--;
                if (shiftedOutBits != 0)
                    if (--mantLo == -1 && --mantHi == -1) {
                        if (--exponent != 0)
                            shiftMantLeft(1);
                    }
            } else {
                if ((shiftedOutBits != 0)) {
                    shiftMantLeft(1);
                    exponent--;
                    if (--mantLo == -1)
                        mantHi--;
                    lz = numberOfLeadingZeros();
                }
                normalize(lz + 1);
            }
        }

        /**
         * Normalizes the mantissa after subtraction, in case when subtrahend was shifted right
         * by more than one bit. The highest bit of mantHi is always 1 (mantHi & HIGH_BIT != 0),
         * for rounding the result, take into account more shifted-out bits, than just the highest of them.
         *
         * @param shiftedOutBits the bits that were shifted out (in the leftmost position of the parameter)
         *                       <br>Covered
         */
        private void normalizeShiftedByAFewBits(long shiftedOutBits) {
            shiftMantLeft(1);
            exponent--;
            if (shiftedOutBits == HIGH_BIT || shiftedOutBits > 0x4000_0000_0000_0000L) {
                if (--mantLo == -1)
                    mantHi--;
            } else if (shiftedOutBits <= 0xC000_0000_0000_0000L)
                mantLo |= 1;

        }

        /**
         * Normalizes the result of subtraction,
         * shifting the mantissa leftwards by {@code shift} bits i case when the result remains normal,
         * or by {@code exponent - 1}, when the result becomes subnormal, and correcting the exponent appropriately.
         *
         * @param shift the distance to shift the mantissa and the amount to decrease the exponent by
         *              <br>Covered
         */
        private void normalize(int shift) {
            if (Integer.compareUnsigned(exponent, shift) > 0) { // Remains normal
                shiftMantLeft(shift);
                exponent -= shift;
            } else {                                            // becomes subnormal
                if (exponent > 1)
                    shiftMantLeft(exponent - 1);
                exponent = 0;
            }
        }

        /**
         * Calculates the number of leading zeros in the mantissa
         *
         * @return the number of leading zeros in the mantissa
         * <br>Covered
         */
        private int numberOfLeadingZeros() {
            int lz = Long.numberOfLeadingZeros(mantHi);
            if (lz == 64) lz += Long.numberOfLeadingZeros(mantLo);
            return lz;
        }

        /**
         * Subtracts subtrahend, whose mantissa in the {@code mantHi, mantLo} fields,
         * from the minuend passed in the parameters, when the exponents of the subtrahend and the minuend are equal.
         */
        private void subtractSameExp(long minuendLo, long minuendHi) {
            mantLo = minuendLo - mantLo;
            if (Long.compareUnsigned(minuendLo, mantLo) < 0)
                minuendHi--;
            mantHi = minuendHi - mantHi;

            // The implicit unity is always 0 after this subtraction (1.xxx - 1.yyy = 0.zzz), so normalization is always needed
            normalize(numberOfLeadingZeros() + 1);
        }

        /**
         * Subtracts a subnormal value, whose mantissa is contained by this instance,
         * from a normal value, whose mantissa is contained in {@code minuendLo} and {@code minuendLo} parameters
         * and exponent is contained in the {@code exponent} field of this instance
         *
         * @param minuendLo the lower 64 bits of the mantissa of the minuend
         * @param minuendHi the higher 64 bits of the mantissa of the minuend
         */
        private void subtractSubnormalFromNormal(long minuendLo, long minuendHi) {
            final int shift = exponent - 1;
            final int lz = numberOfLeadingZeros();

            if (((shift & 0xFFFF_FF00) != 0) || (shift + lz > 129)) {
                mantHi = minuendHi;
                mantLo = minuendLo;
                return;
            }

            final long shiftedOutBits = shiftMantissaRight(
                    shift);
            long borrow = Long.compareUnsigned(shiftedOutBits, HIGH_BIT) > 0 ? 1 : 0;

            borrow = subtrMant(minuendHi, minuendLo, borrow);
            if (borrow != 0) {
                if (shift == 1)
                    normalizeShiftedByOneBit(shiftedOutBits);
                else if (shift != 0)
                    normalizeShiftedByAFewBits(shiftedOutBits);
                else exponent = 0;
            } else if ((mantHi | mantLo) == 0
                    && (shiftedOutBits == HIGH_BIT || shiftedOutBits > 0x4000_0000_0000_0000L)) {
                exponent--;
                mantHi = mantLo = 0xFFFF_FFFF_FFFF_FFFFL;
            }
        }

        /**
         * Subtracts a subnormal value, whose mantissa is contained by this instance,
         * from another subnormal value, whose mantissa is contained in {@code minuendLo} and {@code minuendLo} parameters.
         * FYI, exponents of subnormal values are always 0
         *
         * @param minuendLo the lower 64 bits of the mantissa of the minuend
         * @param minuendHi the higher 64 bits of the mantissa of the minuend
         */
        private void subtractSubnormals(long minuendLo, long minuendHi) {
            mantLo = minuendLo - mantLo;
            if (Long.compareUnsigned(mantLo, minuendLo) > 0)
                minuendHi--;
            mantHi = minuendHi - mantHi;
        }

        /**
         * Sets a bit of the mantissa into 1. The position of the bit to be set is defined by the {@code shift} parameter.
         * The bits are implied to be numbered starting from the highest, from 1 to 128,
         * so that {@code setUnity(1)} sets the MSB of the {@code mantHi} field, and {@code setUnity(128)} sets the LSB of {@code mantLo}
         *
         * @param shift the number of the bit to set, starting from 1, that means the most significant bit of the mantissa
         */
        private void setUnity(int shift) {
            if (shift > 64)
                mantLo |= 1L << 128 - shift;
            else
                mantHi |= 1L << 64 - shift;
        }

        /**
         * Shifts the mantissa leftwards by {@code shift} bits
         *
         * @param shift the distance to shift the mantissa by
         */
        private void shiftMantLeft(int shift) {
            assert (shift >= 0 && shift < 129) : "Can't shift by more than 128 or less than 1 bits";
            if (shift == 0) return;
            if (shift >= 128) {
                mantHi = mantLo = 0;
                return;
            }
            if (shift >= 64) {
                mantHi = mantLo << (shift - 64);
                mantLo = 0;
            } else {
                mantHi = mantHi << shift | mantLo >>> (64 - shift);
                mantLo = mantLo << shift;
            }
        }

        /**
         * Subtracts {@code mantHi} and {@code mantLo} from {@code minuendHi} and {@code minuendLo},
         * taking into account the {@code borrow}.
         * The result is returned in {@code mantHi} and {@code mantLo}
         *
         * @param minuendHi the higher 64 bits of the minuend
         * @param minuendLo the lower 64 bits of the minuend
         * @param borrow    the borrow from the lower (shifted out) bits (additionally subtracts 1 if borrow != 0)
         *
         * @return the borrow from the higher bit (implicit unity). May be 0 or 1
         */
        private long subtrMant(long minuendHi, long minuendLo, long borrow) {
            if (borrow != 0 && --minuendLo == -1) {
                mantLo = -1 - mantLo;
                borrow = 1;
            } else {
                mantLo = minuendLo - mantLo;
                borrow = Long.compareUnsigned(mantLo, minuendLo) > 0 ? 1 : 0;
            }
            if (borrow != 0 && --minuendHi == -1) {
                mantHi = -1 - mantHi;
            } else {
                mantHi = minuendHi - mantHi;
                borrow = Long.compareUnsigned(mantHi, minuendHi) > 0 ? 1 : 0;
            }
            return borrow;
        }

        /**
         * Multiples this instance of {@link QuadrupleImmutable} by the given {@link QuadrupleImmutable} factor, ignoring the signs
         * <br>Uses static arrays
         * <b><i>BUFFER_5x32_A, BUFFER_5x32_B, BUFFER_10x32_A</i></b>
         *
         * @param factor the factor to multiply this instance by
         */
        private void multUnsigned(QuadrupleImmutable factor) {
            // will use these buffers to hold unpacked mantissas of the factors (5 longs each, 4 x 32 bits + higher (implicit) unity)
            final long[] factor1 = buffers.get().BUFFER_5x32_A, factor2 = buffers.get().BUFFER_5x32_B, product = buffers.get().BUFFER_10x32_A;

            long productExponent = Integer.toUnsignedLong(
                    exponent)
                    + Integer.toUnsignedLong(factor.exponent) - EXPONENT_OF_ONE;

            if (exponentWouldExceedBounds(productExponent, 1,
                                          0))
                return;

            // put the mantissas into the buffers that will be used by the proper multiplication
            productExponent = normalizeAndUnpack(factor, productExponent, factor1, factor2);
            if (productExponent < -129) {
                assignZero();
                return;
            }

            multiplyBuffers(factor1, factor2, product);
            final boolean isRoundedUp = roundBuffer(product);

            productExponent = normalizeProduct(product, productExponent, isRoundedUp);
            if (productExponent > EXPONENT_OF_MAX_VALUE) {
                assignInfinity();
                return;
            }

            packBufferToMantissa(product);

            if (productExponent <= 0)
                productExponent = normalizeSubnormal(productExponent, isRoundedUp);

            exponent = (int) productExponent;
        }

        /**
         * Prepares the mantissas for being multiplied:<br>
         * if one of the factors is subnormal, normalizes it and appropriately corrects the exponent of the product,
         * then unpack both mantissas to buffers {@code buffer1}, {@code buffer2}.
         *
         * @param factor          the factor to multiply this by
         * @param productExponent preliminary evaluated value of the exponent of the product
         * @param buffer1         a buffer to hold unpacked mantissa of one of the factors (5 longs, each holds 32 bits )
         * @param buffer2         a buffer to hold unpacked mantissa of the other factor
         *
         * @return the exponent of the product, corrected in case if one of the factors is subnormal
         * <br>Covered
         */
        private long normalizeAndUnpack(QuadrupleImmutable factor, long productExponent, long[] buffer1, long[] buffer2) {

            // If one of the numbers is subnormal, put its mantissa in mantHi, mantLo
            long factorMantHi = factor.mantHi, factorMantLo = factor.mantLo;
            boolean oneIsSubnormal = false;
            if (exponent == 0) {
                oneIsSubnormal = true;
            } else if (factor.exponent == 0) {
                factorMantHi = this.mantHi;
                factorMantLo = this.mantLo;
                this.mantHi = factor.mantHi;
                this.mantLo = factor.mantLo;
                oneIsSubnormal = true;
            }

            if (oneIsSubnormal) {
                final int lz = numberOfLeadingZeros();
                productExponent -= lz;
                if (productExponent < -129)
                    return productExponent;
                shiftMantLeft(lz + 1);
            }

            unpack_To5x32(mantHi, mantLo, buffer1);
            unpack_To5x32(factorMantHi, factorMantLo, buffer2);
            return productExponent;
        }

        /**
         * Multiplies the value stored in {@code factor1} as unpacked 128-bit}<br>
         * {@code (4 x 32 bit + highest (implicit) unity)}
         * <br>
         * by the value stored in factor2 of the same format and saves the result
         * in the {@code product} as unpacked 256-bit value}<br>
         * {@code (8 x 32 bit + 1 or 2 highest bits of the product + 0)  }
         *
         * @param factor1 contains the unpacked value of factor1
         * @param factor2 contains the unpacked value of factor2
         * @param product gets filled with the unpacked value of the product
         */
        private static void multiplyBuffers(long[] factor1, long[] factor2, long[] product) {
            assert (factor1.length == factor2.length && product.length == factor1.length * 2) :
                    "Factors' lengths must be equal to each other and twice less than the product's length";

            Arrays.fill(product, 0);
            final int maxIdxFact = factor1.length - 1;
            long sum = 0;

            for (int i = maxIdxFact; i >= 0; i--) {
                for (int j = maxIdxFact; j >= 0; j--) {
                    sum = factor1[i] * factor2[j];
                    product[i + j + 1] += sum & LOWER_32_BITS;
                    product[i + j] += (sum >>> 32) + (product[i + j + 1] >>> 32);
                    product[i + j + 1] &= LOWER_32_BITS;
                }
            }
        }

        /**
         * Rounds the content of the given unpacked buffer
         * so that it contains 128 bits of the fractional part of the product.
         * The integer part of the product of two mantissas is contained in the lowest bits of it buffer[1],
         * the fractional part is contained in the lower half-words of buffer[2]..buffer[6].
         * If bit 129 (counting rightwards starting from the point position),
         * i.e. bit 31 of buffer[6], is 1, the content of buffer[1] -- buffer[5] gets incremented.
         *
         * @return a flag signifying that the number is actually rounded up,
         * used to prevent unnecessary rounding in the future
         */
        private boolean roundBuffer(long[] buffer) {
            buffer[6] += 0x8000_0000L;
            if ((buffer[6] & 0x1_0000_0000L) == 0)
                return false;

            for (int i = 5; ; i--) {
                buffer[i + 1] = 0;
                buffer[i]++;
                if ((buffer[i] & 0x1_0000_0000L) == 0)
                    break;
            }
            return true;
        }

        /**
         * Normalizes a product of multiplication.<br>
         * The product may be => 2 (e.g. 1.9 * 1.9 = 3.61), in this case it should be
         * divided by two, and the exponent should be incremented.
         *
         * @param product         a buffer containing the product
         * @param productExponent preliminary evaluated exponent of the product (may get adjusted)
         * @param isRoundedUp     a flag signifying that rounding should not be applied
         *
         * @return the exponent of the product, perhaps adjusted
         */
        private long normalizeProduct(long[] product, long productExponent, boolean isRoundedUp) {
            if (product[1] > 1) {
                productExponent++;
                if (productExponent <= EXPONENT_OF_MAX_VALUE)
                    shiftBufferRight(product, isRoundedUp);
            }
            return productExponent;
        }

        /**
         * Packs unpacked mantissa held in the given buffer
         * (0, 1 (integer part, i.e. implicit unity), + 4 longs containing 32 bits each)
         * into the {@code mantLo}, {@code mantHi} fields of this instance
         *
         * @param buffer of 6 (at least) longs, containing the fractional part of the mantissa in the lower halves of words 2..5
         *               <br> Covered (no special data required)
         */
        private void packBufferToMantissa(long[] buffer) {
            mantLo = buffer[5] & LOWER_32_BITS | (buffer[4] << 32);
            mantHi = buffer[3] & LOWER_32_BITS | (buffer[2] << 32);
        }

        /**
         * Normalizes a subnormal value (with negative exponent),
         * shifting the bits of its mantissa rightwards according to the exponent's value and clearing
         *
         * @param productExponent the exponent of the product (always negative for subnormal results)
         * @param isRoundedUp     a flag to prevent taking into account the shifted-out LSB when rounding the value
         *                        (in case if the value was already rounded-up)
         *
         * @return the exponent value of a subnormal Float128, that is 0
         */
        private long normalizeSubnormal(long productExponent, boolean isRoundedUp) {
            if (isRoundedUp) mantLo &= -2L;
            productExponent = makeSubnormal(productExponent);
            return productExponent;
        }

        /**
         * Shifts the contents of a buffer, containing the unpacked mantissa
         * of a Float128 as the lower halves of {@code buffer[2] -- buffer[5]}, rightwards one bit.
         * Rounds it up unless the {@code isRoundedUp} parameter is {@code true}.
         *
         * @param buffer      the buffer of (at least) 6 longs, containing the mantissa of a Float128 value
         * @param isRoundedUp a flag to prevent extra rounding in case if the value is already rounded up
         */
        private void shiftBufferRight(long[] buffer, boolean isRoundedUp) {
            if (isRoundedUp)
                shiftBuffRightWithoutRounding(buffer);
            else
                shiftBuffRightWithRounding(buffer); // There can't be carry to the highest (implied) bit, no need to check it
        }

        /**
         * Shifts the contents of a buffer, containing the unpacked mantissa
         * of a Float128 as the lower halves of {@code buffer[2] -- buffer[5]}, rightwards one bit, and rounds it up.
         *
         * @param buffer the buffer of (at least) 6 longs, containing the mantissa of a Float128 value
         */
        private void shiftBuffRightWithRounding(long[] buffer) {
            final long carry = buffer[5] & 1;
            shiftBuffRightWithoutRounding(buffer);
            buffer[5] += carry;
            for (int i = 5; i >= 2; i--) {
                if ((buffer[i] & HIGHER_32_BITS) != 0) {
                    buffer[i] &= LOWER_32_BITS;
                    buffer[i - 1]++;
                }
            }
        }

        /**
         * Shifts the contents of a buffer, containing the unpacked mantissa
         * of a Float128 as the lower halves of {@code buffer[2] -- buffer[5]}, rightwards one bit, without rounding it up
         * (the shifted-out LSB is just truncated).
         *
         * @param buffer the buffer of (at least) 6 longs, containing the mantissa of a Float128 value
         */
        private void shiftBuffRightWithoutRounding(long[] buffer) {
            for (int i = 5; i >= 2; i--)
                buffer[i] = (buffer[i] >>> 1) | (buffer[i - 1] & 1) << 31;
        }

        /**
         * Unpacks the value of the two longs, containing 128 bits of a fractional
         * part of the mantissa, to an "unpacked" buffer, that consists of 5 longs,
         * the first of which contains the integer part of the mantissa, aka implicit
         * unity (that is always 1), and the others (the items
         * {@code buffer[1] -- buffer[4]}) contain 128 bits of the fractional part in
         * their lower halves (bits 31 - 0), the highest 32 bits in
         * {@code buffer[1]). @param factHi the higher 64 bits of the fractional part
         * of the mantissa
         * <p>
         *
         * @param mantLo the lower 64 bits of the fractional part of the mantissa
         * @param buffer the buffer to hold the unpacked mantissa, should be array of at
         *               least 5 longs
         *               <p>
         *
         * @return the buffer holding the unpacked value (the same reference as passed
         * in as the {@code buffer} parameter
         */
        private static void unpack_To5x32(long mantHi, long mantLo, long[] buffer) {
            buffer[0] = 1;
            buffer[1] = mantHi >>> 32;
            buffer[2] = mantHi & LOWER_32_BITS;
            buffer[3] = mantLo >>> 32;
            buffer[4] = mantLo & LOWER_32_BITS;
        }

        /**
         * Unpacks the value of the two longs, containing 128 bits of a fractional part of the mantissa,
         * to an "unpacked" buffer, that consists of 5 ints,
         * the first of which contains the integer part of the mantissa, aka implicit unity (that is always 1),
         * and the others (the items {@code buffer[1] -- buffer[4]}) contain 128 bits
         * of the fractional part (bits 31 - 0),
         * the highest 32 bits in {@code buffer[1]).
         * <p>
         *
         * @param factHi the higher 64 bits of the fractional part of the mantissa
         * @param mantLo the lower 64 bits of the fractional part of the mantissa
         * @param buffer the buffer to hold the unpacked mantissa, should be array of at least 5 longs
         *               <p>
         *
         * @return the buffer holding the unpacked value (the same reference as passed in as the {@code buffer} parameter
         */
        private static void unpack_To5x32(long mantHi, long mantLo, int[] buffer) {
            buffer[0] = 1;
            buffer[1] = (int) (mantHi >>> 32);
            buffer[2] = (int) (mantHi);
            buffer[3] = (int) (mantLo >>> 32);
            buffer[4] = (int) (mantLo);
        }


        /**
         * Divides this instance of {@link QuadrupleImmutable} by the given {@link QuadrupleImmutable} divisor, ignoring their signs
         * <br>Uses static arrays
         * <b><i>BUFFER_5x32_A_INT, BUFFER_5x32_B, BUFFER_10x32_A_INT, BUFFER_10x32_B</i></b>
         * 20.10.17 10:26:36 A new version with probable doubling of the dividend and
         * simplified estimation of the quotient digit and simplified estimation of the necessity of rounding up the result
         *
         * @param divisor the divisor to divide this instance by
         *                <br>Covered
         */
        private void divideUnsigned(MutableBag divisor) {
            if (divisor.compareMagnitudeTo(ONE) == 0)
                return;
            if (compareMagnitudeTo(divisor) == 0) {
                assignOne();
                return;
            }

            long quotientExponent = Integer.toUnsignedLong(
                    exponent)
                    - Integer.toUnsignedLong(divisor.exponent) + EXPONENT_OF_ONE;

            if (exponentWouldExceedBounds(quotientExponent,
                                          0,
                                          1))
                return;

            boolean needToDivide = true;
            final int[] divisorBuff = buffers.get().BUFFER_5x32_A_INT;
            if (exponent != 0 & divisor.exponent != 0) {
                if (mantHi == divisor.mantHi && mantLo == divisor.mantLo) {
                    mantHi = mantLo = 0;
                    needToDivide = false;
                } else {
                    if (divisor.mantHi == 0 && divisor.mantLo == 0)
                        needToDivide = false;
                    else
                        unpack_To5x32(divisor.mantHi, divisor.mantLo,
                                      divisorBuff);
                }
            } else {
                quotientExponent = normalizeAndUnpackSubnormals(quotientExponent, divisor, divisorBuff);
            }

            if (needToDivide)
                quotientExponent = doDivide(quotientExponent, divisorBuff);

            if (exponentWouldExceedBounds(quotientExponent,
                                          0,
                                          0))
                return;

            if (quotientExponent <= 0)
                quotientExponent = makeSubnormal(quotientExponent);

            exponent = (int) quotientExponent;
        }

        /**
         * Checks if the exponent of the result exceeds acceptable bounds and sets the
         * corresponding value in this case.<br>
         * If it is below {@code  -(128 + lowerBoundTolerance) }, assigns zero to this instance and returns {@code true}.
         * If it is above {@code EXPONENT_OF_MAX_VALUE + upperBoundTolerance}, assigns Infinity to this instance and returns {@code true}.
         * returns {@code false} without changing the value, if the exponent is within the bounds.
         *
         * @param exponent the exponent of the result to be examined
         *                 <br>Covered
         */
        private boolean exponentWouldExceedBounds(long exponent, long lowerBoundTolerance, long upperBoundTolerance) {
            if (exponent > EXPONENT_OF_MAX_VALUE + upperBoundTolerance) {
                assignInfinity();
                return true;
            }
            if (exponent < -(128 + lowerBoundTolerance)) {
                assignZero();
                return true;
            }
            return false;
        }

        /**
         * Normalizes the operands and unpacks the divisor:<br>
         * normalizes the mantissa of this instance as needed, if it's subnormal;
         * unpacks the mantissa of the divisor into divisorBuff and normalizes the unpacked value if the divisor is subnormal
         * (the divisor itself remains unchanged);
         * adjusts appropriately the quotientExponent.
         *
         * @param quotientExponent preliminary evaluated exponent of the divisor, gets corrected and returned
         * @param divisor          the divisor to unpack and normalize as needed
         * @param divisorBuff      the buffer to unpack the divisor into
         *
         * @return the exponent of the quotient, adjusted accordingly to the normalization results
         * <br>Covered
         */
        private long normalizeAndUnpackSubnormals(long quotientExponent, MutableBag divisor, int[] divisorBuff) {
            if (exponent == 0)
                quotientExponent -= normalizeMantissa();

            if (divisor.exponent == 0) {
                quotientExponent += normalizeAndUnpackDivisor(divisor, divisorBuff);
            } else
                unpack_To5x32(divisor.mantHi, divisor.mantLo, divisorBuff);
            return quotientExponent;
        }

        /**
         * Shifts the mantissa (of a subnormal value) leftwards so that it has
         * the conventional format (with implied higher unity to the left of the highest bit of mantHi
         * and higher 64 bits of the fractional part in mantHi)
         *
         * @return the number of bits the mantissa is shifted by, minus one (e.g. 0 for MIN_NORMAL / 2)
         * to use as an exponent correction
         * <br>Covered
         */
        private long normalizeMantissa() {
            int shift = Long.numberOfLeadingZeros(mantHi);
            if (shift == 64)
                shift += Long.numberOfLeadingZeros(mantLo);
            shiftMantLeft(shift + 1);
            return shift;
        }

        /**
         * Unpacks the mantissa of the given divisor into the given buffer and normalizes it,
         * so that the buffer contains the MSB in the LSB of buffer[0]
         * and up to 127 bits in the lower halves of buffer[1] -- buffer[4]
         *
         * @param divisor a subnormal Float128 whose mantissa is to be unpacked and normalizes
         * @param buffer  a buffer of at least 5 longs to unpack the mantissa to
         *
         * @return the number of bits the mantissa is shifted by, minus one (e.g. 0 for MIN_NORMAL / 2)
         * to use as an exponent correction
         * <br>Covered
         */
        private static long normalizeAndUnpackDivisor(MutableBag divisor, int[] buffer) {
            long mantHi = divisor.mantHi, mantLo = divisor.mantLo;
            int shift = Long.numberOfLeadingZeros(mantHi);
            if (shift == 64)
                shift += Long.numberOfLeadingZeros(mantLo);
            final long result = shift;
            shift++;

            if (shift <= 64) {
                mantHi = (mantHi << shift) + (mantLo >>> 64 - shift);
                mantLo <<= shift;
            } else {
                mantHi = mantLo << shift - 64;
                mantLo = 0;
            }

            unpack_To5x32(mantHi, mantLo, buffer);
            return result;
        }

        /**
         * Divides preliminarily normalized mantissa of this instance by the mantissa of the divisor
         * given as an unpacked value in {@code divisor}.
         *
         * @param quotientExponent a preliminarily evaluated exponent of the quotient, may get adjusted
         * @param divisor          unpacked divisor (integer part (implicit unity) in divisor[0],
         *                         128 bits of the fractional part in the lower halves of divisor[1] -- divisor[4])
         *
         * @return (possibly adjusted) exponent of the quotient
         */
        private long doDivide(long quotientExponent, final int[] divisor) {
            final int[] dividend = buffers.get().BUFFER_10x32_A_INT;
            quotientExponent = unpackMantissaTo(quotientExponent, divisor, dividend);
            divideBuffers(dividend, divisor, quotientExponent);
            return quotientExponent;
        }

        /**
         * Unpacks the mantissa of this instance into {@code dividend}.
         * If the mantissa of this instance is less than the divisor, multiplies it by 2 and decrements the {@code quotientExponent}
         *
         * @param quotientExponent a preliminary evaluated exponent of the quotient being computed
         * @param divisor          a buffer (5 longs) containing unpacked divisor,
         *                         integer 1 in divisor[0], 4 x 32 bits of the mantissa in in divisor[1..4]
         * @param dividend         a buffer (10 longs) to unpack the mantissa to,
         *                         integer 1 (or up to 3 in case of doubling) in dividend[1], 4 x 32 bits of the mantissa in in dividend[2..5]
         *
         * @return (possibly decremented) exponent of the quotient
         */
        private long unpackMantissaTo(long quotientExponent, final int[] divisor, final int[] dividend) {
            // The mantissa of this is normalized, the normalized mantissa of the divisor is in divisorBuff
            if (compareMantissaWith(divisor) < 0) {
                unpackDoubledMantissaToBuff_10x32(dividend);
                quotientExponent--;
            } else
                unpackMantissaToBuff_10x32(dividend);
            return quotientExponent;
        }

        /**
         * Compares the mantissa of this instance with the unpacked mantissa of another Float128 value.
         * Returns
         * <li>a positive value if the mantissa of this instance is greater,
         * <li>zero if the mantissas are equal,
         * <li>a negative value if the mantissa of this instance is less than the mantissa of the other Float128.
         * <br><br>
         *
         * @param divisor a buffer (5 longs) containing an unpacked value of the mantissa of the other operand,
         *                integer 1 in divisor[1], 4 x 32 bits of the mantissa in in divisor[1..4]
         *
         * @return a positive value if the mantissa of this instance is greater, zero if the mantissas are equal,
         * or a negative value if the mantissa of this instance is less than the mantissa of the other Float128.
         */
        private int compareMantissaWith(int[] divisor) {
            final int cmp = Long.compareUnsigned(mantHi, ((long) divisor[1] << 32) | (divisor[2] & LOWER_32_BITS));
            return cmp == 0 ? Long.compareUnsigned(mantLo, ((long) divisor[3] << 32) | (divisor[4] & LOWER_32_BITS)) : cmp;
        }

        /**
         * Unpacks the mantissa of this instance into the given buffer and multiplies it by 2 (shifts left),
         * integer part (may be up to 3) in buffer[1], fractional part in buffer[2] -- buffer[5]
         *
         * @param buffer a buffer to unpack the mantissa, at least 6 longs
         *               <br>Covered
         */
        private void unpackDoubledMantissaToBuff_10x32(int[] buffer) { //
            //    Arrays.fill(buffer, 0); // Manual filling a little faster
            buffer[0] = 0;
            buffer[1] = (int) (2 + (mantHi >>> 63));
            buffer[2] = (int) (mantHi >>> 31 & LOWER_32_BITS);
            buffer[3] = (int) (((mantHi << 1) + (mantLo >>> 63)) & LOWER_32_BITS);
            buffer[4] = (int) (mantLo >>> 31 & LOWER_32_BITS);
            buffer[5] = (int) (mantLo << 1 & LOWER_32_BITS);
            for (int i = 6; i < 10; i++)
                buffer[i] = 0;
        }

        /**
         * Unpacks the mantissa of this instance into the given buffer,
         * integer part (implicit unity) in buffer[1], fractional part in lower halves of buffer[2] -- buffer[5]
         *
         * @param buffer a buffer to unpack the mantissa, at least 6 longs
         *               <br>Covered
         */
        private void unpackMantissaToBuff_10x32(int[] buffer) {
            buffer[0] = 0;
            buffer[1] = 1;
            buffer[2] = (int) (mantHi >>> 32);
            buffer[3] = (int) (mantHi & LOWER_32_BITS);
            buffer[4] = (int) (mantLo >>> 32);
            buffer[5] = (int) (mantLo & LOWER_32_BITS);
            for (int i = 6; i < 10; i++)
                buffer[i] = 0;
        }

        /**
         * Divides the dividend given as an unpacked buffer {@code dividendBuff} by the divisor
         * held in the unpacked form in the {@code divisorBuff} and packs the result
         * into the fields {@code mantHi, mantLo} of this instance. Rounds the result as needed.
         * <br>Uses static arrays
         * <b><i>BUFFER_5x32_B_INT</i></b>
         *
         * @param dividend         a buffer of 10 longs containing unpacked mantissa of the dividend (integer 1 (or up to 3 in case of doubling) in dividend[1],
         *                         the fractional part of the mantissa in the lower halves of dividend[2] -- dividend[5])
         * @param divisor          a buffer of 5 longs containing unpacked mantissa of the divisor (integer part (implicit unity) in divisor[0],
         *                         the fractional part of the mantissa in the lower halves of divisor[1] -- divisor[4])
         * @param quotientExponent preliminary evaluated exponent of the quotient, may get adjusted
         */
        private void divideBuffers(int[] dividend, int[] divisor, long quotientExponent) {
            final int[] quotientBuff = buffers.get().BUFFER_5x32_B_INT;

            final long nextBit = divideArrays(dividend, divisor, quotientBuff);

            packMantissaFromWords_1to4(quotientBuff);

            if (quotientExponent > 0
                    && nextBit != 0
                    && ++mantLo == 0)
                ++mantHi;
        }

        /**
         * Divides an unpacked value held in the 10 ints of the {@code dividend}
         * by the value held in the 5 ints of the {@code divisor}
         * and fills 5 ints of the {@code quotient} with the quotient value.
         * All values are unpacked 129-bit values, containing integer parts
         * (implicit unities, always 1) in LSB of buff[0] (buff[1] for dividend)
         * and 128 bits of the fractional part in lower halves of buff[1] -- buff[4] (buff[2] -- buff[5] for dividend).
         * It uses the principle of the standard long division algorithm, with the difference
         * that instead of one decimal digit of the quotient at each step, the next 32 bits are calculated.
         *
         * @param dividend an unpacked value of the mantissa of the dividend (10 x 32 bits: 0, 1, dd1, dd2, dd3, dd4, 0, 0, 0, 0)
         * @param divisor  an unpacked value of the mantissa of the divisor (5 x 32 bits: 1, dr1, dr2, dr3, dr4)
         * @param quotient a buffer that gets filled with the mantissa of the quotient
         *
         * @return the next bit of the quotient (half the LSB), to be used for rounding the result
         * <br>Covered
         */
        private static long divideArrays(int[] dividend, int[] divisor, int[] quotient) {
            final long divisorHigh = ((long) divisor[0] << 32) | (divisor[1] & LOWER_32_BITS);
            int offset = 0;
            quotient[offset++] = 1;
            subtractDivisor(divisor, dividend);

            do {
                final long remainderHigh = ((long) dividend[offset + 1] << 32) | (dividend[offset + 2] & LOWER_32_BITS);

                long quotientWord = (dividend[offset] == 0) ?
                        divideUnsignedLongs(remainderHigh, divisorHigh) :
                        divide65bits(dividend[offset], remainderHigh, divisorHigh);

                if (quotientWord == 0x1_0000_0000L)
                    quotientWord--;

                if (quotientWord != 0) {
                    if (multipyAndSubtract(divisor, quotientWord, offset, dividend) < 0) {
                        quotientWord--;
                        addDivisorBack(divisor, dividend, offset);
                    }
                }

                quotient[offset++] = (int) quotientWord;
            } while (offset <= 4);

            if (greaterThanHalfOfDivisor_3(dividend, divisor, offset))
                return 1;

            return 0;

        }

        /**
         * Subtracts the divisor from the dividend to obtain the remainder for the first iteration
         *
         * @param divisor   unpacked mantissa of the divisor, 1 + 4 x 32 bits, implicit integer 1 in divisor[0]
         * @param remainder unpacked mantissa of the dividend, 2 + 8 x 32 bits, implicit integer 1 in divisor[1]
         */
        private static void subtractDivisor(int[] divisor, int[] remainder) {
            long carry = 0;
            for (int i = 5; i >= 1; i--) {
                final long difference = (remainder[i] & LOWER_32_BITS) - (divisor[i - 1] & LOWER_32_BITS) + carry;
                ;
                remainder[i] = (int) difference;
                carry = difference >> 32;
            }
        }

        private static long divideUnsignedLongs(long dividend, long divisor) {
            final long dividendHi = dividend >>> 16;
            final long remainder = (dividendHi % divisor << 16) | (dividend & 0xFFFFL);
            return (dividendHi / divisor << 16) | (remainder / divisor);
        }

        /**
         * Divides a dividend, consisting of more than 64 bits (and less than 81 bits),
         * by the given divisor, that may contain up to 33 bits.
         *
         * @param dividendHi the most significant 64 bits of the dividend
         * @param dividendLo the least significant 64 bits of the dividend
         * @param divisor    the divisor
         *
         * @return the quotient
         */
        private static long divide65bits(long dividendHi, long dividendLo, long divisor) {
            dividendHi = dividendHi << 48 | dividendLo >>> 16;
            final long quotientHi = dividendHi / divisor;
            final long remainder = ((dividendHi % divisor) << 16) | (dividendLo & 0xFFFF);
            final long quotientLo = remainder / divisor;

            return quotientHi << 16 | quotientLo;
        }


        /**
         * Multiplies the divisor by a newly found word of quotient,
         * taking into account the position of the word in the quotient ({@code offset} is the index
         * of the given word in the array that contains the quotient being calculated),
         * and subtracts the product from the remainder, to prepare the remainder for the next iteration
         * <br>Uses static arrays
         * <b><i>BUFFER_10x32_B</i></b>
         *
         * @param divisor      unpacked mantissa of the divisor, 1 + 4 x 32 bits, implicit integer 1 in divisor[0]
         * @param quotientWord a newly found word (32 bits) of the quotient being computed
         * @param offset       the position (index) of the given {@code quotientWord} in the future quotient,
         *                     defines the position of the product, that is subtracted from the remainder, relative to the latter
         * @param remainder    the remainder to subtract the product from
         */
        private static int multipyAndSubtract(int[] divisor, long quotientWord, int offset, int[] remainder) {
            offset += 5;
            long carry = 0;

            long difference = 0;
            for (int i = 4; i >= 0; i--) {
                final long product = quotientWord * (divisor[i] & LOWER_32_BITS) + carry;
                difference = remainder[offset] - product;
                remainder[offset--] = (int) difference;
                carry = product >>> 32;
                if ((difference & LOWER_32_BITS) > (~(int) product & LOWER_32_BITS))
                    carry++;
            }
            return (int) difference;
        }

        /**
         * Adds the divisor, shifted by offset words, back to remainder, to correct the remainder in case when
         * preliminarily estimated word of quotient occurred to be too great
         *
         * @param divisor   unpacked mantissa of the divisor, 1 + 4 x 32 bits, implicit integer 1 in divisor[0]
         * @param remainder unpacked mantissa of the dividend, 2 + 8 x 32 bits, implicit integer 1 in divisor[1]
         */
        private static void addDivisorBack(int[] divisor, int[] remainder, int offset) {
            offset += 5;
            long carry = 0;

            for (int idx = 4; idx >= 0; idx--) {
                final long sum = (remainder[offset] & LOWER_32_BITS) + (divisor[idx] & LOWER_32_BITS) + carry;
                remainder[offset--] = (int) sum;
                carry = sum >>> 32;
            }
        }


        private static boolean greaterThanHalfOfDivisor_3(int[] remainder, int[] divisor, int offset) {
            for (int idx = 0; idx < 4; idx++) {
                final int cmp = Integer.compare(
                        (remainder[offset] << 1) + (remainder[++offset] >>> 31) + Integer.MIN_VALUE,
                        // Doubled remainder
                        divisor[idx] + Integer.MIN_VALUE
                        // Greater than divisor
                );
                if (cmp > 0)
                    return true;
                if (cmp < 0)
                    return false;
            }
            final int cmp = Integer.compareUnsigned(
                    (remainder[offset] << 1),
                    divisor[4]
            );
            return (cmp >= 0);
        }

        /**
         * Packs the unpacked value from words 1..4 of the given buffer into the mantissa of this instance
         *
         * @param buffer the buffer of at least 5 longs, containing an unpacked value of the mantissa
         *               (the integer part (implicit unity) in buffer[0], the 128 bits of fractional part in the lower halves of buffer[1] -- buffer[4]
         *               <br>Covered
         */
        private void packMantissaFromWords_1to4(int[] buffer) {
            mantLo = (buffer[4] & LOWER_32_BITS) | ((long) buffer[3] << 32);
            mantHi = (buffer[2] & LOWER_32_BITS) | ((long) buffer[1] << 32);
        }

        /**
         * Calculates the higher 136 bits of the square root of the mantissa
         * and stores the high 128 bits of result in mantHi, mantLo,
         * returns the 2 least significant bits, aligned to the left,
         * as the result for purposes of possible rounding
         * <br>Uses static arrays
         * <b><i>BUFFER_3x64_A, BUFFER_3x64_B, BUFFER_3x64_C, BUFFER_3x64_D</i></b>
         *
         * @return bits 128 -- 135 of the root in the high byte of the long result
         */
        private long sqrtMant() {
            final long[] remainder = buffers.get().BUFFER_3x64_A;
            final long[] rootX2 = buffers.get().BUFFER_3x64_B;
            Arrays.fill(rootX2, 0);
            final long[] root = buffers.get().BUFFER_3x64_C;
            Arrays.fill(root, 0);

            final long digit = findFirstDigit();

            remainder[0] = mantHi - ((0x200 + digit) * digit << 48);
            remainder[1] = mantLo;
            remainder[2] = 0;
            shift_6_bitsLeft(remainder);

            root[0] = digit << WORD_LENGTH - DIGIT_LENGTH;
            // The doubled root contains explicit unity, and the digits shifted right by 9 bits,
            // so that the first (most significant) digit's position is as follows: 0b0000_0000_1###_####_#000_...
            // Such scale is convenient for finding the next digit
            rootX2[0] = 0x0080_0000_0000_0000L + (digit << (WORD_LENGTH - (DIGIT_LENGTH * 2) - 1));

            int bitNumber = DIGIT_LENGTH;

            if (isEmpty(remainder))
                while (bitNumber < MAX_BITS_FOR_SQRT) {
                    bitNumber = computeNextDigit(remainder, rootX2, root, bitNumber);
                }

            mantHi = root[0];
            mantLo = root[1];
            return root[2];
        }

        /**
         * Finds the first byte of the root, using a table that maps
         * the most significant 16 bits of the mantissa of this instance
         * to corresponding 8 bits of the sought root
         */
        private long findFirstDigit() {
            final int sqrtDigit = (int) (mantHi >>> 48);
            int idx = Arrays.binarySearch(SQUARE_BYTES, sqrtDigit);
            if (idx < 0) idx = -idx - 2;
            return ROOT_BYTES[idx];
        }

    }

    private static final int WORD_LENGTH = 64;
    private static final int DIGIT_LENGTH = 8;
    private static final int MAX_BITS_FOR_SQRT = 20 * DIGIT_LENGTH;

    /**
     * Shifts the contents of the buffer left by 6 bits
     *
     * @param buffer the buffer to shift
     */
    private static void shift_6_bitsLeft(long[] buffer) {
        for (int i = 0; i < buffer.length - 1; i++)
            buffer[i] = (buffer[i] << 6 | buffer[i + 1] >>> 58);
        buffer[buffer.length - 1] <<= 6;
    }

    /**
     * Calculates the next digit of the root, appends it to the root at a suitable position
     * and changes the involved values, remainder and rootX2, accordingly
     * (extracted from sqrtMant() 20.09.02 10:19:34)
     * <br>Uses static arrays
     * <b><i>BUFFER_3x64_D</i></b>
     *
     * @param remainder the remainder
     * @param rootX2    doubled root
     * @param root      square root found so far
     * @param bitNumber the position of the digit to be found
     *
     * @return the position of the next to be found
     */
    private static int computeNextDigit(final long[] remainder, final long[] rootX2, final long[] root, int bitNumber) {
        final long[] aux = buffers.get().BUFFER_3x64_D;
        final long digit = findNextDigit(rootX2, remainder, aux,
                                         bitNumber);
        addDigit(root, digit, bitNumber);

        final boolean remainderIsEmpty = subtractBuff(aux,
                                                      remainder);
        if (remainderIsEmpty || bitNumber >= MAX_BITS_FOR_SQRT - 8)
            return Integer.MAX_VALUE;

        shift_8_bitsLeft(remainder);

        addDigitToBuff(rootX2, digit, bitNumber + 9);
        bitNumber += DIGIT_LENGTH;
        return bitNumber;
    }

    /**
     * Finds the next digit in the root and the corresponding {@code aux} value,
     * that will be subtracted from the remainder.
     *
     * @param rootX2        doubled root found so far
     * @param remainder     the remainder
     * @param aux           auxiliary value to be subtracted from the remainder
     * @param rootBitNumber the position of the digit in the root
     *
     * @return the digit found
     */
    private static long findNextDigit(long[] rootX2, long[] remainder, long[] aux, int rootBitNumber) {
        long digit = Long.divideUnsigned(remainder[0], rootX2[0]);
        digit = Math.min(digit, 0xFF);

        computeAux(digit, rootBitNumber, rootX2, aux);
        while (compareBuffs64(aux,
                              remainder) > 0) {
            // A very rare case: probability is less than 0.85%
            digit--;
            computeAux(digit, rootBitNumber, rootX2, aux);
        }
        return digit;
    }

    /**
     * Appends the found digit to the calculated root at the position specified by rootBitNumber.
     * for cases where the digit cannot fall on a word boundary
     *
     * @param root          a buffer containing the bits of the root found so far
     * @param digit         a value of the digit to append
     * @param rootBitNumber the position to place the most significant bit of the digit at, counting from MSB
     */
    private static void addDigit(long[] root, long digit, int rootBitNumber) {
        final int buffIdx = rootBitNumber / 64;
        final int bitIdx = rootBitNumber % 64;
        root[buffIdx] += digit << 56 - bitIdx;
    }

    /**
     * Subtracts a number, represented as a big-endian array of longs, from another number of the same form
     *
     * @param subtrahend subtrahend
     * @param minuend    minuend that is replaced with the difference
     *
     * @return {@code true} if the result is 0 (i.e. the operands are equal)
     */
    private static boolean subtractBuff(long[] subtrahend, long[] minuend) {
        boolean diffIsEmpty = true;
        long diff, minnd;

        for (int i = subtrahend.length - 1; i >= 0; i--) {
            minnd = minuend[i];
            diff = minnd - subtrahend[i];

            if (Long.compareUnsigned(diff, minnd) > 0) { // Underflow.
                // It can't be the most significant word (i == 0), since aux can't be greater than remainder -- guaranteed by findNextDigit()
                if (minuend[i - 1] == 0) subtrahend[i - 1]++;
                else minuend[i - 1]--;
            }
            minuend[i] = diff;
            diffIsEmpty &= diff == 0;
        }
        return diffIsEmpty;
    }

    /**
     * Shifts the contents of the buffer left by 8 bits
     *
     * @param buffer the buffer to shift
     */
    private static void shift_8_bitsLeft(long[] buffer) {
        for (int i = 0; i < buffer.length - 1; i++)
            buffer[i] = (buffer[i] << 8 | buffer[i + 1] >>> 56);
        buffer[buffer.length - 1] <<= 8;
    }

    /**
     * Appends the found digit to the calculated root at the position specified by rootBitNumber.
     * for cases where the digit can fall on a word boundary
     *
     * @param buff      a buffer containing the bits of the root found so far
     * @param digit     a value of the digit to append
     * @param bitNumber the position to place the most significant bit of the digit at, counting from MSB
     */
    private static void addDigitToBuff(long[] buff, long digit, int bitNumber) { //
        final int buffIdx = bitNumber / 64;
        final int bitIdx = bitNumber % 64;

        if (bitIdx <= 64 - 8) {
            buff[buffIdx] += digit << 64 - 8 - bitIdx;
        } else {
            buff[buffIdx] += digit >>> bitIdx + 8 - 64;
            buff[buffIdx + 1] += digit << 128 - 8 - bitIdx;
        }
    }

    /**
     * Computes the auxiliary value to be subtracted from the remainder: aux = 2rd + d^2 * scale.
     * Instead of the 'scale' the bit position {@code rootBitNumber} is used
     *
     * @param digit         the found next digit of the root
     * @param rootBitNumber the position of the digit in the root
     * @param rootX2        doubled root found so far
     * @param aux           the buffer to be filled with the found value of aux
     */
    private static void computeAux(long digit, int rootBitNumber, long[] rootX2, long[] aux) {
        copyBuff(rootX2, aux);
        addDigitToBuff(aux, digit, rootBitNumber + 10);
        multBufByDigit(aux, digit);
    }

    /**
     * Copies an array of longs from src to dst
     *
     * @param src source
     * @param dst destination
     */
    private static void copyBuff(long[] src, long[] dst) {
        System.arraycopy(src, 0, dst, 0, src.length);
    }

    /**
     * Compares two numbers
     * represented as arrays of {@code long} (big-endian, most significant bits in buff[0])
     *
     * @param buff1 contains the first number to compare
     * @param buff2 contains the second number to compare
     *
     * @return The result of comparison, according to general Java comparison convention
     */
    private static int compareBuffs64(long[] buff1, long[] buff2) {
        for (int i = 0; i < buff1.length; i++)
            if (buff1[i] != buff2[i])
                return Long.compareUnsigned(buff1[i], buff2[i]);
        return 0;
    }

    /**
     * Multiplies a number represented as {@code long[]} by a digit
     * (that's expected to be less than 32 bits long)
     */
    private static void multBufByDigit(long[] buff, long digit) {
        long carry = 0;
        for (int i = buff.length - 1; i >= 0; i--) {
            final long prodLo = (buff[i] & LOWER_32_BITS) * digit + carry;
            final long prodHi = (buff[i] >>> 32) * digit;
            carry = prodHi >>> 32;
            final long product = prodLo + (prodHi << 32);
            if (Long.compareUnsigned(product, (prodHi << 32)) < 0)
                carry++;
            buff[i] = product;
        }
    }

    /**
     * Multiplies 192 bits of the mantissa given in the arguments
     * {@code mantHi, mantLo, and thirdWord}, without implicit unity (only fractional part)
     * by 192 bits of the constant value of sqrt(2)
     * <br> uses static arrays
     * <b><i>BUFFER_4x64_A, BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_12x32</i></b>
     *
     * @param mantHi    64 most significant bits of the fractional part of the mantissa
     * @param mantLo    bits 64..127 of the fractional part of the mantissa
     * @param thirdWord 64 least significant bits of the fractional part of the mantissa
     *
     * @return 192 bits of the fractional part of the product
     */
    private long[] multBySqrt2(long mantHi, long mantLo, long thirdWord) {

        var buff464 = buffers.get().BUFFER_4x64_A;
        buff464[0] = 0;
        buff464[1] = mantHi >>> 1 | HIGH_BIT;
        buff464[2] = mantLo >>> 1 | mantHi << 63;
        buff464[3] = thirdWord >>> 1 | mantLo << 63;

        final long[] product = multPacked3x64(
        );

        product[0] = product[1] << 2 | product[2] >>> 62;
        product[1] = product[2] << 2 | product[3] >>> 62;
        product[2] = product[3] << 2;
        return product;
    }

    /**
     * Multiplies mantissas of quasi-decimal numbers given as contents of arrays factor1 and factor2
     * (with exponent in buff[0] and 192 bits of mantissa in buff[1]..buff[3]),
     * replaces the mantissa of factor1 with the product. Does not affect factor1[0].<br>
     * uses static arrays <b><i>BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_12x32</b></i>
     *
     * @return factor1, whose mantissa is replaced with the product
     */
    private static long[] multPacked3x64() {
        multPacked3x64_simply();
        return pack_12x32_to_3x64();
    }


    /**
     * Returns a copy of this Float128.
     *
     * @return A new instance of Float128 with the same value as this
     */
    public QuadrupleImmutable cpy() {
        return new QuadrupleImmutable(this);
    }

    /**
     * Returns a new instance of {@link QuadrupleImmutable} with the value of the absolute value of this instance
     *
     * @return a new instance of {@link QuadrupleImmutable} with the value of the absolute value of this instance
     */
    public QuadrupleImmutable abs() {
        return new QuadrupleImmutable(false, exponent, mantHi, mantLo);
    }

    /**
     * Checks if the value is negative.
     *
     * @return {@code true}, if the value is negative, {@code false} otherwise
     */
    public boolean isNegative() {
        return negative;
    }

    /**
     * Checks if the value is infinite (i.e {@code NEGATIVE_INFINITY} or {@code POSITIVE_INFINITY}).
     *
     * @return {@code true}, if the value is infinity (either positive or negative), {@code false} otherwise
     */
    public boolean isInfinite() {
        return (exponent == EXPONENT_OF_INFINITY) && ((mantHi | mantLo) == 0);
    }

    /**
     * Checks if the value is not a number (i.e. has the value of {@code NaN}).
     *
     * @return {@code true}, if the value is not a number (NaN), {@code false} otherwise
     */
    public boolean isNaN() {
        return (exponent == EXPONENT_OF_INFINITY) && ((mantHi | mantLo) != 0);
    }

    /**
     * Checks if the value is zero, either positive or negative.
     *
     * @return {@code true}, if the value is 0 or -0, otherwise returns
     */
    public boolean isZero() {
        return (mantHi | mantLo | exponent) == 0;
    }

    /**
     * Returns 1 for positive values, -1 for negative values (including -0), and 0 for the positive zero value
     *
     * @return 1 for positive values, -1 for negative values (including -0), and 0 for the positive zero value
     */
    public int signum() {
        return negative ? -1 :
                isZero() ? 0 :
                        1;
    }

    /** Just for convenience: 0x8000_0000_0000_0000L; (== Long.MIN_VALUE) */
    private static final long HIGH_BIT = 0x8000_0000_0000_0000L;
    /** Just for convenience: 0x8000_0000_0000_0000L; */
    private static final long BIT_63 = HIGH_BIT;

    /** Just for convenience: 0x0000_0000_FFFF_FFFFL */
    private static final long LOWER_32_BITS = 0x0000_0000_FFFF_FFFFL;
    /** Just for convenience: 0xFFFF_FFFF_0000_0000L; */
    private static final long HIGHER_32_BITS = 0xFFFF_FFFF_0000_0000L;
    /** Just for convenience: 0x8000_0000L; // 2^31 */
    private static final long POW_2_31_L = 0x8000_0000L; // 2^31

    /** Inner structure of double: where it holds its sign */
    private static final long DOUBLE_SIGN_MASK = HIGH_BIT;
    /** Inner structure of double: where it holds its exponent */
    private static final long DOUBLE_EXP_MASK = 0x7ff0_0000_0000_0000L;
    /** Inner structure of double: where it holds its mantissa */
    private static final long DOUBLE_MANT_MASK = 0x000f_ffff_ffff_ffffL;

    /** double's exponent value corresponding to 2^0 = 1, shifted to lower bits */
    private static final int EXP_0D = 0x0000_03FF;

    /** The highest bit of Quad's mantissa that doesn't fit in double's mantissa (is lower than the lowest) */
    private static final long HALF_DOUBLES_LSB = 0x0000_0000_0000_0800L;
    /** The implied position of the implied unity in double */
    private static final long DOUBLE_IMPLIED_MSB = 0x0010_0000_0000_0000L;


    /**
     * Corrects possible underflow of the decimal mantissa, passed in the {@code buffer_10x32},
     * by multiplying it by a power of ten. The corresponding value to adjust the decimal exponent is returned as the result
     *
     * @return a corrective (addition) that is needed to adjust the decimal exponent of the number
     */
    private static int correctPossibleUnderflow() {
        int expCorr = 0;
        while (isLessThanOne()) { // Underflow
            multBuffBy10();
            expCorr -= 1;
        }
        return expCorr;
    }

    /**
     * Unpacks the mantissa of a 192-bit quasi-decimal (4 longs: exp10, mantHi, mantMid, mantLo)
     * to a buffer of 6 longs, where the least significant 32 bits of each long contains
     * respective 32 bits of the mantissa
     *
     * @param qd192     array of 4 longs containing the number to unpack
     * @param buff_6x32 buffer of 6 long to hold the unpacked mantissa
     */
    private static void unpack_3x64_to_6x32(long[] qd192, long[] buff_6x32) {
        buff_6x32[0] = qd192[1] >>> 32;
        buff_6x32[1] = qd192[1] & LOWER_32_BITS;
        buff_6x32[2] = qd192[2] >>> 32;
        buff_6x32[3] = qd192[2] & LOWER_32_BITS;
        buff_6x32[4] = qd192[3] >>> 32;
        buff_6x32[5] = qd192[3] & LOWER_32_BITS;
    }

    /**
     * Checks if the unpacked quasi-decimal value held in the given buffer
     * is less than one (in this format, one is represented as { 0x1999_9999L, 0x9999_9999L, 0x9999_9999L,...}
     *
     * @return {@code true}, if the value is less than one
     */
    private static boolean isLessThanOne() {
        var buff1232 = buffers.get().BUFFER_12x32;
        if (buff1232[0] < 0x1999_9999L) return true;
        if (buff1232[0] > 0x1999_9999L) return false;

        // A note regarding the coverage:
        // Multiplying a 128-bit number by another 192-bit number,
        // as well as multiplying of two 192-bit numbers,
        // can never produce 320 (or 384 bits, respectively) of 0x1999_9999L, 0x9999_9999L,
        for (int i = 1; i < buff1232.length; i++) { // so this loop can't be covered entirely
            if (buff1232[i] < 0x9999_9999L) return true;
            if (buff1232[i] > 0x9999_9999L) return false;
        }
        // and it can never reach this point in real life.
        return false;
    }

    /**
     * Multiplies the unpacked value stored in the given buffer by 10
     */
    private static void multBuffBy10() {
        var buff1232 = buffers.get().BUFFER_12x32;
        final int maxIdx = buff1232.length - 1;
        buff1232[0] &= LOWER_32_BITS;
        buff1232[maxIdx] *= 10;
        for (int i = maxIdx - 1; i >= 0; i--) {
            buff1232[i] = buff1232[i] * 10 + (buff1232[i + 1] >>> 32);
            buff1232[i + 1] &= LOWER_32_BITS;
        }
    }

    /**
     * Multiplies mantissas of two packed quasi-decimal values
     * (each is an array of 4 longs, exponent + 3 x 64 bits of mantissa)
     * Returns the product as unpacked buffer of 12 x 32 (12 x 32 bits of product)<br>
     * uses static arrays <b><i>BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_12x32</b></i>
     */
    private static void multPacked3x64_simply() {
        var buff1232 = buffers.get().BUFFER_12x32;
        var buff632a = buffers.get().BUFFER_6x32_A;
        var buff632b = buffers.get().BUFFER_6x32_B;
        Arrays.fill(buff1232, 0);
        unpack_3x64_to_6x32(buffers.get().BUFFER_4x64_A, buff632a);
        unpack_3x64_to_6x32(buffers.get().SQRT_2_AS_LONGS, buff632b);

        for (int i = 5; i >= 0; i--) // compute partial 32-bit products
            for (int j = 5; j >= 0; j--) {
                final long part = buff632a[i] * buff632b[j];
                buff1232[j + i + 1] += part & LOWER_32_BITS;
                buff1232[j + i] += part >>> 32;
            }

        for (int i = 11; i > 0; i--) {
            buff1232[i - 1] += buff1232[i] >>> 32;
            buff1232[i] &= LOWER_32_BITS;
        }
    }

    /**
     * converts 192 most significant bits of the mantissa of a number from an unpacked quasi-decimal form (where 32 least significant bits only used)
     * to a packed quasi-decimal form (where buff[0] contains the exponent and buff[1]..buff[3] contain 3 x 64 = 192 bits of mantissa)
     *
     * @return packedQD192 with words 1..3 filled with the packed mantissa. packedQD192[0] is not affected.
     */
    private static long[] pack_12x32_to_3x64() {
        var buff1232 = buffers.get().BUFFER_12x32;
        var buff464 = buffers.get().BUFFER_4x64_A;
        buff464[1] = (buff1232[0] << 32) + buff1232[1];
        buff464[2] = (buff1232[2] << 32) + buff1232[3];
        buff464[3] = (buff1232[4] << 32) + buff1232[5];
        return buff464;
    }

    /**
     * Checks if the buffer is empty (contains nothing but zeros)
     *
     * @param buffer the buffer to check
     *
     * @return {@code true} if the buffer is empty, {@code false} otherwise
     */
    private static boolean isEmpty(long[] buffer) {
        for (long l : buffer)
            if (l != 0)
                return true;
        return false;
    }

    /**
     * Compares the value of this instance with the value of the specified instance.
     *
     * @param other the {@link QuadrupleImmutable} to compare with
     *
     * @return a negative integer, zero, or a positive integer as the value of this instance is less than,
     * equal to, or greater than the value of the specified instance.
     */
    @Override
    public int compareTo(QuadrupleImmutable other) {

        if (isNaN())
            return other.isNaN() ? 0 : 1;
        if (other.isNaN())
            return -1;

        // For Doubles, -0 < 0. Do it the same way
        if (negative != other.negative)
            return negative ? -1 : 1;

        // Signs are equal -- compare exponents (unsigned)
        int result = Integer.compareUnsigned(exponent, other.exponent);

        if (result == 0)
            result = Long.compareUnsigned(mantHi, other.mantHi);
        if (result == 0)
            result = Long.compareUnsigned(mantLo, other.mantLo);

        if (negative) result = -result;
        return result;
    }

    /**
     * Computes a hashcode for this {@link QuadrupleImmutable},
     * based on the values of its fields.
     *
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (isNaN())
            return HASH_CODE_OF_NAN;
        final int prime = 31;
        int result = 1;
        result = prime * result + exponent;
        result = prime * result + Long.hashCode(mantHi);
        result = prime * result + Long.hashCode(mantLo);
        result = prime * result + (negative ? 1231 : 1237);
        return result;
    }

    @Override
    public String toString() {
        return QuadrupleParser.toString(this);
    }

}
