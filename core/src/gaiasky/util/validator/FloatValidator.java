/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;

public class FloatValidator extends NumberValidator<Float> {


    public FloatValidator(float min, float max) {
        super(null, min, max);
    }

    public FloatValidator(IValidator parent, float min, float max) {
        super(parent, min, max);
    }

    @Override
    protected boolean validateLocal(String value) {
        float val;
        try {
            val = Parser.parseFloat(value);
        } catch (NumberFormatException e) {
            return false;
        }

        return val >= min && val <= max;
    }

    @Override
    public String getMinString() {
        return min == Float.MIN_VALUE ? I18n.msg("gui.infinity.minus") : Float.toString(min);
    }

    @Override
    public String getMaxString() {
        return max == Float.MAX_VALUE ? I18n.msg("gui.infinity") : Float.toString(max);
    }

}
