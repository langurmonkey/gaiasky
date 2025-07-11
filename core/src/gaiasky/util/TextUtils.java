/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.utils.StringBuilder;
import gaiasky.GaiaSky;
import gaiasky.util.i18n.I18n;
import net.jafama.FastMath;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utilities to manipulate strings and text.
 */
public class TextUtils {

    // Regex pattern to match ANSI escape sequences
    private static final String ANSI_REGEX = "\\u001B\\[[;\\d]*m";

    /**
     * Removes ANSI color codes from the input string.
     *
     * @param input The string possibly containing ANSI color codes.
     *
     * @return A clean string without ANSI codes.
     */
    public static String stripAnsiCodes(String input) {
        if (input == null) return null;
        return input.replaceAll(ANSI_REGEX, "");
    }


    /**
     * Escape a give String to make it safe to be printed or stored.
     *
     * @param s The input String.
     *
     * @return The output String.
     **/
    public static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\f", "\\f")
                .replace("\"", "\\\"");
    }

    public static String surroundBrackets(String in) {
        return surround(in, "[", "]");
    }

    public static String surround(String in,
                                  String pre,
                                  String post) {
        return pre + in + post;
    }

    /**
     * Breaks the string with new line ('\n') characters so that the lines have
     * approximately <code>breakChars</code> characters.
     *
     * @param in         The character sequence.
     * @param breakChars The number of characters per line.
     *
     * @return The string, broken into lines.
     */
    public static String breakCharacters(CharSequence in,
                                         int breakChars) {
        return breakCharacters(in.toString(), breakChars);
    }

    /**
     * Breaks the string with new line ('\n') characters so that the lines have
     * approximately <code>breakChars</code> characters.
     *
     * @param in         The string.
     * @param breakChars The number of characters per line.
     *
     * @return The string, broken into lines.
     */
    public static String breakCharacters(String in,
                                         int breakChars) {
        return breakCharacters(in, breakChars, false);
    }

    /**
     * Breaks the string with new line ('\n') characters so that the lines have
     * approximately <code>breakChars</code> characters.
     *
     * @param in         The string.
     * @param breakChars The number of characters per line.
     * @param forceBreak Break the string even when there are no separator characters.
     *
     * @return The string, broken into lines.
     */
    public static String breakCharacters(String in,
                                         int breakChars,
                                         boolean forceBreak) {
        if (in == null) {
            return null;
        }
        // Warp text if breakChars <= 0
        if (breakChars > 0) {
            java.lang.StringBuilder sb = new java.lang.StringBuilder(in);
            int chars = 0;
            for (int i = 0; i < sb.length(); i++) {
                char c = sb.charAt(i);
                if (c == '\n' || c == '\r') {
                    chars = 0;
                } else {
                    chars++;
                    if (chars > breakChars && (forceBreak || Character.isSpaceChar(c) || c == '/' || c == '\\')) {
                        if (Character.isSpaceChar(c)) {
                            // Replace space with new line.
                            sb.setCharAt(i, '\n');
                        } else {
                            // Add new line after.
                            sb.insert(i + 1, '\n');
                        }
                        chars = 0;
                    }
                }
            }
            in = sb.toString();
        }
        return in;
    }

    public static long countLines(String str) {
        return str.chars().filter(ch -> ch == '\n' || ch == '\r').count();
    }

    public static String breakSpaces(String in,
                                     int breakSpaces) {
        // Warp text if breakSpaces > 0
        if (breakSpaces > 0) {
            java.lang.StringBuilder sb = new java.lang.StringBuilder(in);
            int spaces = 0;
            for (int i = 0; i < sb.length(); i++) {
                char c = sb.charAt(i);
                if (Character.isSpaceChar(c)) {
                    spaces++;
                }
                if (spaces == breakSpaces) {
                    sb.setCharAt(i, '\n');
                    spaces = 0;
                }
            }
            in = sb.toString();
        }
        return in;
    }


    public static String capString(String in,
                                   int targetLength) {
        return capString(in, targetLength, false);
    }

    public static String capString(String in,
                                   int targetLength,
                                   boolean fromStart) {
        if (in == null)
            return null;
        if (in.length() <= targetLength) {
            return in;
        } else {
            if (fromStart) {
                return "..." + in.substring(in.length() - (targetLength - 3));
            } else {
                return in.substring(0, targetLength - 1) + "...";
            }
        }
    }

    /**
     * Converts from property displayName to method displayName by removing the
     * separator dots and capitalising each chunk. Example: model.texture.bump
     * -> ModelTextureBump
     *
     * @param property The property displayName
     *
     * @return The method name
     */
    public static String propertyToMethodName(String property) {
        String[] parts = property.split("\\.");
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            b.append(capitalise(part));
        }
        return b.toString();
    }

    /**
     * Returns the given string with the first letter capitalised
     *
     * @param line The input string
     *
     * @return The string with its first letter capitalised
     */
    public static String capitalise(String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

    /**
     * Returns the given string with the first letter capitalised and all the
     * others in lower case
     *
     * @param line The input string
     *
     * @return The string with its first letter capitalised and the others in
     * lower case
     */
    public static String trueCapitalise(String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1).toLowerCase();
    }

    /**
     * Concatenates the strings using the given split
     *
     * @param split   The split
     * @param strings The strings
     *
     * @return The concatenation
     */
    public static String concatenate(String split,
                                     String... strings) {
        if (strings == null || strings.length == 0)
            return null;
        java.lang.StringBuilder out = new java.lang.StringBuilder();
        for (String str : strings) {
            if (str != null && !str.isEmpty()) {
                if (!out.isEmpty())
                    out.append(split);
                out.append(str);
            }
        }
        return out.toString();
    }

    /**
     * Concatenates the strings using the given split
     *
     * @param split   The split
     * @param strings The strings
     *
     * @return The concatenation
     */
    public static String concatenate(final String split,
                                     final java.util.List<String> strings) {
        java.lang.StringBuilder out = new java.lang.StringBuilder();
        for (String str : strings) {
            if (str != null && !str.isEmpty()) {
                if (!out.isEmpty())
                    out.append(split);
                out.append(str);
            }
        }
        return out.toString();
    }

    public static String arrayToStr(String[] arr) {
        java.lang.StringBuilder buff = new java.lang.StringBuilder();
        for (String s : arr) {
            buff.append(s).append('\n');
        }
        return buff.toString();
    }

    public static String arrayToStr(String[] arr,
                                    String pre,
                                    String post,
                                    String sep) {
        java.lang.StringBuilder buff = new java.lang.StringBuilder(pre);
        for (int i = 0; i < arr.length; i++) {
            buff.append(arr[i]);
            if (i < arr.length - 1) {
                buff.append(sep);
            }
        }
        return buff + post;
    }

    public static String setToStr(Set<String> set) {
        return setToStr(set, "[", "]", ", ");
    }

    public static String setToStr(Set<String> set,
                                  String pre,
                                  String post,
                                  String sep) {
        java.lang.StringBuilder buff = new java.lang.StringBuilder(pre);
        if (set != null) {
            int n = set.size();
            int i = 0;
            for (String elem : set) {
                buff.append(elem);
                if (i < n - 1) {
                    buff.append(sep);
                }
                i++;
            }
        }
        return buff + post;
    }

    public static String getFormattedTimeWarp(double warp) {
        if (warp > 0.9 || warp < -0.9) {
            // Remove decimals
            warp = FastMath.round(warp);
        } else {
            // Round to 2 decimal places
            warp = FastMath.round(warp * 1000.0) / 1000.0;
        }
        return "x" + GlobalResources.formatNumber(warp);
    }

    public static String getFormattedTimeWarp() {
        return TextUtils.getFormattedTimeWarp(GaiaSky.instance.time.getWarpFactor());
    }

    public static String secondsToTimeUnit(double seconds) {
        var nf = GlobalResources.nf;
        var nfSci = GlobalResources.nfSci;
        if (seconds >= 0) {
            if (seconds < 60) {
                // Seconds.
                return nf.format(seconds) + " " + I18n.msg("gui.unit.second");
            } else if (seconds < 3600) {
                // Minutes.
                return nf.format(seconds / 60.0) + " " + I18n.msg("gui.unit.minute");
            } else if (seconds < 86400) {
                // Hours.
                return nf.format(seconds / 3600.0) + " " + I18n.msg("gui.unit.hour");
            } else if (seconds < 604800) {
                // Days.
                return nf.format(seconds / 86400.0) + " " + I18n.msg("gui.unit.day");
            } else if (seconds < 2629800) {
                // Weeks.
                return nf.format(seconds / 604800.0) + " " + I18n.msg("gui.unit.week");
            } else if (seconds < 31557600) {
                // Months.
                return nf.format(seconds / 2629800.0) + " " + I18n.msg("gui.unit.month");
            } else {
                // Years.
                return nfSci.format(seconds / 31557600.0) + " " + I18n.msg("gui.unit.year");
            }
        } else {
            if (seconds > -60) {
                // Seconds.
                return nf.format(seconds) + " " + I18n.msg("gui.unit.second");
            } else if (seconds > -3600) {
                // Minutes.
                return nf.format(seconds / 60.0) + " " + I18n.msg("gui.unit.minute");
            } else if (seconds > -86400) {
                // Hours.
                return nf.format(seconds / 3600.0) + " " + I18n.msg("gui.unit.hour");
            } else if (seconds > -604800) {
                // Days.
                return nf.format(seconds / 86400.0) + " " + I18n.msg("gui.unit.day");
            } else if (seconds > -2629800) {
                // Weeks.
                return nf.format(seconds / 604800.0) + " " + I18n.msg("gui.unit.week");
            } else if (seconds > -31557600) {
                // Months.
                return nf.format(seconds / 2629800.0) + " " + I18n.msg("gui.unit.month");
            } else {
                // Years.
                return nfSci.format(seconds / 31557600.0) + " " + I18n.msg("gui.unit.year");
            }

        }
    }

    /**
     * Concatenates the base with each of the strings in suffixes
     *
     * @param base     The base string
     * @param suffixes All the suffixes
     *
     * @return The result
     */
    public static String[] concatAll(String base,
                                     String[] suffixes) {
        String[] result = new String[suffixes.length];
        for (int i = 0; i < suffixes.length; i++) {
            result[i] = base + suffixes[i];
        }
        return result;
    }

    /**
     * Returns a new array with the given element inserted at the beginning (index 0).
     *
     * @param elements The array.
     * @param element  The new element to insert.
     * @param <T>      The type of objects contained in the arrays.
     *
     * @return The new array.
     */
    public static <T> T[] addToBeginningOfArray(T[] elements, T element) {
        T[] newArray = Arrays.copyOf(elements, elements.length + 1);
        newArray[0] = element;
        System.arraycopy(elements, 0, newArray, 1, elements.length);
        return newArray;
    }

    public static boolean contains(String[] list,
                                   String key) {
        return contains(list, key, false);
    }

    public static boolean contains(String[] list,
                                   String key,
                                   boolean ignoreCase) {
        AtomicBoolean result = new AtomicBoolean(false);
        Arrays.stream(list).forEach(candidate -> {
            if (ignoreCase ? candidate.equalsIgnoreCase(key) : candidate.equals(key)) {
                result.set(true);
            }
        });
        return result.get();
    }

    public static boolean startsWith(String[] list,
                                     String prefix) {
        AtomicBoolean result = new AtomicBoolean(false);
        Arrays.stream(list).forEach(candidate -> {
            if (candidate.startsWith(prefix)) {
                result.set(true);
            }
        });
        return result.get();
    }

    public static String ensureStartsWith(String base,
                                          String start) {
        if (!base.startsWith(start))
            return start + base;
        else
            return base;
    }

    public static boolean contains(String name,
                                   List<String> list) {
        for (String candidate : list)
            if (candidate != null && !candidate.isEmpty() && name.contains(candidate))
                return true;
        return false;
    }

    public static boolean containsOrMatches(String[] list,
                                            String key,
                                            boolean ignoreCase) {
        AtomicBoolean contained = new AtomicBoolean(false);
        Arrays.stream(list).forEach(candidate -> {
            if (ignoreCase ? candidate.equalsIgnoreCase(key) : candidate.equals(key)) {
                contained.set(true);
            } else if (key.matches(candidate)) {
                contained.set(true);
            }
        });
        return contained.get();
    }

    public static boolean containsOrMatches(String[][] list,
                                            String key,
                                            boolean ignoreCase) {
        AtomicBoolean contained = new AtomicBoolean(false);
        for (String[] l : list) {
            Arrays.stream(l).forEach(candidate -> {
                if (ignoreCase ? candidate.equalsIgnoreCase(key) : candidate.equals(key)) {
                    contained.set(true);
                } else if (key.matches(candidate)) {
                    contained.set(true);
                }
            });
        }
        return contained.get();
    }

    /**
     * Removes the tags and unescapes the given HTML code into a plain string.
     *
     * @param html The HTML code.
     *
     * @return Unescaped plain text without HTML tags or codes.
     */
    public static String html2text(String html) {
        return HTML4Unescape.unescapeHTML4(html.replaceAll("<.*?>", ""));
    }

    /*
     *
     * unescapePerlString()
     *
     *      Tom Christiansen <tchrist@perl.com>
     *      Sun Nov 28 12:55:24 MST 2010
     *
     * It's completely ridiculous that there's no standard
     * unescape_java_string function.  Since I have to do the
     * damn thing myself, I might as well make it halfway useful
     * by supporting things Java was too stupid to consider in
     * strings:
     *
     *   => "?" items  are additions to Java string escapes
     *                 but normal in Java regexes
     *
     *   => "!" items  are also additions to Java regex escapes
     *
     * Standard singletons: ?\a ?\e \f \n \r \t
     *
     *      NB: \b is unsupported as backspace so it can pass-through
     *          to the regex translator untouched; I refuse to make anyone
     *          doublebackslash it as doublebackslashing is a Java idiocy
     *          I desperately wish would die out.  There are plenty of
     *          other ways to write it:
     *
     *              \cH, \12, \012, \x08 \x{8}, \u0008, \U00000008
     *
     * Octal escapes: \0 \0N \0NN \N \NN \NNN
     *    Can range up to !\777 not \377
     *
     * Control chars: ?\cX
     *      Means: ord(X) ^ ord('@')
     *
     * Old hex escapes: \xXX
     *      unbraced must be 2 xdigits
     *
     * Perl hex escapes: !\x{XXX} braced may be 1-8 xdigits
     *       NB: proper Unicode never needs more than 6, as highest
     *           valid codepoint is 0x10FFFF, not maxint 0xFFFFFFFF
     *
     * Lame Java escape: \[IDIOT JAVA PREPROCESSOR]uXXXX must be
     *                   exactly 4 xdigits;
     *
     *       I can't write XXXX in this comment where it belongs
     *       because the damned Java Preprocessor can't mind its
     *       own business.  Idiots!
     *
     * Lame Python escape: !\UXXXXXXXX must be exactly 8 xdigits
     *
     * TODO: Perl translation escapes: \Q \U \L \E \[IDIOT JAVA PREPROCESSOR]u \l
     *       These are not so important to cover if you're passing the
     *       result to Pattern.compile(), since it handles them for you
     *       further downstream.  Hm, what about \[IDIOT JAVA PREPROCESSOR]u?
     *
     */

    public static String unescape(String oldStr) {

        /*
         * In contrast to fixing Java's broken regex char classes,
         * this one need be no bigger, as un-escaping shrinks the string
         * here, where in the other one, it grows it.
         */

        java.lang.StringBuilder newStr = new java.lang.StringBuilder(oldStr.length());

        boolean saw_backslash = false;

        for (int i = 0; i < oldStr.length(); i++) {
            int cp = oldStr.codePointAt(i);
            if (oldStr.codePointAt(i) > Character.MAX_VALUE) {
                i++;
            }

            if (!saw_backslash) {
                if (cp == '\\') {
                    saw_backslash = true;
                } else {
                    newStr.append(Character.toChars(cp));
                }
                continue; /* switch */
            }

            if (cp == '\\') {
                saw_backslash = false;
                newStr.append('\\');
                newStr.append('\\');
                continue; /* switch */
            }

            switch (cp) {

                case 'r':
                    newStr.append('\r');
                    break; /* switch */

                case 'n':
                    newStr.append('\n');
                    break; /* switch */

                case 'f':
                    newStr.append('\f');
                    break; /* switch */

                /* PASS a \b THROUGH!! */
                case 'b':
                    newStr.append("\\b");
                    break; /* switch */

                case 't':
                    newStr.append('\t');
                    break; /* switch */

                case 'a':
                    newStr.append('\007');
                    break; /* switch */

                case 'e':
                    newStr.append('\033');
                    break; /* switch */

                /*
                 * A "control" character is what you get when you xor its
                 * codepoint with '@'==64.  This only makes sense for ASCII,
                 * and may not yield a "control" character after all.
                 *
                 * Strange but true: "\c{" is ";", "\c}" is "=", etc.
                 */
                case 'c': {
                    if (++i == oldStr.length()) {
                        die("trailing \\c");
                    }
                    cp = oldStr.codePointAt(i);
                    /*
                     * don't need to grok surrogates, as next line blows them up
                     */
                    if (cp > 0x7f) {
                        die("expected ASCII after \\c");
                    }
                    newStr.append(Character.toChars(cp ^ 64));
                    break; /* switch */
                }

                case '8':
                case '9':
                    die("illegal octal digit");
                    /* NOTREACHED */

                    /*
                     * may be 0 to 2 octal digits following this one
                     * so back up one for fallthrough to next case;
                     * unread this digit and fall through to next case.
                     */
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                    --i;
                    /* FALLTHROUGH */

                    /*
                     * Can have 0, 1, or 2 octal digits following a 0
                     * this permits larger values than octal 377, up to
                     * octal 777.
                     */
                case '0': {
                    if (i + 1 == oldStr.length()) {
                        /* found \0 at end of string */
                        newStr.append(Character.toChars(0));
                        break; /* switch */
                    }
                    i++;
                    int digits = 0;
                    int j;
                    for (j = 0; j <= 2; j++) {
                        if (i + j == oldStr.length()) {
                            break; /* for */
                        }
                        /* safe because will unread surrogate */
                        int ch = oldStr.charAt(i + j);
                        if (ch < '0' || ch > '7') {
                            break; /* for */
                        }
                        digits++;
                    }
                    if (digits == 0) {
                        --i;
                        newStr.append('\0');
                        break; /* switch */
                    }
                    int value = 0;
                    try {
                        value = Integer.parseInt(oldStr.substring(i, i + digits), 8);
                    } catch (NumberFormatException nfe) {
                        die("invalid octal value for \\0 escape");
                    }
                    newStr.append(Character.toChars(value));
                    i += digits - 1;
                    break; /* switch */
                } /* end case '0' */

                case 'x': {
                    if (i + 2 > oldStr.length()) {
                        die("string too short for \\x escape");
                    }
                    i++;
                    boolean saw_brace = false;
                    if (oldStr.charAt(i) == '{') {
                        /* ^^^^^^ ok to ignore surrogates here */
                        i++;
                        saw_brace = true;
                    }
                    int j;
                    for (j = 0; j < 8; j++) {

                        if (!saw_brace && j == 2) {
                            break;  /* for */
                        }

                        /*
                         * ASCII test also catches surrogates
                         */
                        int ch = oldStr.charAt(i + j);
                        if (ch > 127) {
                            die("illegal non-ASCII hex digit in \\x escape");
                        }

                        if (saw_brace && ch == '}') {
                            break; /* for */
                        }

                        if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))) {
                            die(String.format("illegal hex digit #%d '%c' in \\x", ch, ch));
                        }

                    }
                    if (j == 0) {
                        die("empty braces in \\x{} escape");
                    }
                    int value = 0;
                    try {
                        value = Integer.parseInt(oldStr.substring(i, i + j), 16);
                    } catch (NumberFormatException nfe) {
                        die("invalid hex value for \\x escape");
                    }
                    newStr.append(Character.toChars(value));
                    if (saw_brace) {
                        j++;
                    }
                    i += j - 1;
                    break; /* switch */
                }

                case 'u': {
                    if (i + 4 > oldStr.length()) {
                        die("string too short for \\u escape");
                    }
                    i++;
                    int j;
                    for (j = 0; j < 4; j++) {
                        /* this also handles the surrogate issue */
                        if (oldStr.charAt(i + j) > 127) {
                            die("illegal non-ASCII hex digit in \\u escape");
                        }
                    }
                    int value = 0;
                    try {
                        value = Integer.parseInt(oldStr.substring(i, i + j), 16);
                    } catch (NumberFormatException nfe) {
                        die("invalid hex value for \\u escape");
                    }
                    newStr.append(Character.toChars(value));
                    i += j - 1;
                    break; /* switch */
                }

                case 'U': {
                    if (i + 8 > oldStr.length()) {
                        die("string too short for \\U escape");
                    }
                    i++;
                    int j;
                    for (j = 0; j < 8; j++) {
                        /* this also handles the surrogate issue */
                        if (oldStr.charAt(i + j) > 127) {
                            die("illegal non-ASCII hex digit in \\U escape");
                        }
                    }
                    int value = 0;
                    try {
                        value = Integer.parseInt(oldStr.substring(i, i + j), 16);
                    } catch (NumberFormatException nfe) {
                        die("invalid hex value for \\U escape");
                    }
                    newStr.append(Character.toChars(value));
                    i += j - 1;
                    break; /* switch */
                }

                default:
                    newStr.append('\\');
                    newStr.append(Character.toChars(cp));
                    /*
                     * say(String.format(
                     *       "DEFAULT unrecognized escape %c passed through",
                     *       cp));
                     */
                    break; /* switch */

            }
            saw_backslash = false;
        }

        /* weird to leave one at the end */
        if (saw_backslash) {
            newStr.append('\\');
        }

        return newStr.toString();
    }

    private static void die(String foa) {
        throw new IllegalArgumentException(foa);
    }

    public static String classSimpleName(String className) {
        if (className.contains(".")) {
            return className.substring(className.lastIndexOf(".") + 1);
        } else {
            return className;
        }
    }

    /**
     * Pads the given string with the given character to be the given length.
     *
     * @param str     The string to pad.
     * @param length  The target length.
     * @param padChar The padding character to use.
     *
     * @return The padded string, or the original string if its length was greater than
     * the given target length.
     */
    public static String padString(String str,
                                   int length,
                                   char padChar) {
        if (str.length() >= length) {
            return str;
        } else {
            return String.format("%1$" + length + "s", str).replace(' ', padChar);
        }
    }

    public static int hashFast(String str) {
        return hashFast(str.toCharArray());
    }

    public static int hashFast(char[] val) {
        int h = 0, i = 0;
        int len = val.length;
        for (; i + 3 < len; i += 4) {
            h = 31 * 31 * 31 * 31 * h
                    + 31 * 31 * 31 * val[i]
                    + 31 * 31 * val[i + 1]
                    + 31 * val[i + 2]
                    + val[i + 3];
        }
        for (; i < len; i++) {
            h = 31 * h + val[i];
        }
        return h;
    }

    public static int hashFNV1(String str) {
        return hashFNV1(str.toCharArray(), 1);
    }

    public static int hashFNV1(char[] data,
                               long seed) {
        int len = data.length;
        for (char datum : data) {
            seed += (seed << 1) + (seed << 4) + (seed << 7) + (seed << 8) + (seed << 24);
            seed ^= datum;
        }
        return (int) seed;
    }

    public static int hashMurmur(String str) {
        return hashMurmur(str.toCharArray(), 1);
    }

    public static int hashMurmur(char[] data,
                                 int seed) {
        int m = 0x5bd1e995;
        int r = 24;

        int h = seed ^ data.length;

        int len = data.length;
        int len_4 = len >> 2;

        for (int i = 0; i < len_4; i++) {
            int i_4 = i << 2;
            int k = data[i_4 + 3];
            k = k << 8;
            k = k | (data[i_4 + 2] & 0xff);
            k = k << 8;
            k = k | (data[i_4 + 1] & 0xff);
            k = k << 8;
            k = k | (data[i_4] & 0xff);
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }

        int len_m = len_4 << 2;
        int left = len - len_m;

        if (left != 0) {
            if (left >= 3) {
                h ^= (int) data[len - 3] << 16;
            }
            if (left >= 2) {
                h ^= (int) data[len - 2] << 8;
            }
            if (left >= 1) {
                h ^= (int) data[len - 1];
            }

            h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

    /**
     * Reads the first line of a file.
     *
     * @param file The path pointing to the file to read.
     *
     * @return The first line as a string.
     */
    public static Optional<String> readFirstLine(Path file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line = reader.readLine();
            reader.close();
            return Optional.of(line);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Sanitizes the given input string so that it can be used as a file name.
     *
     * @param input The input string.
     *
     * @return A file system sanitized version of the string.
     */
    public static String sanitizeFilename(String input) {
        // 1. Replace illegal characters with underscores
        String sanitized = input.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 2. Remove control characters
        sanitized = sanitized.replaceAll("[\\p{Cntrl}]", "");

        // 3. Trim spaces and dots (important for Windows)
        sanitized = sanitized.trim();
        while (sanitized.endsWith(".") || sanitized.endsWith(" ")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }

        // 4. Windows reserved names check
        String[] reserved = {
                "CON", "PRN", "AUX", "NUL",
                "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
                "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        };

        for (String res : reserved) {
            if (sanitized.equalsIgnoreCase(res)) {
                sanitized = "_" + sanitized;
                break;
            }
        }

        // 5. Empty fallback
        if (sanitized.isEmpty()) {
            sanitized = "unnamed";
        }

        return sanitized;
    }


    /** HTML4 characters. */
    private static class HTML4Unescape {
        private static final HashMap<String, Integer> knownEscapes = new HashMap<>(1024);

        /**
         * Unescape a string containing entity escapes to a string containing the actual Unicode characters
         * corresponding to the escapes.
         * This is a replacement for the <code>unescapeHtml4</code> function in
         * <code>org.apache.commons.lang3.StringEscapeUtils</code>.
         *
         * @param str The string to unescape.
         *
         * @return A new unescaped String, <code>null</code> if null string input.
         */
        public static String unescapeHTML4(String str) {
            if (str == null)
                return null;
            int index = 0;
            Integer cCode;
            while ((index = str.indexOf('&', index)) != -1) {
                int index2 = str.indexOf(';', index + 1);
                if (index2 == -1)
                    return str;
                if (str.charAt(index + 1) == '#') {
                    int result = 0;
                    try {
                        if (str.charAt(index + 2) == 'x') {
                            for (int i = index + 3; i < index2; ++i)
                                result = (result << 4) | hexValue(str.charAt(i));
                        } else {
                            result = Integer.parseInt(str.substring(index + 2, index2));
                        }
                    } catch (NumberFormatException e) {
                        index = index2;
                        continue;
                    }
                    str = str.substring(0, index) + Character.toString((char) result) + str.substring(index2 + 1);
                    ++index;
                } else if ((cCode = knownEscapes.get(str.substring(index + 1, index2))) != null) {
                    str = str.substring(0, index) + Character.toString((char) ((int) cCode)) + str.substring(index2 + 1);
                    ++index;
                } else {
                    index = index2 + 1;
                }
            }
            return str;
        }

        private static int hexValue(char c) {
            if ((c >= '0') && (c <= '9'))
                return (int) (c - '0');
            if ((c >= 'A') && (c <= 'F'))
                return ((int) (c - 'A')) + 0x0A;
            throw new NumberFormatException();
        }

        static {
            knownEscapes.put("nbsp", 32);
            knownEscapes.put("lt", 60);
            knownEscapes.put("gt", 62);
            knownEscapes.put("quot", 34);
            knownEscapes.put("amp", 38);
            knownEscapes.put("AElig", 198);
            knownEscapes.put("Aacute", 193);
            knownEscapes.put("Acirc", 194);
            knownEscapes.put("Agrave", 192);
            knownEscapes.put("Aring", 197);
            knownEscapes.put("Atilde", 195);
            knownEscapes.put("Auml", 196);
            knownEscapes.put("Ccedil", 199);
            knownEscapes.put("ETH", 208);
            knownEscapes.put("Eacute", 201);
            knownEscapes.put("Ecirc", 202);
            knownEscapes.put("Egrave", 200);
            knownEscapes.put("Euml", 203);
            knownEscapes.put("Iacute", 205);
            knownEscapes.put("Icirc", 206);
            knownEscapes.put("Igrave", 204);
            knownEscapes.put("Iuml", 207);
            knownEscapes.put("Ntilde", 209);
            knownEscapes.put("Oacute", 211);
            knownEscapes.put("Ocirc", 212);
            knownEscapes.put("Ograve", 210);
            knownEscapes.put("Oslash", 216);
            knownEscapes.put("Otilde", 213);
            knownEscapes.put("Ouml", 214);
            knownEscapes.put("THORN", 222);
            knownEscapes.put("Uacute", 218);
            knownEscapes.put("Ucirc", 219);
            knownEscapes.put("Ugrave", 217);
            knownEscapes.put("Uuml", 220);
            knownEscapes.put("Yacute", 221);
            knownEscapes.put("aacute", 225);
            knownEscapes.put("acirc", 226);
            knownEscapes.put("aelig", 230);
            knownEscapes.put("agrave", 224);
            knownEscapes.put("aring", 229);
            knownEscapes.put("atilde", 227);
            knownEscapes.put("auml", 228);
            knownEscapes.put("ccedil", 231);
            knownEscapes.put("eacute", 233);
            knownEscapes.put("ecirc", 234);
            knownEscapes.put("egrave", 232);
            knownEscapes.put("eth", 240);
            knownEscapes.put("euml", 235);
            knownEscapes.put("iacute", 237);
            knownEscapes.put("icirc", 238);
            knownEscapes.put("igrave", 236);
            knownEscapes.put("iuml", 239);
            knownEscapes.put("ntilde", 241);
            knownEscapes.put("oacute", 243);
            knownEscapes.put("ocirc", 244);
            knownEscapes.put("ograve", 242);
            knownEscapes.put("oslash", 248);
            knownEscapes.put("otilde", 245);
            knownEscapes.put("ouml", 246);
            knownEscapes.put("szlig", 223);
            knownEscapes.put("thorn", 254);
            knownEscapes.put("uacute", 250);
            knownEscapes.put("ucirc", 251);
            knownEscapes.put("ugrave", 249);
            knownEscapes.put("uuml", 252);
            knownEscapes.put("yacute", 253);
            knownEscapes.put("yuml", 255);
            knownEscapes.put("cent", 162);
            knownEscapes.put("OElig", 0x0152);
            knownEscapes.put("oelig", 0x0153);
            knownEscapes.put("euro", 0x20AC);
        }
    }
}
