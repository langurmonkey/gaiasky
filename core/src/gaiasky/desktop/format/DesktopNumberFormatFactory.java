/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.format;

import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;

public class DesktopNumberFormatFactory extends NumberFormatFactory {

    @Override
    protected INumberFormat getNumberFormatter(String pattern) {
        return new DesktopNumberFormat(pattern);
    }

}
