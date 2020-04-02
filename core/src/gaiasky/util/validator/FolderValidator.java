package gaiasky.util.validator;

public class FolderValidator extends CallbackValidator {
    public FolderValidator() {
        super();
    }

    @Override
    protected boolean validateLocal(String value) {
        return !value.contains("/") && !value.contains("\\");
    }
}
