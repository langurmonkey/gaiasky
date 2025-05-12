/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities to convert {@link Quadruple}s to and from {@link String}s.
 */
public class QuadrupleParser {

    /**
     * Parses a string containing a floating-point number and sets the fields
     * of the owner (a {@code Quadruple} instance)
     * <br>uses static arrays
     * <b><i>BUFFER_4x64_B, BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_6x32_C, BUFFER_12x32</i></b>
     *
     * @param source input string to parse
     * @param owner  the Quadruple instance whose fields are to set so that it has the value presented by the <b>source</b>
     *
     * @return the <b>owner</b> with the values of the fields modified to correspond to the value presented by the <b>source</b>
     */
    public static Quadruple parse(String source, Quadruple owner) {
        return NumberParser.parse(source, owner);
    }

    public static String toString(Quadruple q) {
        return toString(q.isNegative(), q.exponent(), q.mantHi(), q.mantLo());
    }

    public static String toString(QuadrupleImmutable f) {
        return toString(f.negative(), f.exponent(), f.mantHi(), f.mantLo());
    }

    private static final long[] BUFFER_4x32_A = new long[4];

    private static final long[] BUFFER_4x64_A = new long[4];
    private static final long[] BUFFER_4x64_B = new long[4];

    private static final long[] BUFFER_6x32_A = new long[6];
    private static final long[] BUFFER_6x32_B = new long[6];
    private static final long[] BUFFER_6x32_C = new long[6];

    private static final long[] BUFFER_10x32_A = new long[10];

    private static final long[] BUFFER_12x32 = new long[12];

    public static final int EXPONENT_OF_INFINITY = 0xFFFF_FFFF;
    public static final int EXPONENT_BIAS = 0x7FFF_FFFF;
    private static final int SIGNIFICANT_DIGITS = 40;
    private static final char[] ZEROS = "0000000000000000000000000000000000000000".toCharArray(); // 40 zeros
    private static final long LOWER_32_BITS = 0x0000_0000_FFFF_FFFFL;
    private static final long HIGHER_32_BITS = 0xFFFF_FFFF_0000_0000L;
    private static final long POW_2_31_L = 0x8000_0000L; // 2^31
    private static final long[] MIN_NORMAL_QUASIDEC = {
            -646456992,
            0x3a20_e831_7231_f10aL,
            0x7e1e_e626_d389_6445L,
            0x9767_1787_1b08_457bL,
    };

    public static String toString(boolean negative, int exponent, long mantHi, long mantLo) {

        if (exponent == EXPONENT_OF_INFINITY)
            return ((mantHi | mantLo) != 0) ? "NaN" : (negative) ? "-Infinity" : "Infinity";

        if ((exponent | mantHi | mantLo) == 0)
            return negative ? "-0.0" : "0.0";

        final int exp2 = exponent - EXPONENT_BIAS;

        // Decimal mantissa M and decimal exponent E are found from the binary mantissa m and the binary exponent e as
        // M = m * 2^e / 10^E.
        // Since E is always either (floor((e+1)*log(2))) or (floor((e+1)*log(2)) - 1),
        // we can find E' = floor((e+1)*log(2)) and M' = m * 2^e / 10^E',
        // and if M' < 1, we just multiply it by 10 and subtract 1 from E' to obtain M and E.
        // To avoid division by a power of 10, we multiply the mantissa by a power of 2,
        // encoded in a form of (what I call) quasi-decimal number,
        // that is an array of 4 longs, where qd[0] contains the decimal exponent of the number,
        // and qd[1] - qd[3] contain 192 bits of the decimal mantissa, divided by 10,
        // so that 1.0 looks like 0x1999_9999..., and 9.9999999... looks like 0xFFFF_FFFF_.
        // (in other words, it ranges from 1/10 * 2^192 to 9.999... * 2^192)
        // The result is in the same form, that allows for easy conversion into a string of decimal digits.

        long[] mant10 = BUFFER_4x64_A;
        if (exponent == 0) {
            mant10 = multMantByMinNormal(mant10, mantHi, mantLo);
        } else {
            mant10 = multMantByPowerOfTwo(powerOfTwo(exp2), mant10, exponent, mantHi, mantLo);
        }
        final StringBuilder mantStr = decimalMantToString(mant10);
        final int exp10 = (int) mant10[0] - 1;

        mantStr.insert(1, '.');
        mantStr.append("e").append(String.format("%+03d", exp10));
        if (negative) mantStr.insert(0, '-');

        return mantStr.toString();
    }

    /**
     * Converts the decimal mantissa of a number given in a binary form into
     * a string of decimal digits of the required length.
     * Rounds it up as needed and corrects the number's exponent in case of overflow caused by rounding up (e.g. 9.9999...e-1 -> 1.0e0)
     * <br>Uses static arrays
     * <b><i>BUFFER_6x32_A</i></b>
     * <pre>
     * @param qdNumber contains a quasi-decimal representation of the number
     *    ({@code qdNumber[0]} -- decimal exponent,
     *     {@code qdNumber[1]..qdNumber[3]} -- decimal mantissa divided by 10)}</pre>
     */
    private static StringBuilder decimalMantToString(long[] qdNumber) {
        final long[] multBuffer = BUFFER_6x32_A;
        final StringBuilder sb = convertMantToString(qdNumber, multBuffer);

        if ((multBuffer[0] & 0x8000_0000L) != 0)
            if (addCarry(sb) == 1)
                qdNumber[0]++;

        if (sb.length() < SIGNIFICANT_DIGITS)
            sb.append(ZEROS, 0, SIGNIFICANT_DIGITS - sb.length());

        return sb;
    }

    /**
     * Adds one to a decimal number represented as a sequence of decimal digits contained in {@code StringBuilder}.
     * propagates carry as needed, so that {@code addCarryTo("6789") = "6790", addCarryTo("9999") = "10000"} etc.
     *
     * @param sb a {@code StringBuilder} containing the number that is added one to
     *
     * @return 1 if an additional higher "1" was added in front of the number as a result of rounding-up,
     * 0 otherwise
     */
    private static int addCarry(StringBuilder sb) {
        for (int i = sb.length() - 1; i >= 0; i--) {
            final char c = sb.charAt(i);
            if (c == '9') sb.setCharAt(i, '0');
            else {
                sb.setCharAt(i, (char) (c + 1));
                return 0;
            }
        }
        sb.insert(0, '1');
        sb.deleteCharAt(sb.length() - 1);
        return 1;
    }

