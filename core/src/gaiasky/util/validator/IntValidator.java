/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import gaiasky.util.i18n.I18n;

public class IntValidator extends NumberValidator<Integer> {

    public IntValidator() {
        this(null);
    }

    public IntValidator(IValidator parent) {
        super(parent, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public IntValidator(int min, int max) {
        super(null, min, max);
    }

    @Override
    protected boolean validateLocal(String value) {
        int val;
        try {
            val = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }

        return val >= min && val <= max;
    }

    @Override
    public String getMinString() {
        return min == Integer.MIN_VALUE ? I18n.msg("gui.infinity.minus") : Integer.toString(min);
    }

    @Override
    public String getMaxString() {
        return max == Integer.MAX_VALUE ? I18n.msg("gui.infinity") : Integer.toString(max);
    }

}
