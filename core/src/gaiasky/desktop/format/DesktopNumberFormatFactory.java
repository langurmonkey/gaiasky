/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.desktop.format;

import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;

public class DesktopNumberFormatFactory extends NumberFormatFactory {

    @Override
    protected INumberFormat getNumberFormatter(String pattern) {
        return new DesktopNumberFormat(pattern);
    }

}
