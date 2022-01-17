/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import gaiasky.GaiaSky;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contains some general utilities to deal with text and strings.
 */
public class TextUtils {

    public static String surroundBrackets(String in) {
        return surround(in, "[", "]");
    }

    public static String surround(String in, String pre, String post) {
        return pre + in + post;
    }

    public static String breakCharacters(CharSequence in, int breakChars) {
        return breakCharacters(in.toString(), breakChars);
    }

    public static String breakCharacters(String in, int breakChars) {
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
                    if (chars > breakChars && Character.isSpaceChar(c)) {
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

    public static String breakSpaces(CharSequence in, int breakSpaces) {
        return breakSpaces(in.toString(), breakSpaces);
    }

    public static String breakSpaces(String in, int breakSpaces) {
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

    public static void capLabelWidth(Label l, float targetWidth) {
        while (l.getWidth() > targetWidth) {
            StringBuilder currText = l.getText();
            currText.deleteCharAt(currText.length);
            l.setText(currText);
            l.pack();
        }
        l.setText(l.getText() + "...");
    }

    public static CharSequence limitWidth(CharSequence text, float width, float letterWidth) {
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

    public static String capString(String in, int targetLength) {
        return capString(in, targetLength, false);
    }

    public static String capString(String in, int targetLength, boolean fromStart) {
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
     * @param split The split
     * @param strs  The strings
     *
     * @return The concatenation
     */
    public static String concatenate(String split, String... strs) {
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
     *
     * @return The concatenation
     */
    public static String concatenate(final String split, final Array<String> strings) {
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
     *
     * @return The concatenation
     */
    public static String concatenate(final String split, final java.util.List<String> strings) {
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

    public static String arrayToStr(String[] arr, String pre, String post, String sep) {
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

    public static String setToStr(Set<String> set, String pre, String post, String sep) {
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

    /** Decimal format **/
    private static final INumberFormat nf;
    private static final INumberFormat nfsci;

    static {
        nf = NumberFormatFactory.getFormatter("#########.###");
        nfsci = NumberFormatFactory.getFormatter("0.#E0");
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
            return "x" + nfsci.format(warp);
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
     *
     * @return The result
     */
    public static String[] concatAll(String base, String[] suffixes) {
        String[] result = new String[suffixes.length];
        for (int i = 0; i < suffixes.length; i++) {
            result[i] = base + suffixes[i];
        }
        return result;
    }

    public static String[] concatAll(String base, String[] suffixes, String suffixAdditional) {
        String[] suffixesNew = new String[suffixes.length + 1];
        for (int i = 0; i < suffixes.length; i++)
            suffixesNew[i] = suffixes[1];
        suffixesNew[suffixes.length] = suffixAdditional;
        return concatAll(base, suffixesNew);
    }

    public static boolean contains(String[] list, String key) {
        return contains(list, key, false);
    }

    public static boolean contains(String[] list, String key, boolean ignoreCase) {
        AtomicBoolean contained = new AtomicBoolean(false);
        Arrays.stream(list).forEach(candidate -> {
            if (ignoreCase ? candidate.equalsIgnoreCase(key) : candidate.equals(key)) {
                contained.set(true);
            }
        });
        return contained.get();
    }

    public static boolean contains(String name, java.util.List<String> list) {
        for (String candidate : list)
            if (candidate != null && !candidate.isEmpty() && name.contains(candidate))
                return true;
        return false;
    }

    public static boolean containsOrMatches(String[] list, String key, boolean ignoreCase) {
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

    public static String html2text(String html) {
        return html.replaceAll("\\<.*?\\>", "");
    }
}
