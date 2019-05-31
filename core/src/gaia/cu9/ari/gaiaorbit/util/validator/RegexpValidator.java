/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.validator;

public class RegexpValidator extends CallbackValidator {
    private String expr;

    public RegexpValidator(String expression) {
        this(null, expression);
    }

    public RegexpValidator(IValidator parent, String expression) {
        super(parent);
        this.expr = expression;
    }

    @Override
    protected boolean validateLocal(String value) {
        return value.matches(expr);
    }

}
