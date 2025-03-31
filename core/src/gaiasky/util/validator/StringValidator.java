package gaiasky.util.validator;

/**
 * Validates strings according to a list of banned characters.
 */
public class StringValidator extends CallbackValidator {

    private Character[] banned;

    public StringValidator(IValidator parent, Character[] banned) {
        super(parent);
        this.banned = banned;
    }
    public StringValidator(Character[] banned) {
       this.banned = banned;
    }

    @Override
    protected boolean validateLocal(String value) {
        for (Character c : banned) {
            if (value.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }
}
