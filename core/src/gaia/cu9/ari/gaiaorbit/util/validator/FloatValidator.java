package gaia.cu9.ari.gaiaorbit.util.validator;

public class FloatValidator extends CallbackValidator {

    private float min;
    private float max;

    public FloatValidator(float min, float max) {
        this(null, min, max);
    }

    public FloatValidator(IValidator parent, float min, float max) {
        super(parent);
        this.min = min;
        this.max = max;
    }

    @Override
    protected boolean validateLocal(String value) {
        Float val;
        try {
            val = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return false;
        }

        return val >= min && val <= max;
    }

}
