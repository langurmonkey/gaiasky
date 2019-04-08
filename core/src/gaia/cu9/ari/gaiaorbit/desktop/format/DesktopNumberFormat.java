/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.desktop.format;

import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;

import java.text.DecimalFormat;

public class DesktopNumberFormat implements INumberFormat {
    private final DecimalFormat df;

    public DesktopNumberFormat(String pattern) {
        df = new DecimalFormat(pattern);
    }

    @Override
    public String format(double num) {
        return df.format(num);
    }

    @Override
    public String format(long num) {
        return df.format(num);
    }

}
