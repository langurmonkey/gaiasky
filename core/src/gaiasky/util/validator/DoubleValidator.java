/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import gaiasky.util.parse.Parser;

public class DoubleValidator extends CallbackValidator {

    private final double min;
    private final double max;

    public DoubleValidator(double min, double max) {
        this(null, min, max);
    }

    public DoubleValidator(IValidator parent, double min, double max) {
        super(parent);
        this.min = min;
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    @Override
    protected boolean validateLocal(String value) {
        Double val;
        try {
            val = Parser.parseDouble(value);
        } catch (NumberFormatException e) {
            return false;
        }

        return val >= min && val <= max;
    }

}