    /**
     * Converts the decimal mantissa of a number given in a binary form into
     * a string of decimal digits of the length that is less or equal to {@code maxLen}.
     * Leaves a result of multiplying mantissa by {@code 10^maxLen} in the {@code multBuffer}
     * <pre>
     * @param qdNumber contains a quasi-decimal representation of the number (packed)
     *    ({@code qdNumber[0]} -- decimal exponent,
     *     {@code qdNumber[1]..qdNumber[3]} -- decimal mantissa divided by 10)}</pre>
     *
     * @param multBuffer a buffer of 6 longs to store interim products of m * 10^n
     *
     * @return a new {@code StringBuilder}, containing decimal digits of the number
     */
    private static StringBuilder convertMantToString(long[] qdNumber, long[] multBuffer) {
        final StringBuilder sb = new StringBuilder(SIGNIFICANT_DIGITS);
        unpack_3x64_to_6x32(qdNumber, multBuffer);
        int charCount = 0;
        do {
            multBuffBy10(multBuffer);
            sb.append(Character.forDigit((int) (multBuffer[0] >>> 32), 10));
            charCount++;
        } while (charCount < SIGNIFICANT_DIGITS && !isEmpty(multBuffer));
        return sb;
    }

    /**
     * Multiplies the fractional part of the mantissa of this instance
     * of Quadruple that is expected to be subnormal,
     * by 192-bit quasi-decimal value of MIN_NORMAL
     * <br>Uses static buffers
     * <b><i>BUFFER_4x32_A, BUFFER_6x32_A, BUFFER_10x32_A</i></b>
     *
     * @param product_4x64 a buffer of 4 longs to be filled with the result
     *
     * @return product_4x64, filled with the product in a form of a 192-bit quasi-decimal value
     */
    private static long[] multMantByMinNormal(long[] product_4x64, long mantHi, long mantLo) {
        final long[] factor_6x32 = BUFFER_6x32_A;
        unpack_3x64_to_6x32(MIN_NORMAL_QUASIDEC, factor_6x32);
        final long decimalExpOfPow2 = MIN_NORMAL_QUASIDEC[0];

        final long[] buffer_10x32 = BUFFER_10x32_A;
        Arrays.fill(buffer_10x32, 0);
        return multMantBy192bits(factor_6x32, decimalExpOfPow2, product_4x64, buffer_10x32, 0, mantHi, mantLo);
    }

    /**
     * Multiplies the mantissa of this instance
     * of Quadruple by 192-bit quasi-decimal value of a power of 2.
     * <br>Uses static buffers
     * <b><i>BUFFER_4x32_A, BUFFER_6x32_A, BUFFER_10x32_A</i></b>
     *
     * @param product_4x64 a buffer of 4 longs to be filled with the result
     *
     * @return product_4x64, filled with the product in a form of a 192-bit quasi-decimal value
     */
    private static long[] multMantByPowerOfTwo(long[] pow2, long[] product_4x64, int exponent, long mantHi, long mantLo) {
        final long[] factor_6x32 = BUFFER_6x32_A;
        unpack_3x64_to_6x32(pow2, factor_6x32);
        final long decimalExpOfPow2 = pow2[0];

        final long[] buffer_10x32 = BUFFER_10x32_A;
        Arrays.fill(buffer_10x32, 0);
        System.arraycopy(factor_6x32, 0, buffer_10x32, 0, 6);

        return multMantBy192bits(factor_6x32, decimalExpOfPow2, product_4x64, buffer_10x32, exponent, mantHi, mantLo);
    }

    /**
     * Multiplies the mantissa of this instance by a 192-bit number (a power of 2),
     * given as the content of an unpacked buffer of 6 longs (6 x 32 bits) and the decimal exponent,
     * corrects possible overflow or underflow to ensure that the product is within the range
     * 1/10 * 2^192 .. 9.999.../10 and thus fits the quasi-decimal format, packs it into
     * 3 less significant words of {@code product_4x64}, corrects the exponent respectively
     * and puts it in the most significant word of {@code product_4x64}.
     * Used to multiple a quadruple value by a power of 2 while converting it to a String.<br>
     * Uses static buffers <b><i>BUFFER_4x32_A</i></b>
     *
     * @param factor_6x32      factor 2, 192 bits as unpacked buffer 6 x 32, without the integer part (implicit unity)
     * @param decimalExpOfPow2 decimal exponent of the power of 2, whose mantissa is in
     * @param product_4x64     buffer of 4 longs to hold the product
     * @param buffer_10x32     temporal buffer used for multiplication
     *
     * @return product_4x64 filled with the product
     */
    private static long[] multMantBy192bits(long[] factor_6x32, final long decimalExpOfPow2,
                                            long[] product_4x64, long[] buffer_10x32,
                                            int exponent, long mantHi, long mantLo) {
        unpackQuadToBuff(mantHi, mantLo);

        for (int i = 5; i >= 0; i--)
            for (int j = 3; j >= 0; j--) {
                final long product = factor_6x32[i] * BUFFER_4x32_A[j];
                buffer_10x32[j + i + 1] += product & LOWER_32_BITS;
                buffer_10x32[j + i] += product >>> 32;
            }

        for (int i = 9; i > 0; i--) {
            buffer_10x32[i - 1] += buffer_10x32[i] >>> 32;
            buffer_10x32[i] &= LOWER_32_BITS;
        }

        final int expCorrection = (exponent == 0) ?
                correctPossibleUnderflow(buffer_10x32) :
                correctPossibleOverflow(buffer_10x32);

        pack_10x32_to_3x64(buffer_10x32, product_4x64);
        product_4x64[0] = decimalExpOfPow2 + expCorrection;
        return product_4x64;
    }

    /**
     * Corrects possible underflow of the decimal mantissa, passed in the {@code buffer_10x32},
     * by multiplying it by a power of ten. The corresponding value to adjust the decimal exponent is returned as the result
     *
     * @param buffer_10x32 a buffer containing the mantissa to be corrected
     *
     * @return a corrective (addition) that is needed to adjust the decimal exponent of the number
     */
    private static int correctPossibleUnderflow(long[] buffer_10x32) {
        int expCorr = 0;
        while (isLessThanOne(buffer_10x32)) { // Underflow
            multBuffBy10(buffer_10x32);
            expCorr -= 1;
        }
        return expCorr;
    }

    /**
     * Checks if the unpacked quasi-decimal value held in the given buffer
     * is less than one (in this format, one is represented as { 0x1999_9999L, 0x9999_9999L, 0x9999_9999L,...}
     *
     * @param buffer a buffer containing the value to check
     *
     * @return {@code true}, if the value is less than one
     */
    private static boolean isLessThanOne(long[] buffer) {
        if (buffer[0] < 0x1999_9999L) return true;
        if (buffer[0] > 0x1999_9999L) return false;

        for (int i = 1; i < buffer.length; i++) {
            if (buffer[i] < 0x9999_9999L) return true;
            if (buffer[i] > 0x9999_9999L) return false;
        }
        return false;
    }

