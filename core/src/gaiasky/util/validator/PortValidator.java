/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import gaiasky.util.parse.Parser;

/**
 * Validates port numbers.
 */
public class PortValidator extends CallbackValidator{

    @Override
    protected boolean validateLocal(String value) {
        int val;
        try {
            val = Parser.parseIntException(value);
        } catch (NumberFormatException e) {
            return false;
        }
        return val == 0 || (val >= 1024 && val <= 49151);
    }
}
