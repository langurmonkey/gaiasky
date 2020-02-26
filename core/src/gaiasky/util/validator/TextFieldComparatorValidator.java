/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

import gaiasky.util.scene2d.OwnTextField;

/**
 * A validator that compares the current value to the value of the
 * given text fields (lessThan and greaterThan). It assumes all values are floats.
 */
public class TextFieldComparatorValidator extends CallbackValidator {

    private OwnTextField[] lessThan, greaterThan;

    public TextFieldComparatorValidator(IValidator parent, OwnTextField[] lessThan, OwnTextField[] greaterThan) {
        super(parent);
        this.lessThan = lessThan;
        this.greaterThan = greaterThan;
    }

    @Override
    protected boolean validateLocal(String value) {
        try {
            float val = Float.valueOf(value);

            if (lessThan != null) {
                for (OwnTextField tf : lessThan) {
                    float v = Float.valueOf(tf.getText());
                    if (val >= v)
                        return false;
                }
            }
            if (greaterThan != null) {
                for (OwnTextField tf : greaterThan) {
                    float v = Float.valueOf(tf.getText());
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
