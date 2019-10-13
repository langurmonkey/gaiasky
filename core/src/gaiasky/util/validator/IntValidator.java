/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

public class IntValidator extends CallbackValidator {

    private int min;
    private int max;

    public IntValidator() {
        this(null);
    }
    public IntValidator(IValidator parent) {
        this(parent, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public IntValidator(int min, int max) {
        this(null, min, max);
    }
    public IntValidator(IValidator parent, int min, int max) {
        super(parent);
        this.min = min;
        this.max = max;
    }

    @Override
    protected boolean validateLocal(String value) {
        Integer val = null;
        try {
            val = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }

        return val >= min && val <= max;
    }

}
