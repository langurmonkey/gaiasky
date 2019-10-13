/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

public class LengthValidator extends CallbackValidator {
    private int minLength, maxLength;

    public LengthValidator(int minLength, int maxLength) {
        this(null, minLength, maxLength);
    }

    public LengthValidator(IValidator parent, int minLength, int maxLength) {
        super(parent);
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    protected boolean validateLocal(String value) {
        return value.length() >= minLength && value.length() <= maxLength;
    }

}