    /**
     * Corrects possible overflow of the decimal mantissa, passed in the {@code buffer_10x32},
     * by dividing it by a power of ten. The corresponding value to adjust the decimal exponent is returned as the result
     *
     * @param buffer_10x32 a buffer containing the mantissa to be corrected
     *
     * @return a corrective (addition) that is needed to adjust the decimal exponent of the number
     */
    private static int correctPossibleOverflow(long[] buffer_10x32) {
        int expCorr = 0;
        if ((buffer_10x32[0] & HIGHER_32_BITS) != 0) { // Overflow
            divBuffBy10(buffer_10x32);
            expCorr = 1;
        }
        return expCorr;
    }

    /**
     * Unpacks the mantissa of given quadruple to 4 longs of buffer,
     * so that each word of the buffer contains the corresponding 32 bits of the mantissa
     * in its least significant 32 bits
     */
    private static void unpackQuadToBuff(long mantHi, long mantLo) {
        BUFFER_4x32_A[0] = mantHi >>> 32;                // big-endian, highest word
        BUFFER_4x32_A[1] = mantHi & LOWER_32_BITS;
        BUFFER_4x32_A[2] = mantLo >>> 32;
        BUFFER_4x32_A[3] = mantLo & LOWER_32_BITS;
    }

    /**
     * Multiplies the unpacked value stored in the given buffer by 10
     *
     * @param buffer contains the unpacked value to multiply (32 least significant bits are used)
     */
    private static void multBuffBy10(long[] buffer) {
        final int maxIdx = buffer.length - 1;
        buffer[0] &= LOWER_32_BITS;
        buffer[maxIdx] *= 10;
        for (int i = maxIdx - 1; i >= 0; i--) {
            buffer[i] = buffer[i] * 10 + (buffer[i + 1] >>> 32);
            buffer[i + 1] &= LOWER_32_BITS;
        }
    }

    /**
     * Divides the unpacked value stored in the given buffer by 10
     *
     * @param buffer contains the unpacked value to divide (32 least significant bits are used)
     */
    private static void divBuffBy10(long[] buffer) {
        long r;
        final int maxIdx = buffer.length - 1;
        for (int i = 0; i <= maxIdx; i++) { // big/endian
            r = Long.remainderUnsigned(buffer[i], 10);
            buffer[i] = Long.divideUnsigned(buffer[i], 10);
            if (i < maxIdx)
                buffer[i + 1] += r << 32;
        }
    }

    /**
     * Calculates the required power and returns the result in
     * the quasi-decimal format (an array of longs, where result[0] is the decimal exponent
     * of the resulting value, and result[1] -- result[3] contain 192 bits of the mantissa divided by ten
     * (so that 8 looks like  <pre>{@code {1, 0xCCCC_.._CCCCL, 0xCCCC_.._CCCCL, 0xCCCC_.._CCCDL}}}</pre>
     * uses static arrays
     * <b><i>BUFFER_4x64_B</b>, BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_12x32</i></b>,
     *
     * @param exp the power to raise 2 to
     *
     * @return the value of {@code 2^exp}
     */
    private static long[] powerOfTwo(long exp) {
        if (exp == 0)
            return POS_POWERS_OF_2[0];

        long[][] powers = POS_POWERS_OF_2;
        if (exp < 0) {
            exp = -exp;
            powers = NEG_POWERS_OF_2;
        }

        long currPowOf2 = POW_2_31_L;
        int idx = powers.length - 1;
        long[] power = null;

        while (exp > 0) {
            if (exp >= currPowOf2) {
                if (power == null)
                    power = powers[idx];
                else
                    power = multPacked3x64_AndAdjustExponent(power, powers[idx]);
                exp -= currPowOf2;
            }
            idx--;
            currPowOf2 >>>= 1;
        }

        return power;
    }


    /**
     * Packs the unpacked quasi-decimal value contained in unpackedQDMant
     * (which uses only the least significant 32 bits of each word)
     * to packed quasi-decimal value (whose exponent should be in v[0] and 192 bits of mantissa in v[1]..v[3] )
     *
     * @param unpackedQDMant a buffer of at least 6 longs, containing the unpacked mantissa
     * @param packedQDValue  a buffer of at least 4 longs to hold the result
     */
    private static void pack_10x32_to_3x64(long[] unpackedQDMant, long[] packedQDValue) {
        packedQDValue[1] = (unpackedQDMant[0] << 32) + unpackedQDMant[1];
        packedQDValue[2] = (unpackedQDMant[2] << 32) + unpackedQDMant[3];
        packedQDValue[3] = (unpackedQDMant[4] << 32) + unpackedQDMant[5];
    }

