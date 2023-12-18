package gaiasky.util.validator;

import org.apache.commons.compress.harmony.pack200.NewAttributeBands.Call;

public abstract class NumberValidator<T extends Number> extends CallbackValidator {

    protected final T min;
    protected final T max;

    public NumberValidator(IValidator parent,
                           T min,
                           T max) {
        super(parent);
        this.min = min;
        this.max = max;
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }

    public abstract String getMinString();

    public abstract String getMaxString();
}
