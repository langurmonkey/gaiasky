/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.validator;

public abstract class CallbackValidator implements IValidator {

    private Runnable isValidCallback, isInvalidCallback;
    private IValidator parent;

    public CallbackValidator() {
        super();
    }

    public CallbackValidator(IValidator parent) {
        super();
        this.parent = parent;
    }

    public CallbackValidator(Runnable correctCallback, Runnable incorrectCallback) {
        super();
        this.isValidCallback = correctCallback;
        this.isInvalidCallback = incorrectCallback;
    }

    public void setIsValidCallback(Runnable isValidCallback) {
        this.isValidCallback = isValidCallback;
    }

    public void setIsInvalidCallback(Runnable isInvalidCallback) {
        this.isInvalidCallback = isInvalidCallback;
    }

    protected void runIsValidCallback() {
        if (isValidCallback != null)
            isValidCallback.run();
    }

    protected void runIsInvalidCallback() {
        if (isInvalidCallback != null)
            isInvalidCallback.run();
    }

    public IValidator getParent() {
        return parent;
    }

    @Override
    public boolean validate(String value) {
        boolean valid = validateLocal(value);
        if (valid)
            runIsValidCallback();
        else
            runIsInvalidCallback();
        return valid && (parent == null || parent.validate(value));
    }

    protected abstract boolean validateLocal(String value);

}