    /**
     * converts 192 most significant bits of the mantissa of a number from an unpacked quasi-decimal form (where 32 least significant bits only used)
     * to a packed quasi-decimal form (where buff[0] contains the exponent and buff[1]..buff[3] contain 3 x 64 = 192 bits of mantissa)
     *
     * @param result a buffer of at least 4 long to hold the packed value
     */
    private static void pack_12x32_to_3x64(long[] result) {
        result[1] = (BUFFER_12x32[0] << 32) + BUFFER_12x32[1];
        result[2] = (BUFFER_12x32[2] << 32) + BUFFER_12x32[3];
        result[3] = (BUFFER_12x32[4] << 32) + BUFFER_12x32[5];
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
     * Multiplies two quasi-decimal numbers
     * contained in buffers of 3 x 64 bits with exponents, puts the product to <b><i>BUFFER_4x64_B</i></b><br>
     * and returns it.
     * Both each of the buffers and the product contain 4 longs - exponent and 3 x 64 bits of mantissa.
     * If the higher word of mantissa of the product is less than 0x1999_9999_9999_9999L (i.e. mantissa is less than 0.1)
     * multiplies mantissa by 10 and adjusts the exponent respectively.
     * <br>uses static arrays
     * <b><i>BUFFER_4x64_B, BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_12x32</i></b>,
     * (Big-endian)
     *
     * @return BUFFER_4x64_B
     */
    private static long[] multPacked3x64_AndAdjustExponent(long[] factor1, long[] factor2) {
        multPacked3x64_simply(factor1, factor2);
        final int expCorr = correctPossibleUnderflow(BUFFER_12x32);
        long[] result = BUFFER_4x64_B;
        pack_12x32_to_3x64(result);

        result[0] = factor1[0] + factor2[0] + expCorr;
        return result;
    }

    /**
     * Multiplies mantissas of two packed quasi-decimal values
     * (each is an array of 4 longs, exponent + 3 x 64 bits of mantissa)
     * Returns the product as unpacked buffer of 12 x 32 (12 x 32 bits of product)<br>
     * uses static arrays <b><i>BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_12x32</b></i>
     *
     * @param factor1 an array of longs containing factor 1 as packed quasi-decimal
     * @param factor2 an array of longs containing factor 2 as packed quasi-decimal
     */
    private static void multPacked3x64_simply(long[] factor1, long[] factor2) {
        Arrays.fill(BUFFER_12x32, 0);
        unpack_3x64_to_6x32(factor1, BUFFER_6x32_A);
        unpack_3x64_to_6x32(factor2, BUFFER_6x32_B);

        for (int i = 5; i >= 0; i--)
            for (int j = 5; j >= 0; j--) {
                final long part = BUFFER_6x32_A[i] * BUFFER_6x32_B[j];
                BUFFER_12x32[j + i + 1] += part & LOWER_32_BITS;
                BUFFER_12x32[j + i] += part >>> 32;
            }

        for (int i = 11; i > 0; i--) {
            BUFFER_12x32[i - 1] += BUFFER_12x32[i] >>> 32;
            BUFFER_12x32[i] &= LOWER_32_BITS;
        }
    }

    /**
     * An array of positive powers of two, each value consists of 4 longs: decimal exponent and 3 x 64 bits of mantissa, divided by ten
     * Used to find an arbitrary power of 2 (by powerOfTwo(long exp) )
     */
    private static final long[][] POS_POWERS_OF_2 = {
            {1, 0x1999_9999_9999_9999L, 0x9999_9999_9999_9999L, 0x9999_9999_9999_999aL},
            {1, 0x3333_3333_3333_3333L, 0x3333_3333_3333_3333L, 0x3333_3333_3333_3334L},
            {1, 0x6666_6666_6666_6666L, 0x6666_6666_6666_6666L, 0x6666_6666_6666_6667L},
            {2, 0x28f5_c28f_5c28_f5c2L, 0x8f5c_28f5_c28f_5c28L, 0xf5c2_8f5c_28f5_c290L},
            {3, 0x4189_374b_c6a7_ef9dL, 0xb22d_0e56_0418_9374L, 0xbc6a_7ef9_db22_d0e6L},
            {5, 0xa7c5_ac47_1b47_8423L, 0x0fcf_80dc_3372_1d53L, 0xcddd_6e04_c059_2104L},
            {10, 0x6df3_7f67_5ef6_eadfL, 0x5ab9_a207_2d44_268dL, 0x97df_837e_6748_956eL},
            {20, 0x2f39_4219_2484_46baL, 0xa23d_2ec7_29af_3d61L, 0x0607_aa01_67dd_94cbL},
            {39, 0x571c_bec5_54b6_0dbbL, 0xd5f6_4baf_0506_840dL, 0x451d_b70d_5904_029bL},
            {78, 0x1da4_8ce4_68e7_c702L, 0x6520_247d_3556_476dL, 0x1469_caf6_db22_4cfaL},
            {155, 0x2252_f0e5_b397_69dcL, 0x9ae2_eea3_0ca3_ade0L, 0xeeaa_3c08_dfe8_4e30L},
            {309, 0x2e05_5c9a_3f6b_a793L, 0x1658_3a81_6eb6_0a59L, 0x22c4_b082_6cf1_ebf7L},
            {617, 0x52bb_45e9_cf23_f17fL, 0x7688_c076_06e5_0364L, 0xb344_79aa_9d44_9a57L},
            {1234, 0x1abc_81c8_ff5f_846cL, 0x8f5e_3c98_53e3_8c97L, 0x4506_0097_f3bf_9296L},
            {2467, 0x1bec_53b5_10da_a7b4L, 0x4836_9ed7_7dbb_0eb1L, 0x3b05_587b_2187_b41eL},
            {4933, 0x1e75_063a_5ba9_1326L, 0x8abf_b8e4_6001_6ae3L, 0x2800_8702_d29e_8a3cL},
            {9865, 0x243c_5d8b_b5c5_fa55L, 0x40c6_d248_c588_1915L, 0x4c0f_d99f_d5be_fc22L},
            {19729, 0x334a_5570_c3f4_ef3cL, 0xa13c_36c4_3f97_9c90L, 0xda7a_c473_555f_b7a8L},
            {39457, 0x66c3_0444_5dd9_8f3bL, 0xa8c2_93a2_0e47_a41bL, 0x4c5b_03dc_1260_4964L},
            {78914, 0x293f_fbf5_fb02_8cc4L, 0x89d3_e5ff_4423_8406L, 0x369a_339e_1bfe_8c9bL},
            {157827, 0x4277_92fb_b68e_5d20L, 0x7b29_7cd9_fc15_4b62L, 0xf091_4211_4aa9_a20cL},
            {315653, 0xac92_bc65_ad5c_08fcL, 0x00be_eb11_5a56_6c19L, 0x4ba8_82d8_a462_2437L},
            {631306, 0x7455_8144_0f92_e80eL, 0x4da8_22cf_7f89_6f41L, 0x509d_5986_7816_4ecdL},
            {1262612, 0x34dd_99b4_c695_23a5L, 0x64bc_2e8f_0d8b_1044L, 0xb03b_1c96_da5d_d349L},
            {2525223, 0x6d2b_bea9_d6d2_5a08L, 0xa0a4_606a_88e9_6b70L, 0x1820_63bb_c2fe_8520L},
            {5050446, 0x2e8e_47d6_3bfd_d6e3L, 0x2b55_fa89_76ea_a3e9L, 0x1a6b_9d30_8641_2a73L},
            {10100891, 0x54aa_68ef_a1d7_19dfL, 0xd850_5806_612c_5c8fL, 0xad06_8837_fee8_b43aL},
            {20201782, 0x1c00_464c_cb7b_ae77L, 0x9e38_7778_4c77_982cL, 0xd94a_f3b6_1717_404fL},
            {40403563, 0x1ea0_99c8_be2b_6cd0L, 0x8bfb_6d53_9fa5_0466L, 0x6d3b_c37e_69a8_4218L},
            {80807125, 0x24a4_57f4_66ce_8d18L, 0xf2c8_f3b8_1bc6_bb59L, 0xa78c_7576_92e0_2d49L},
            {161614249, 0x3472_5667_7aba_6b53L, 0x3fbf_90d3_0611_a67cL, 0x1e03_9d87_e0bd_b32bL},
            {323228497, 0x6b72_7daf_0fd3_432aL, 0x71f7_1121_f9e4_200fL, 0x8fcd_9942_d486_c10cL},
            {646456994, 0x2d18_e844_84d9_1f78L, 0x4079_bfe7_829d_ec6fL, 0x2155_1643_e365_abc6L},
    };

    /**
     * An array of negative powers of two, each value consists of 4 longs: decimal exponent and 3 x 64 bits of mantissa, divided by ten.
     * Used to find an arbitrary power of 2 (by powerOfTwo(long exp) )
     */
    private static final long[][] NEG_POWERS_OF_2 = {
            {1, 0x1999_9999_9999_9999L, 0x9999_9999_9999_9999L, 0x9999_9999_9999_999aL},
            {0, 0x8000_0000_0000_0000L, 0x0000_0000_0000_0000L, 0x0000_0000_0000_0000L},
            {0, 0x4000_0000_0000_0000L, 0x0000_0000_0000_0000L, 0x0000_0000_0000_0001L},
            {-1, 0xa000_0000_0000_0000L, 0x0000_0000_0000_0000L, 0x0000_0000_0000_0000L},
            {-2, 0x6400_0000_0000_0000L, 0x0000_0000_0000_0000L, 0x0000_0000_0000_0000L},
            {-4, 0x2710_0000_0000_0000L, 0x0000_0000_0000_0000L, 0x0000_0000_0000_0001L},
            {-9, 0x3b9a_ca00_0000_0000L, 0x0000_0000_0000_0000L, 0x0000_0000_0000_0001L},
            {-19, 0x8ac7_2304_89e8_0000L, 0x0000_0000_0000_0000L, 0x0000_0000_0000_0000L},
            {-38, 0x4b3b_4ca8_5a86_c47aL, 0x098a_2240_0000_0000L, 0x0000_0000_0000_0001L},
            {-77, 0xdd15_fe86_affa_d912L, 0x49ef_0eb7_13f3_9ebeL, 0xaa98_7b6e_6fd2_a002L},
            {-154, 0xbeee_fb58_4aff_8603L, 0xaafb_550f_facf_d8faL, 0x5ca4_7e4f_88d4_5371L},
            {-308, 0x8e67_9c2f_5e44_ff8fL, 0x570f_09ea_a7ea_7648L, 0x5961_db50_c6d2_b888L},
            {-616, 0x4f37_1b33_99fc_2ab0L, 0x8170_041c_9feb_05aaL, 0xc7c3_4344_7c75_bcf6L},
            {-1233, 0xf51e_9281_7901_3fd3L, 0xde4b_d12c_de4d_985cL, 0x4a57_3ca6_f94b_ff14L},
            {-2466, 0xeab3_8812_7bcc_aff7L, 0x1667_6391_42b9_fbaeL, 0x775e_c999_5e10_39fbL},
            {-4932, 0xd72c_b2a9_5c7e_f6ccL, 0xe81b_f1e8_25ba_7515L, 0xc2fe_b521_d6cb_5dcdL},
            {-9864, 0xb4dc_1be6_6045_02dcL, 0xd491_079b_8eef_6535L, 0x578d_3965_d24d_e84dL},
            {-19728, 0x7fc6_447b_ee60_ea43L, 0x2548_da5c_8b12_5b27L, 0x5f42_d114_2f41_d349L},
            {-39456, 0x3fc6_5180_f88a_f8fbL, 0x6a69_15f3_8334_9413L, 0x063c_3708_b6ce_b291L},
            {-78913, 0x9ee0_197c_8dcd_55bfL, 0x2b2b_9b94_2c38_f4a2L, 0x0f8b_a634_e9c7_06aeL},
            {-157826, 0x6299_63a2_5b8b_2d79L, 0xd00b_9d22_86f7_0876L, 0xe970_0470_0c36_44fcL},
            {-315652, 0x25f9_cc30_8cee_f4f3L, 0x40f1_9543_911a_4546L, 0xa2cd_3894_52cf_c366L},
            {-631305, 0x3855_97b0_d47e_76b8L, 0x1b9f_67e1_03bf_2329L, 0xc311_9848_5959_85f7L},
            {-1262611, 0x7bf7_95d2_76c1_2f66L, 0x66a6_1d62_a446_659aL, 0xa1a4_d73b_ebf0_93d5L},
            {-2525222, 0x3c07_d96a_b1ed_7799L, 0xcb73_55c2_2cc0_5ac0L, 0x4ffc_0ab7_3b1f_6a49L},
            {-5050445, 0x8cc4_cd8c_3ede_fb9aL, 0x6c8f_f86a_90a9_7e0cL, 0x166c_fddb_f98b_71bfL},
            {-10100890, 0x4d67_d81c_c88e_1228L, 0x1d7c_fb06_666b_79b3L, 0x7b91_6728_aaa4_e70dL},
            {-20201781, 0xea0c_5549_4e7a_552dL, 0xb88c_b948_4bb8_6c61L, 0x8d44_893c_610b_b7dFL},
            {-40403562, 0xd5fa_8c82_1ec0_c24aL, 0xa80e_46e7_64e0_f8b0L, 0xa727_6bfa_432f_ac7eL},
            {-80807124, 0xb2da_e307_426f_6791L, 0xc970_b82f_58b1_2918L, 0x0472_592f_7f39_190eL},
            {-161614248, 0x7cf5_1edd_8a15_f1c9L, 0x656d_ab34_98f8_e697L, 0x12da_a2a8_0e53_c807L},
            {-323228496, 0x3cfe_609a_b588_3c50L, 0xbec8_b5d2_2b19_8871L, 0xe184_7770_3b46_22b4L},
            {-646456993, 0x9152_447b_9d7c_da9aL, 0x3b4d_3f61_10d7_7aadL, 0xfa81_bad1_c394_adb4L},
    };

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
                return false;
        return true;
    }

