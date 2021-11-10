package gaiasky.util.validator;

public class DirectoryNameValidator extends CallbackValidator {
    @Override
    protected boolean validateLocal(String value) {
        return value.matches("[\\w,-]+");
    }
}
