/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener.FocusEvent;
import com.badlogic.gdx.utils.Null;
import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;
import gaiasky.util.validator.CallbackValidator;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;
import gaiasky.util.validator.IntValidator;

/**
 * TextButton in which the cursor changes when the mouse rolls over.
 * It also fixes the size issue.
 */
public class OwnTextField extends TextField {

    private Skin skin;
    private float ownWidth = 0f, ownHeight = 0f;
    private IValidator validator = null;
    private String lastCorrectText = "";
    private Color regularColor;
    private Color errorColor;

    public OwnTextField (@Null String text, Skin skin) {
        super(text, new TextFieldStyle(skin.get(TextFieldStyle.class)));
        this.skin = skin;
    }

    public OwnTextField(String text, Skin skin, IValidator validator) {
        this(text, skin);
        this.validator = validator;
        initValidator();
    }

    public OwnTextField(String text, Skin skin, String styleName) {
        super(text, new TextFieldStyle(skin.get(styleName, TextFieldStyle.class)));
        this.skin = skin;
    }

    public OwnTextField(String text, Skin skin, String styleName, IValidator validator) {
        this(text, skin, styleName);
        this.validator = validator;
        initValidator();
    }

    public void setValidator(IValidator validator) {
        this.validator = validator;
        initValidator();
    }

    public void setErrorColor(Color errorColor) {
        this.errorColor = errorColor;
    }

    /**
     * Checks the validity of the value. If the text field has no validator, all
     * values are valid. If it has a validator, it checks whether the value
     * is ok
     *
     * @return True if the value is valid or the text field has no validator, false otherwise
     */
    public boolean isValid() {
        return this.validator == null || this.validator.validate(this.getText());
    }

    public float getFloatValue(float defaultValue) {
        return (float) getDoubleValue(defaultValue);
    }

    public double getDoubleValue(double defaultValue) {
        try {
            return Parser.parseFloatException(getText());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long getIntValue(int defaultValue) {
        return (int) getLongValue(defaultValue);
    }

    public long getLongValue(long defaultValue) {
        try {
            return Parser.parseLongException(getText());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void initValidator() {
        if (validator != null) {
            errorColor = new Color(0xff3333ff);
            regularColor = getColor().cpy();
            addListener(event -> {
                if (event instanceof ChangeEvent) {
                    String str = getText();
                    if (validator.validate(str)) {
                        this.setColor(regularColor);
                        this.getStyle().fontColor = regularColor;
                        lastCorrectText = str;
                    } else {
                        this.setColor(errorColor);
                        this.getStyle().fontColor = errorColor;
                    }
                    return true;
                } else if (event instanceof FocusEvent) {
                    if (!((FocusEvent) event).isFocused()) {
                        // We lost focus, return to last correct text if current not valid
                        String str = getText();
                        if (!validator.validate(str)) {
                            this.setText(lastCorrectText);
                            this.setColor(regularColor);
                            this.getStyle().fontColor = regularColor;
                        }
                    }
                    return true;
                }
                return false;
            });
            addValidatorTooltip();
        }
    }

    private void addValidatorTooltip() {
        addValidatorTooltip(validator);
    }

    private void addValidatorTooltip(IValidator validator) {
        if (validator != null) {
            if (validator instanceof FloatValidator) {
                FloatValidator fv = (FloatValidator) validator;
                addListener(new OwnTextTooltip(I18n.msg("gui.validator.values", fv.getMin(), fv.getMax()), skin));
            } else if (validator instanceof IntValidator) {
                IntValidator iv = (IntValidator) validator;
                addListener(new OwnTextTooltip(I18n.msg("gui.validator.values", iv.getMin(), iv.getMax()), skin));
            }
            if (validator instanceof CallbackValidator) {
                CallbackValidator cv = (CallbackValidator) validator;
                addValidatorTooltip(cv.getParent());
            }
        }

    }

    @Override
    public void setWidth(float width) {
        ownWidth = width;
        super.setWidth(width);
    }

    @Override
    public void setHeight(float height) {
        ownHeight = height;
        super.setHeight(height);
    }

    @Override
    public void setSize(float width, float height) {
        ownWidth = width;
        ownHeight = height;
        super.setSize(width, height);
    }

    @Override
    public float getPrefWidth() { //-V6052
        if (ownWidth != 0) {
            return ownWidth;
        } else {
            return super.getPrefWidth();
        }
    }

    @Override
    public float getPrefHeight() { //-V6052
        if (ownHeight != 0) {
            return ownHeight;
        } else {
            return super.getPrefHeight();
        }
    }

}