    /** Max value of the decimal exponent, corresponds to EXPONENT_OF_MAX_VALUE */
    private static final int MAX_EXP10 = 646456993;
    /** Min value of the decimal exponent, corresponds to EXPONENT_OF_MIN_NORMAL */
    private static final int MIN_EXP10 = -646457032;      // corresponds

    /** Approximate value of log<sub>2</sub>(10) */
    private static final double LOG2_10 = Math.log(10) / Math.log(2);
    /** Approximate value of log<sub>2</sub>(e) */
    private static final double LOG2_E = 1 / Math.log(2.0);

    /** Minimum possible positive value, 6.672829482607474308148353774991346115977e-646457032 */
    private static final Quadruple MIN_VALUE = new Quadruple().assignMinValue();
    /** Maximum possible value, 1.761613051683963353207493149791840285665e+646456993 */
    private static final Quadruple MAX_VALUE = new Quadruple().assignMaxValue();
    /** Minimum possible positive normal value, 2.270646210401492537526567265179587581247e-646456993 */
    private static final Quadruple MIN_NORMAL = new Quadruple().assignMinNormal();
    /** Not a number */
    private static final Quadruple NaN = new Quadruple().assignNaN();
    private static final Quadruple NEGATIVE_INFINITY = new Quadruple().assignNegativeInfinity();
    private static final Quadruple POSITIVE_INFINITY = new Quadruple().assignPositiveInfinity();

