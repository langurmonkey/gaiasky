/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */
package gaiasky.util.parse;

import net.jafama.FastMath;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class Parser {
    /**
     * <p>
     * Quick long string parser that can handle negative and positive values.
     * </p>
     * <p>
     * Parser supports leading/trailing whitespace.
     * </p>
     *
     * @param input String to parse
     * @return Parsed long or 0 if the parsing fails
     */
    public static long parseLong(String input) {
        long result = 0;
        int pos = 0;
        int len = input.length();
        if (len == 0)
            return 0;
        char c = input.charAt(0);
        long sign = 1;

        // skip any starting white space
        while (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') {
            ++pos;
            if (pos >= len)
                return 0;
            c = input.charAt(pos);
        }

        // handle both - and + signs
        if (c == '-') {
            sign = -1;
            ++pos;
            if (pos >= len)
                return 0;
        } else if (c == '+') {
            ++pos;
            if (pos >= len)
                return 0;
        }

        // breaks inside on pos >= len or non-digit character
        while (pos < len) {
            c = input.charAt(pos++);
            if (c < '0' || c > '9') {
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') // break on trailing white space or exponent
                    break;
                else
                    return 0;
            }
            result = (result * 10) + (c - '0');
        }

        return sign * result;
    }

    /**
     * <p>
     * Quick long string parser that can handle negative and positive values.
     * </p>
     * <p>
     * Parser supports leading/trailing whitespace.
     * </p>
     *
     * @param input String to parse
     * @return Parsed long or 0 if the parsing fails
     * @throws NumberFormatException If the given input can not be parsed
     */
    public static long parseLongException(String input) throws NumberFormatException {
        long result = 0;
        int pos = 0;
        int len = input.length();
        if (len == 0)
            return 0;
        char c = input.charAt(0);
        long sign = 1;

        // skip any starting white space
        while (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') {
            ++pos;
            if (pos >= len)
                throw new NumberFormatException("Invalid input");
            c = input.charAt(pos);
        }

        // handle both - and + signs
        if (c == '-') {
            sign = -1;
            ++pos;
            if (pos >= len)
                throw new NumberFormatException("Invalid input");
        } else if (c == '+') {
            ++pos;
            if (pos >= len)
                throw new NumberFormatException("Invalid input");
        }

        // breaks inside on pos >= len or non-digit character
        while (pos < len) {
            c = input.charAt(pos++);
            if (c < '0' || c > '9') {
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') // break on trailing white space or exponent
                    break;
                else
                    throw new NumberFormatException("Invalid input");
            }
            result = (result * 10) + (c - '0');
        }

        return sign * result;
    }

    /**
     * <p>
     * Quick double string parser that can handle negative and positive doubles
     * but not exponents.
     * </p>
     * <p>
     * Parser supports NaN, +/-Inf, exponents and leading/trailing whitespace.
     * </p>
     * <p>
     * Parser is mostly locale unaware except for dot and comma for decimal
     * separator.
     *
     * @param input String to parse
     * @return Parsed double or Double.NaN if the parsing fails See also
     * {@link Parser#parseDoubleException(String)} If exceptions on
     * invalid input is necessary
     */
    public static double parseDouble(String input) {
        double result = 0;
        int pos = 0;
        int len = input.length();
        if (len == 0)
            return Double.NaN;
        char c = input.charAt(0);
        double sign = 1;

        // skip any starting white space
        while (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') {
            ++pos;
            if (pos >= len)
                return Double.NaN;
            c = input.charAt(pos);
        }

        // if input looks like N, it probably is
        if (c == 'N' || c == 'n') {
            return Double.NaN;
        }

        // handle both - and + signs
        if (c == '-') {
            sign = -1;
            ++pos;
            if (pos >= len)
                return Double.NaN;
        } else if (c == '+') {
            ++pos;
            if (pos >= len)
                return Double.NaN;
        }

        c = input.charAt(pos);
        if (c == 'I' || c == 'i') // probably inf, let's confirm
        {
            ++pos;
            if (pos >= len)
                return Double.NaN;
            c = input.charAt(pos);
            if (!(c == 'N' || c == 'n'))
                return Double.NaN;
            ++pos;
            if (pos >= len)
                return Double.NaN;
            c = input.charAt(pos);
            if (!(c == 'F' || c == 'f'))
                return Double.NaN;
            else {
                if (sign == -1)
                    return Double.NEGATIVE_INFINITY;
                else
                    return Double.POSITIVE_INFINITY;
            }
        }

        while (true) // breaks inside on pos >= len or non-digit character
        {
            if (pos >= len)
                return sign * result;
            c = input.charAt(pos++);
            if (c < '0' || c > '9')
                break;
            result = (result * 10.0) + (c - '0');
        }

        if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') // break on trailing white space
            return sign * result;

        if (c != '.' && c != ',')
            return Double.NaN;
        double exp = 0.1;
        while (pos < len) {
            c = input.charAt(pos++);
            if (c < '0' || c > '9') {
                if (c == 'e' || c == 'E' || c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') // break on trailing white space or exponent
                    break;
                else
                    return Double.NaN;
            }
            result += (c - '0') * exp;
            exp *= 0.1;
        }
        if (!(c == 'e' || c == 'E') || pos == len)
            return sign * result;

        int expSign = 1;
        c = input.charAt(pos);

        // handle both - and + signs
        if (c == '-') {
            expSign = -1;
            ++pos;
            if (pos >= len)
                return sign * result;
        } else if (c == '+') {
            ++pos;
            if (pos >= len)
                return sign * result;
        }

        double expResult = 0;

        while (true) // breaks inside on pos >= len or non-digit character
        {
            if (pos >= len)
                return sign * result * FastMath.pow(10.0, expSign * expResult);
            c = input.charAt(pos++);
            if (c < '0' || c > '9')
                break;
            expResult = (expResult * 10.0) + (c - '0');
        }

        if (c != '.' && c != ',')
            return sign * result * FastMath.pow(10.0, expSign * expResult);

        double expExp = 0.1;
        while (pos < len) {
            c = input.charAt(pos++);
            if (c < '0' || c > '9') {
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') // break on trailing white space or exponent
                    break;
                else
                    return Double.NaN;
            }
            expResult += (c - '0') * expExp;
            expExp *= 0.1;
        }

        return sign * result * FastMath.pow(10.0, expSign * expResult);
    }

    /**
     * <p>
     * Quick double string parser that can handle negative and positive doubles
     * but not exponents.
     * </p>
     * <p>
     * Parser supports NaN, +/-Inf and leading/trailing whitespace.
     * </p>
     * <p>
     * Parser is mostly locale unaware except for dot and comma for decimal
     * separator.
     *
     * @param input String to parse
     * @return Parsed double or Double.NaN if the parsing fails
     * @throws NumberFormatException If the input string is invalid
     */
    public static double parseDoubleException(String input) throws NumberFormatException {
        double result = 0;
        int pos = 0;
        int len = input.length();
        if (len == 0)
            throw new NumberFormatException("Invalid input");
        char c = input.charAt(0);
        double sign = 1;

        // skip any starting white space
        while (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') {
            ++pos;
            if (pos >= len)
                throw new NumberFormatException("Invalid input");
            c = input.charAt(pos);
        }

        // if input looks like N, it probably is
        if (c == 'N' || c == 'n') {
            return Double.NaN;
        }

        // handle both - and + signs
        if (c == '-') {
            sign = -1;
            ++pos;
            if (pos >= len)
                throw new NumberFormatException("Invalid input");
        } else if (c == '+') {
            ++pos;
            if (pos >= len)
                throw new NumberFormatException("Invalid input");
        }

        c = input.charAt(pos);
        if (c == 'I' || c == 'i') // probably inf, let's confirm
        {
            ++pos;
            if (pos >= len)
                throw new NumberFormatException("Invalid input");
            c = input.charAt(pos);
            if (!(c == 'N' || c == 'n'))
                throw new NumberFormatException("Invalid input");
            ++pos;
            if (pos >= len)
                throw new NumberFormatException("Invalid input");
            c = input.charAt(pos);
            if (!(c == 'F' || c == 'f'))
                throw new NumberFormatException("Invalid input");
            else {
                if (sign == -1)
                    return Double.NEGATIVE_INFINITY;
                else
                    return Double.POSITIVE_INFINITY;
            }
        }

        while (true) // breaks inside on pos >= len or non-digit character
        {
            if (pos >= len)
                return sign * result;
            c = input.charAt(pos++);
            if (c < '0' || c > '9')
                break;
            result = (result * 10.0) + (c - '0');
        }

        if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') // break on trailing white space
            return sign * result;

        if (c != '.' && c != ',')
            throw new NumberFormatException("Invalid input");
        double exp = 0.1;
        while (pos < len) {
            c = input.charAt(pos++);
            if (c < '0' || c > '9') {
                if (c == 'e' || c == 'E' || c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') // break on trailing white space or exponent
                    break;
                else
                    throw new NumberFormatException("Invalid input");
            }
            result += (c - '0') * exp;
            exp *= 0.1;
        }
        if (!(c == 'e' || c == 'E') || pos == len)
            return sign * result;

        int expSign = 1;
        c = input.charAt(pos);

        // handle both - and + signs
        if (c == '-') {
            expSign = -1;
            ++pos;
            if (pos >= len)
                return sign * result;
        } else if (c == '+') {
            ++pos;
            if (pos >= len)
                return sign * result;
        }

        double expResult = 0;

        while (true) // breaks inside on pos >= len or non-digit character
        {
            if (pos >= len)
                return sign * result * FastMath.pow(10.0, expSign * expResult);
            c = input.charAt(pos++);
            if (c < '0' || c > '9')
                break;
            expResult = (expResult * 10.0) + (c - '0');
        }

        if (c != '.' && c != ',')
            return sign * result * FastMath.pow(10.0, expSign * expResult);

        double expExp = 0.1;
        while (pos < len) {
            c = input.charAt(pos++);
            if (c < '0' || c > '9') {
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\0') // break on trailing white space or exponent
                    break;
                else
                    throw new NumberFormatException("Invalid input");
            }
            expResult += (c - '0') * expExp;
            expExp *= 0.1;
        }

        return sign * result * FastMath.pow(10.0, expSign * expResult);
    }

    /**
     * Method used to parse accepted boolean values in incoming messages
     *
     * @param what What to parse
     * @return True or false (also default return) depending on the value of
     * what
     */
    public static boolean parseBoolean(Object what) {
        if (what instanceof String) {
            String text = ((String) what).toLowerCase(Locale.ENGLISH);
            return (text.startsWith("t") || text.equals("1") || text.equals("on") || text.equals("yes"));
        } else if (what instanceof Boolean)
            return (Boolean) what;
        else
            return false;
    }

    /**
     * Parses an integer. If the input is not a valid integer representation it
     * returns 0.
     *
     * @param str The input string.
     * @return The integer representation of the string.
     */
    public static int parseInt(String str) {
        int ival = 0, idx = 0, end;
        boolean sign = false;
        char ch;

        if (str == null || (end = str.length()) == 0 || ((ch = str.charAt(0)) < '0' || ch > '9') && (!(sign = ch == '-') || ++idx == end || ((ch = str.charAt(idx)) < '0' || ch > '9')))
            return 0;

        for (; ; ival *= 10) {
            ival += '0' - ch;
            if (++idx == end)
                return sign ? ival : -ival;
            if ((ch = str.charAt(idx)) < '0' || ch > '9')
                return 0;
        }
    }

    /**
     * Parses an integer. Throws a {@link NumberFormatException} if the input is
     * not a valid integer representation.
     *
     * @param str The input string.
     * @return The integer representation of the string.
     */
    public static int parseIntException(String str) throws NumberFormatException {
        int ival = 0, idx = 0, end;
        boolean sign = false;
        char ch;

        if (str == null || (end = str.length()) == 0 || ((ch = str.charAt(0)) < '0' || ch > '9') && (!(sign = ch == '-') || ++idx == end || ((ch = str.charAt(idx)) < '0' || ch > '9')))
            throw new NumberFormatException(str);

        for (; ; ival *= 10) {
            ival += '0' - ch;
            if (++idx == end)
                return sign ? ival : -ival;
            if ((ch = str.charAt(idx)) < '0' || ch > '9')
                throw new NumberFormatException(str);
        }
    }

    /**
     * Parses a string to an integer, and returns the default value if the string does not represent an integer.
     * @param str The string.
     * @param defaultValue The default value.
     * @return The integer representation of the string, or the default value.
     */
    public static int parseIntOrElse(String str, int defaultValue) {
        try {
            return parseIntException(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Convenience method which uses the double parser and casts the result. It
     * will not throw Please check {@link Parser#parseDouble(String)}.
     *
     * @param input The input string.
     * @return The parsed float, or 0 if the parsing failed.
     */
    public static float parseFloat(String input) {
        return (float) parseDouble(input);
    }

    /**
     * Convenience method which uses the double parser and casts the result.
     * Please check {@link Parser#parseDoubleException(String)}
     *
     * @param input The input string.
     * @return The parsed float, or {@link Float#NaN} if the parsing failed.
     * @throws NumberFormatException if the parsing failed.
     */
    public static float parseFloatException(String input) throws NumberFormatException {
        return (float) parseDoubleException(input);
    }

    /**
     * Parses a float array in the form '[a, b, c, ...]
     *
     * @param input The input string.
     * @return The parsed float array, or null if the parsing failed.
     */
    public static float[] parseFloatArray(String input) {
        if (input == null) {
            return null;
        }
        input = input.trim();
        String[] tokens = input.substring(1, input.length() - 1).split(",");
        List<Float> list = Arrays.stream(tokens).map(Parser::parseFloat).collect(Collectors.toList());
        float[] result = new float[list.size()];
        int i = 0;
        for (Float f : list) {
            result[i] = f;
            i++;
        }
        return result;
    }

    /**
     * Parses a float array in the form '[a, b, c, ...]
     *
     * @param input The input string.
     * @return The parsed float array, or null if the parsing failed.
     */
    public static float[] parseFloatArrayException(String input) throws NumberFormatException {
        if (input == null) {
            return null;
        }
        input = input.trim();
        String[] tokens = input.substring(1, input.length() - 1).split(",");
        List<Float> list = Arrays.stream(tokens).map(Parser::parseFloatException).collect(Collectors.toList());
        float[] result = new float[list.size()];
        int i = 0;
        for (Float f : list) {
            result[i] = f;
            i++;
        }
        return result;
    }

    /**
     * Parses a double precision float array in the form '[a, b, c, ...]
     *
     * @param input The input string.
     * @return The parsed double precision float array, or null if the parsing failed.
     */
    public static double[] parseDoubleArray(String input) {
        if (input == null) {
            return null;
        }
        input = input.trim();
        String[] tokens = input.substring(1, input.length() - 1).split(",");
        List<Double> list = Arrays.stream(tokens).map(Parser::parseDouble).collect(Collectors.toList());
        double[] result = new double[list.size()];
        int i = 0;
        for (Double f : list) {
            result[i] = f;
            i++;
        }
        return result;
    }

    /**
     * Parses a double precision float array in the form '[a, b, c, ...]
     *
     * @param input The input string.
     * @return The parsed double precision float array, or null if the parsing failed.
     */
    public static double[] parseDoubleArrayException(String input) throws NumberFormatException {
        if (input == null) {
            return null;
        }
        input = input.trim();
        String[] tokens = input.substring(1, input.length() - 1).split(",");
        List<Double> list = Arrays.stream(tokens).map(Parser::parseDoubleException).collect(Collectors.toList());
        double[] result = new double[list.size()];
        int i = 0;
        for (Double f : list) {
            result[i] = f;
            i++;
        }
        return result;
    }
}
