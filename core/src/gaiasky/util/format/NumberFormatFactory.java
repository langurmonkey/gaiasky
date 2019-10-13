/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.format;

public abstract class NumberFormatFactory {
    private static NumberFormatFactory instance;

    public static void initialize(NumberFormatFactory inst) {
        instance = inst;
    }

    public static INumberFormat getFormatter(String pattern) {
        return instance.getNumberFormatter(pattern);
    }

    protected abstract INumberFormat getNumberFormatter(String pattern);
}
