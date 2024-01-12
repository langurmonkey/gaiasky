/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.OwnTextField;

public class TextFieldComparatorValidator extends CallbackValidator {

    private final OwnTextField[] lessThan;
    private final OwnTextField[] greaterThan;

    public TextFieldComparatorValidator(IValidator parent, OwnTextField[] lessThan, OwnTextField[] greaterThan) {
        super(parent);
        this.lessThan = lessThan;
        this.greaterThan = greaterThan;
    }

    @Override
    protected boolean validateLocal(String value) {
        try {
            float val = Parser.parseFloatException(value);

            if (lessThan != null) {
                for (OwnTextField tf : lessThan) {
                    float v = Parser.parseFloatException(tf.getText());
                    if (val >= v)
                        return false;
                }
            }
            if (greaterThan != null) {
                for (OwnTextField tf : greaterThan) {
                    float v = Parser.parseFloatException(tf.getText());
                    if (val <= v)
                        return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