    /**
     * A class that parses a string containing a numeric value and sets the fields of its {@code Quadruple} owner accordingly.
     * Contains involved static methods, constants, and interim variables
     */
    private static class NumberParser {

        /** A pattern used to strip leading zeroes from integer numbers */
        private static final Pattern LEADING_ZEROES_PTRN = Pattern.compile("(^0+)(\\d*)");

        /** The maximum number of digits in the mantissa that are taken into account */
        private static final int MAX_MANTISSA_LENGTH = 59;

        static Quadruple owner;

        /**
         * A mapping of string designations of special values,
         * used by {@link NumberParser#parse(String, Quadruple)}
         */
        private static final Map<String, Quadruple> QUADRUPLE_CONSTANTS = new HashMap<>() {{
            put("quadruple.min_value", MIN_VALUE);
            put("min_value", MIN_VALUE);
            put("quadruple.max_value", MAX_VALUE);
            put("max_value", MAX_VALUE);
            put("quadruple.min_normal", MIN_NORMAL);
            put("min_normal", MIN_NORMAL);
            put("quadruple.nan", NaN);
            put("nan", NaN);
            put("quadruple.negative_infinity", NEGATIVE_INFINITY);
            put("negative_infinity", NEGATIVE_INFINITY);
            put("-infinity", NEGATIVE_INFINITY);
            put("quadruple.positive_infinity", POSITIVE_INFINITY);
            put("positive_infinity", POSITIVE_INFINITY);
            put("infinity", POSITIVE_INFINITY);
            put("+infinity", POSITIVE_INFINITY);
        }};

        /**
         * A decomposer and container to extract and store the parts of the string representing a number.
         * Its fields are set by the {@link #decompose(String)} method and used to build a Quadruple value
         *
         * @author misa
         */
        private static class NumberParts {

            /** Decimal exponent of the number */
            private long exp10;
            /** Sign flag ({@code true} for negatives) */
            private boolean negative;
            /** Mantissa without the dot and leading/trailing zeros */
            private String mantStr;
            /** exponent correction, derived from mantissa */
            private int expCorrection;

            /***
             * A regex to parse floating-point number with a minimal framing
             * of methods to extract separate parts of the number
             * @author misa
             *
             */
            private static class FPStringRegex {
                private static final Pattern FP_STRING_PTRN = Pattern.compile(
                        "^([+\\-])?((\\d*\\.)?(\\d*))(e([+\\-])?0*(\\d+))?$",
                        Pattern.CASE_INSENSITIVE);

                private static Matcher m;

                private static void match(String source) {
                    m = FP_STRING_PTRN.matcher(source);
                    if (!m.find())
                        throw new NumberFormatException("Invalid number: '" + source + "'");
                }

                private static boolean negative() {
                    return ("-".equals(m.group(1)));
                }

                private static String expString() {
                    return m.group(5);
                }

                private static String intPartString() {
                    return m.group(3);
                }

                private static String fractPartString() {
                    return m.group(4);
                }

            }

            private String sourceStr;

            /**
             * Decomposes an input string containing a floating-point number
             * into parts (sign, mantissa, exponent, and necessary exponent correction depending on the mantissa)
             * and sets appropriately the inner fields to be used by subsequent processing
             *
             * @param source the source String
             *
             * @return the reference of this instance
             */
            private NumberParser.NumberParts decompose(String source) {
                this.sourceStr = source;
                NumberParser.NumberParts.FPStringRegex.match(source);

                negative = NumberParser.NumberParts.FPStringRegex.negative();
                exp10 = extractExp10(NumberParser.NumberParts.FPStringRegex.expString());
                expCorrection = buildMantString(NumberParser.NumberParts.FPStringRegex.intPartString(),
                                                NumberParser.NumberParts.FPStringRegex.fractPartString());

                return this;
            }

            /**
             * Builds a String containing the mantissa of the floating-point number being parsed
             * as a string of digits without trailing or leading zeros.
             * Finds the exponent correction depending on the point position and the number of leading zeroes.
             *
             * @param intPartString   the integer part of the mantissa, (m.b. including the dot)
             * @param fractPartString the integer part of the mantissa
             */
            private int buildMantString(String intPartString, String fractPartString) {
                int expCorrection = uniteMantString(intPartString, fractPartString);

                final Matcher m2 = LEADING_ZEROES_PTRN.matcher(mantStr);
                if (m2.find()) {
                    mantStr = m2.group(2);
                    expCorrection -= m2.group(1).length();
                }
                mantStr = mantStr.replaceFirst("0*$", "");
                return expCorrection;
            }

            /**
             * Unites the integer part of the mantissa with the fractional part and computes
             * necessary exponent correction that depends on the position of the decimal point
             *
             * @param intPartString   the integer part of the mantissa, may be null for empty mantissa (e.g. "e123"), or consist of only "."
             * @param fractPartString the fractional part of the mantissa, may be empty for e.g. "33.e5"
             *
             * @return the exponent correction to be added to the explicitly expressed number's exponent
             */
            private int uniteMantString(String intPartString, String fractPartString) {
                if (intPartString == null) {
                    intPartString = fractPartString;
                    fractPartString = "";
                }

                intPartString = intPartString.replaceFirst("\\.$", "");
                if (intPartString.isEmpty() && fractPartString.isEmpty())
                    throw new NumberFormatException("Invalid number: " + sourceStr);

                mantStr = intPartString + fractPartString;      // mantissa as a string
                return intPartString.length() - 1;              // 10.0 = 1e1, 1.0 = 1e0, 0.1 = 1e-1 etc;
            }

            private static final Pattern EXP_STR_PTRN = Pattern.compile("e([+\\-])?(\\d+)");

            /**
             * Extracts a long value of the exponent from a substring
             * containing the exponent of the floating-point number being parsed,
             * e.g. "e+646456993"
             *
             * @param expString substring containing the exponent, may be null if the number is in decimal format (without exponent)
             *
             * @return numeric exponent value
             */
            private static long extractExp10(String expString) {
                long exp10 = 0;
                if (expString != null) {
                    final Matcher m = EXP_STR_PTRN.matcher(expString);
                    if (m.find()) {
                        exp10 = parseLong(m.group(2));
                        if ("-".equals(m.group(1))) exp10 = -exp10;
                    }
                }
                return exp10;
            }

            /**
             * Parses a String containing an unsigned long number.
             * For values greater than   999_999_999_999_999_999 (1e18-1) returns Long.MAX_VALUE.
             *
             * @param longString string representation of a number
             *
             * @return a long value represented by the longString
             */
            private static long parseLong(String longString) {
                if (longString.length() > 18) return Long.MAX_VALUE;
                return Long.parseLong(longString);
            }

        }

