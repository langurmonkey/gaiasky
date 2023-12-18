/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;

public class DoubleValidator extends NumberValidator<Double> {

    public DoubleValidator(double min,
                           double max) {
        super(null, min, max);
    }

    public DoubleValidator(IValidator parent,
                           double min,
                           double max) {
        super(parent, min, max);
    }

    @Override
    protected boolean validateLocal(String value) {
        double val;
        try {
            val = Parser.parseDouble(value);
        } catch (NumberFormatException e) {
            return false;
        }

        return val >= min && val <= max;
    }

    @Override
    public String getMinString() {
        return min == Double.MIN_VALUE ? I18n.msg("gui.infinity.minus") : Double.toString(min);
    }

    @Override
    public String getMaxString() {
        return max == Double.MAX_VALUE ? I18n.msg("gui.infinity") : Double.toString(max);
    }

}
