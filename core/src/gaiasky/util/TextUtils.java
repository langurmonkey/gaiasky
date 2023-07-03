/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import gaiasky.GaiaSky;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utilities to manipulate strings and text.
 */
public class TextUtils {

    /**
     * Decimal format
     **/
    private static final DecimalFormat nf;
    private static final DecimalFormat nfSci;

    static {
        nf = new DecimalFormat("#########.###");
        nfSci = new DecimalFormat("0.#E0");
    }

    /**
     * Escape a give String to make it safe to be printed or stored.
     *
     * @param s The input String.
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
     * Breaks the character sequence with new line characters '\n' so that the lines have
     * approximately <code>breakChars</code> characters.
     *
     * @param in         The character sequence.
     * @param breakChars The number of characters per line.
     * @return The string, broken into lines.
     */
    public static String breakCharacters(CharSequence in,
                                         int breakChars) {
        return breakCharacters(in.toString(), breakChars);
    }

    /**
     * Breaks the string with new line characters '\n' so that the lines have
     * approximately <code>breakChars</code> characters.
     *
     * @param in         The string.
     * @param breakChars The number of characters per line.
     * @return The string, broken into lines.
     */
    public static String breakCharacters(String in,
                                         int breakChars) {
        return breakCharacters(in, breakChars, false);
    }

    /**
     * Breaks the string with new line characters '\n' so that the lines have
     * approximately <code>breakChars</code> characters.
     *
     * @param in         The string.
     * @param breakChars The number of characters per line.
     * @param forceBreak Break the string even when there are not separator characters.
     * @return The string, broken into lines.
     */
    public static String breakCharacters(String in,
                                         int breakChars,
                                         boolean forceBreak) {
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
                    if (chars > breakChars && (forceBreak || Character.isSpaceChar(c))) {
                        sb.setCharAt(i, '\n');
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

    public static String breakSpaces(CharSequence in,
                                     int breakSpaces) {
        return breakSpaces(in.toString(), breakSpaces);
    }

    public static String breakSpaces(String in,
                                     int breakSpaces) {
        // Warp text if breakSpaces <= 0
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

    public static void capLabelWidth(Label l,
                                     float targetWidth) {
        while (l.getWidth() > targetWidth) {
            StringBuilder currText = l.getText();
            currText.deleteCharAt(currText.length);
            l.setText(currText);
            l.pack();
        }
        l.setText(l.getText() + "...");
    }

    public static CharSequence limitWidth(CharSequence text,
                                          float width,
                                          float letterWidth) {
        int lettersPerLine = (int) (width / letterWidth);
        StringBuilder out = new StringBuilder();
        int currentLine = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' && Math.abs(currentLine - lettersPerLine) <= 5) {
                c = '\n';
                currentLine = 0;
            } else if (c == '\n') {
                currentLine = 0;
            } else {
                currentLine++;
            }
            out.append(c);
        }

        return out;
    }

    public static String capString(String in,
                                   int targetLength) {
        return capString(in, targetLength, false);
    }

    public static String capString(String in,
                                   int targetLength,
                                   boolean fromStart) {
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
     * @return The string with its first letter capitalised and the others in
     * lower case
     */
    public static String trueCapitalise(String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1).toLowerCase();
    }

    /**
     * Concatenates the strings using the given split
     *
     * @param split The split
     * @param strs  The strings
     * @return The concatenation
     */
    public static String concatenate(String split,
                                     String... strs) {
        if (strs == null || strs.length == 0)
            return null;
        java.lang.StringBuilder out = new java.lang.StringBuilder();
        for (String str : strs) {
            if (str != null && !str.isEmpty()) {
                if (out.length() > 0)
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
     * @return The concatenation
     */
    public static String concatenate(final String split,
                                     final Array<String> strings) {
        java.lang.StringBuilder out = new java.lang.StringBuilder();
        for (String str : strings) {
            if (str != null && !str.isEmpty()) {
                if (out.length() > 0)
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
     * @return The concatenation
     */
    public static String concatenate(final String split,
                                     final java.util.List<String> strings) {
        java.lang.StringBuilder out = new java.lang.StringBuilder();
        for (String str : strings) {
            if (str != null && !str.isEmpty()) {
                if (out.length() > 0)
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
            warp = Math.round(warp);
        } else {
            // Round to 2 decimal places
            warp = Math.round(warp * 1000.0) / 1000.0;
        }
        if (warp > 99999 || warp < -99999) {
            return "x" + nfSci.format(warp);
        } else {
            return "x" + nf.format(warp);
        }
    }

    public static String getFormattedTimeWarp() {
        return TextUtils.getFormattedTimeWarp(GaiaSky.instance.time.getWarpFactor());
    }

    /**
     * Concatenates the base with each of the strings in suffixes
     *
     * @param base     The base string
     * @param suffixes All the suffixes
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

    public static String[] concatAll(String base,
                                     String[] suffixes,
                                     String suffixAdditional) {
        String[] suffixesNew = new String[suffixes.length + 1];
        for (int i = 0; i < suffixes.length; i++)
            suffixesNew[i] = suffixes[1];
        suffixesNew[suffixes.length] = suffixAdditional;
        return concatAll(base, suffixesNew);
    }

    public static boolean contains(String[] list,
                                   String key) {
        return contains(list, key, false);
    }

    public static boolean contains(String[] list,
                                   String key,
                                   boolean ignoreCase) {
        AtomicBoolean contained = new AtomicBoolean(false);
        Arrays.stream(list).forEach(candidate -> {
            if (ignoreCase ? candidate.equalsIgnoreCase(key) : candidate.equals(key)) {
                contained.set(true);
            }
        });
        return contained.get();
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

    public static String html2text(String html) {
        return html.replaceAll("<.*?>", "");
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

}