        static final NumberParser.NumberParts PARTS = new NumberParser.NumberParts();


        /**
         * Parses a string containing a floating-point number and sets the fields
         * of the owner (a {@code Quadruple} instance)
         * <br>uses static arrays
         * <b><i>BUFFER_4x64_B, BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_6x32_C, BUFFER_12x32</i></b>
         *
         * @param source input string to parse
         * @param owner  the Quadruple instance whose fields are to set so that it has the value presented by the <b>source</b>
         *
         * @return the <b>owner</b> with the values of the fields modified to correspond to the value presented by the <b>source</b>
         */
        public static Quadruple parse(String source, Quadruple owner) {
            source = source.trim().toLowerCase();

            final Quadruple qConst = QUADRUPLE_CONSTANTS.get(source);
            if (qConst != null)
                return owner.assign(qConst);

            source = source.replaceAll("_", "");
            NumberParser.owner = owner;
            buildQuadruple(PARTS.decompose(source));
            return owner;
        }

        /**
         * Builds a quadruple value based on the parts of the decimal floating-point number.
         * Puts the value into the owner's fields
         * <br>uses static arrays
         * <b><i>BUFFER_4x64_B, BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_6x32_C, BUFFER_12x32</i></b>,
         *
         * @param parts contains parts of the parsed number -- integer and fractional parts of the decimal mantissa, exponent, and sign.
         */
        private static void buildQuadruple(NumberParser.NumberParts parts) {
            owner.setNegative(parts.negative);
            long exp10 = parts.exp10;
            final int exp10Corr = parseMantissa(parts);
            // and puts it into buff_6x32_C. Returns necessary exponent correction
            if (exp10Corr == 0 && isEmpty(BUFFER_6x32_C)) {
                owner.assignZero(false);
                return;
            }

            exp10 += exp10Corr;

            if (exceedsAcceptableExponentRange(exp10))
                return;

            final long exp2 = findBinaryExponent(exp10);
            findBinaryMantissa((int) exp10,
                               exp2
            );
        }

        /**
         * Finds the numeric value of the normalized decimal mantissa
         * in "quasi-decimal" format ( M * 2^192 / 10, so that 1.0 becomes 0x19..99, and 9.99..99 becomes 0xFF..FF)
         * and puts it into the given buffer.
         * Finds and returns the exponent correction to be added to the number's exponent
         * (depending on the position of the decimal point in the original mantissa and possible rounding up), so that
         * 0.123 corresponds to expCorr == -1,  1.23 to expCorr == 0, and 123000.0 to expCorr = 5.
         *
         * @param parts a {@code NumberParts} instance containing the parts of the number
         *
         * @return exponent correction to be added to the parsed number's exponent
         * /
         **/
        private static int parseMantissa(NumberParser.NumberParts parts) {
            if (parts.mantStr.isEmpty()) {
                Arrays.fill(BUFFER_6x32_C, 0);
                return 0;
            } else {
                return parseMantString(parts.mantStr) + parts.expCorrection;
            }
        }

        /**
         * Parses the given String, containing a long decimal number,
         * and sets its numeric 192-bit value in the given buffer.
         * May require to increment exponent if (10^n) - 1 ( == 99..99) gets rounded up to 10^n ( == 100..00),
         * returns 1 in such case, otherwise returns 1.
         *
         * @param mantStr a String containing the decimal number to be parsed
         *
         * @return exponent correction to be added to the explicit exponent of the number
         */
        private static int parseMantString(String mantStr) {
            int expCorr = 0;
            final StringBuilder sb = new StringBuilder(mantStr);
            // Limit the string length to avoid unnecessary fuss
            if (sb.length() > MAX_MANTISSA_LENGTH) {
                final boolean carry = sb.charAt(MAX_MANTISSA_LENGTH) >= '5';
                sb.delete(MAX_MANTISSA_LENGTH, sb.length());
                if (carry)
                    expCorr += addCarry(sb);
            }
            findNumericMantValue(sb);
            return expCorr;
        }

        /**
         * Converts a string of digits to a 192-bit "quasi-decimal" numeric value
         *
         * @param sb a StringBuilder containing the digits of the mantissa
         */
        private static void findNumericMantValue(StringBuilder sb) {
            Arrays.fill(BUFFER_6x32_C, 0);
            for (int i = sb.length() - 1; i >= 0; i--) {
                BUFFER_6x32_C[0] |= (long) Character.digit(sb.charAt(i), 10) << 32;
                divBuffBy10(BUFFER_6x32_C);
            }
        }

        /**
         * Checks that the decimal exponent value doesn't exceed the possible range.<br>
         * if exponent < MIN_EXP10, assigns 0 to the owner and returns {@code true}.
         * if exponent > MAX_EXP10, assigns Infinity to the owner and returns {@code true}.
         * Otherwise, returns {@code false}.
         */
        private static boolean exceedsAcceptableExponentRange(long exp10) {
            if (exp10 < MIN_EXP10) {
                owner.assignZero(false);
                return true;
            } else if (exp10 > MAX_EXP10) {
                owner.assignInfinity(false);
                return true;
            }
            return false;
        }

        /** (2^63) / 10 =~ 9.223372e17 */
        private static final double TWO_POW_63_DIV_10 = 922337203685477580d;

        /**
         * Finds binary exponent, using decimal exponent and mantissa.<br>
         * exp2 = exp10 * log<sub>2</sub>(10) + log<sub>2</sub>(mant)<br>
         *
         * @param exp10 decimal exponent
         *
         * @return found value of binary exponent
         */
        private static long findBinaryExponent(long exp10) {
            final long mant10 = BUFFER_6x32_C[0] << 31 | BUFFER_6x32_C[1] >>> 1;
            // 0x0CC..CCC -- 0x7FF..FFF (2^63/10 -- 2^63-1)
            final double mant10d = mant10 / TWO_POW_63_DIV_10;
            return (long) Math.floor((exp10) * LOG2_10 + log2(mant10d));
        }

        /**
         * Calculates log<sub>2</sub> of the given x
         *
         * @param x argument that can't be 0
         *
         * @return the value of log<sub>2</sub>(x)
         */
        private static double log2(double x) {
            // x can't be 0
            return LOG2_E * Math.log(x);
        }

