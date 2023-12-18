/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import gaiasky.util.i18n.I18n;

public class LongValidator extends NumberValidator<Long> {


    public LongValidator() {
        this(null);
    }

    public LongValidator(IValidator parent) {
        super(parent, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public LongValidator(long min,
                         long max) {
        super(null, min, max);
    }

    public LongValidator(IValidator parent,
                         long min,
                         long max) {
        super(parent, min, max);
    }

    @Override
    protected boolean validateLocal(String value) {
        long val;
        try {
            val = Long.parseLong(value);
        } catch (NumberFormatException e) {
            return false;
        }

        return val >= min && val <= max;
    }

    @Override
    public String getMinString() {
        return min == Long.MIN_VALUE ? I18n.msg("gui.infinity.minus") : Long.toString(min);
    }

    @Override
    public String getMaxString() {
        return max == Long.MAX_VALUE ? I18n.msg("gui.infinity") : Long.toString(max);
    }

}
