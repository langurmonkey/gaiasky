/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

public class LongValidator extends CallbackValidator {

    private final long min;
    private final long max;

    public LongValidator() {
        this(null);
    }

    public LongValidator(IValidator parent) {
        this(parent, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public LongValidator(long min, long max) {
        this(null, min, max);
    }

    public LongValidator(IValidator parent, long min, long max) {
        super(parent);
        this.min = min;
        this.max = max;
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
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

}
