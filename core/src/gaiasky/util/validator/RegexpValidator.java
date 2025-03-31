/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

/**
 * Validator that checks that the given string matches a regular expression.
 */
public class RegexpValidator extends CallbackValidator {
    private final String expr;

    public RegexpValidator(String expression) {
        this(null, expression);
    }

    public RegexpValidator(IValidator parent, String expression) {
        super(parent);
        this.expr = expression;
    }

    @Override
    protected boolean validateLocal(String value) {
        return value.matches(expr);
    }

}