        /**
         * Finds binary mantissa based on the given decimal mantissa and binary exponent,
         * <pre>mant<sub>2</sub> = mant<sub>10</sub> * 10^exp<sub>10</sub> / 2^exp<sub>2</sub></pre>
         * Assigns the found mantissa value to the owner's fields mantHi, mantLo and sets its binary exponent.
         * <br>uses static arrays
         * <b><i>BUFFER_4x64_B, BUFFER_6x32_A, BUFFER_6x32_B, BUFFER_12x32</i></b>,
         *
         * @param exp10 decimal exponent from the source string
         * @param exp2  binary mantissa found from decimal mantissa
         */
        private static void findBinaryMantissa(int exp10, long exp2) {
            final long[] powerOf2 = powerOfTwo(-exp2);
            long[] product = BUFFER_12x32;
            multUnpacked6x32bydPacked(powerOf2, product);
            multBuffBy10(product);

            if (powerOf2[0] != -exp10)
                multBuffBy10(product);

            exp2 += normalizeMant(product);
            exp2 += EXPONENT_BIAS;

            // For subnormal values, exp2 <= 0
            if (exp2 <= 0) {
                // Don't round subnormals up, makeSubnormal will round them,
                // unless bits 129..191 of the product >= 0xFFFF_FFFF_FFFF_FFE0L
                // (i.e. unless the fractional part of the mantissa >= (0.5 - 1.7e-18))

                if (product[4] == 0xFFFF_FFFFL
                        && (product[5] & 0xFFFF_FFE0L) == 0xFFFF_FFE0L)
                    exp2 += roundUp(product);

                fillOwnerMantissaFrom(product);
                if (exp2 <= 0)
                    exp2 = owner.makeSubnormal(exp2);
            } else {
                exp2 += roundUp(product);
                fillOwnerMantissaFrom(product);
            }

            owner.setExponent((int) exp2);
            if (owner.exponent() == EXPONENT_OF_INFINITY) {
                owner.setMantHi(0);
                owner.setMantLo(0);
            }
        }

        /**
         * Multiplies unpacked 192-bit value by a packed 192-bit factor
         * <br>uses static arrays
         * <b><i>BUFFER_6x32_B</i></b>
         *
         * @param factor2 an array of 4 longs containing packed quasi-decimal power of two
         * @param product a buffer of at least 12 longs to hold the product
         */
        private static void multUnpacked6x32bydPacked(long[] factor2, long[] product) {
            Arrays.fill(product, 0);
            unpack_3x64_to_6x32(factor2, BUFFER_6x32_B);
            factor2 = BUFFER_6x32_B;

            final int maxFactIdx = BUFFER_6x32_C.length - 1;

            for (int i = maxFactIdx; i >= 0; i--)
                for (int j = maxFactIdx; j >= 0; j--) {
                    final long part = BUFFER_6x32_C[i] * factor2[j];
                    product[j + i + 1] += part & LOWER_32_BITS;
                    product[j + i] += part >>> 32;
                }

            for (int i = 11; i > 0; i--) {
                product[i - 1] += product[i] >>> 32;
                product[i] &= LOWER_32_BITS;
            }
        }


        /**
         * Fills the mantissa of the owner with the higher 128 bits of the buffer
         *
         * @param mantissa a buffer containing unpacked mantissa (n longs, where only lower 32 bits of each word are used)
         */
        private static void fillOwnerMantissaFrom(long[] mantissa) {
            owner.setMantHi((mantissa[0] << 32) + mantissa[1]);
            owner.setMantLo((mantissa[2] << 32) + mantissa[3]);
        }

        /***
         * Makes sure that the (unpacked) mantissa is normalized,
         * i.e. buff[0] contains 1 in bit 32 (the implied integer part) and higher 32 of mantissa in bits 31..0,
         * and buff[1]..buff[4] contain other 96 bits of mantissa in their lower halves:
         * <pre>0x0000_0001_XXXX_XXXXL, 0x0000_0000_XXXX_XXXXL...</pre>
         * If necessary, divides the mantissa by appropriate power of 2 to make it normal.
         * @param mantissa a buffer containing unpacked mantissa
         * @return if the mantissa was not normal initially, a correction that should be added to the result's exponent, or 0 otherwise
         */
        private static int normalizeMant(long[] mantissa) {
            final int expCorr = 31 - Long.numberOfLeadingZeros(mantissa[0]);
            if (expCorr != 0)
                divBuffByPower2(mantissa, expCorr);
            return expCorr;
        }

        /**
         * Rounds up the contents of the unpacked buffer to 128 bits
         * by adding unity one bit lower than the lowest of these 128 bits.
         * If carry propagates up to bit 33 of buff[0], shifts the buffer rightwards
         * to keep it normalized.
         *
         * @param mantissa the buffer to get rounded
         *
         * @return 1 if the buffer was shifted, 0 otherwise
         */
        private static int roundUp(long[] mantissa) {
            addToBuff(mantissa, 5, 100);
            addToBuff(mantissa, 4, 0x8000_0000L);
            if ((mantissa[0] & (HIGHER_32_BITS << 1)) != 0) {
                divBuffByPower2(mantissa, 1);
                return 1;
            }
            return 0;
        }

        /**
         * Adds the summand to the idx-th word of the unpacked value stored in the buffer
         * and propagates carry as necessary
         *
         * @param buff    the buffer to add the summand to
         * @param idx     the index of the element to which the summand is to be added
         * @param summand the summand to add to the idx-th element of the buffer
         */
        private static void addToBuff(long[] buff, int idx, long summand) {
            final int maxIdx = idx;
            buff[maxIdx] += summand;
            for (int i = maxIdx; i > 0; i--) {
                if ((buff[i] & HIGHER_32_BITS) != 0) {
                    buff[i] &= LOWER_32_BITS;
                    buff[i - 1]++;
                } else break;
            }
        }

        /**
         * Divides the contents of the buffer by 2^exp2<br>
         * (shifts the buffer rightwards by exp2 if the exp2 is positive, and leftwards if it's negative),
         * keeping it unpacked (only lower 32 bits of each element are used, except the buff[0]
         * whose higher half is intended to contain integer part)
         *
         * @param buffer the buffer to divide
         * @param exp2   the exponent of the power of two to divide by, expected to be
         */
        private static void divBuffByPower2(long[] buffer, int exp2) {
            final int maxIdx = buffer.length - 1;
            final int backShift = 32 - Math.abs(exp2);

            if (exp2 > 0) {
                for (int i = maxIdx; i > 0; i--)
                    buffer[i] = (buffer[i] >>> exp2)
                            | ((buffer[i - 1] << backShift) & LOWER_32_BITS);
                buffer[0] = (buffer[0] >>> exp2);
            } else if (exp2 < 0) {
                exp2 = -exp2;
                buffer[0] = (buffer[0] << exp2) | (buffer[1] >> backShift);
                for (int i = 1; i < maxIdx; i++)
                    buffer[i] = ((buffer[i] << exp2) & LOWER_32_BITS)
                            | (buffer[i + 1] >> backShift);
                buffer[maxIdx] = (buffer[maxIdx] << exp2) & LOWER_32_BITS;
            }
        }

    }
}
