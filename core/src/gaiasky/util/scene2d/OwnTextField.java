/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener.FocusEvent;
import com.badlogic.gdx.utils.Null;
import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;
import gaiasky.util.validator.CallbackValidator;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;
import gaiasky.util.validator.IntValidator;

/**
 * Extension of libgdx's text field that incorporates some QOL improvements like built-in validation
 * or a clear button.
 */
public class OwnTextField extends TextField {

    private final Skin skin;
    private float ownWidth = 0f, ownHeight = 0f;
    private IValidator validator = null;
    private String lastCorrectText = "";
    private Color regularColor;
    private Color errorColor;
    private final boolean clearButtonFlag = true;
    private final String clearButtonDrawableUp = "clear";
    private final String clearButtonDrawableDown = "clear-down";
    private Drawable clearDrawable;
    private float clearX, clearY, clearW, clearH;

    public OwnTextField(@Null String text, Skin skin) {
        super(text, new TextFieldStyle(skin.get(TextFieldStyle.class)));
        this.skin = skin;
        initClearButton();
    }

    public OwnTextField(String text, Skin skin, IValidator validator) {
        this(text, skin);
        this.validator = validator;
        initValidator();
        initClearButton();
    }

    public OwnTextField(String text, Skin skin, String styleName) {
        super(text, new TextFieldStyle(skin.get(styleName, TextFieldStyle.class)));
        this.skin = skin;
        initClearButton();
    }

    public OwnTextField(String text, Skin skin, String styleName, IValidator validator) {
        this(text, skin, styleName);
        this.validator = validator;
        initValidator();
    }

    public void initClearButton() {
        if (clearButtonFlag) {
            clearDrawable = skin.getDrawable(clearButtonDrawableUp);
            clearW = clearDrawable.getMinWidth();
            clearH = clearDrawable.getMinHeight();
            final var me = this;
            // Clear text if collision with drawable.
            this.addListener(new ClickListener() {
                public void clicked(InputEvent event, float x, float y) {
                    if (!me.isDisabled() && x >= clearX && x <= clearX + clearW && y >= clearY && y <= clearY + clearH) {
                        // Collision!
                        boolean bak = me.getProgrammaticChangeEvents();
                        me.setProgrammaticChangeEvents(true);
                        me.setText("");
                        me.setProgrammaticChangeEvents(bak);
                    }
                }

                @Override
                public boolean mouseMoved(InputEvent event, float x, float y) {
                    if (!me.isDisabled()) {
                        if (x >= clearX && x <= clearX + clearW && y >= clearY && y <= clearY + clearH) {
                            clearDrawable = skin.getDrawable(clearButtonDrawableDown);
                        } else {
                            clearDrawable = skin.getDrawable(clearButtonDrawableUp);
                        }
                    }
                    return super.mouseMoved(event, x, y);
                }
            });
        }
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
            if (validator instanceof FloatValidator fv) {
                addListener(new OwnTextTooltip(I18n.msg("gui.validator.values", fv.getMinString(), fv.getMaxString()), skin));
            } else if (validator instanceof IntValidator iv) {
                addListener(new OwnTextTooltip(I18n.msg("gui.validator.values", iv.getMinString(), iv.getMaxString()), skin));
            }
            if (validator instanceof CallbackValidator cv) {
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

    @Override
    public void moveCursor(boolean forward, boolean jump) {
        super.moveCursor(forward, jump);
    }

    public void goHome(boolean jump) {
        cursor = 0;
    }

    public void goEnd(boolean jump) {
        cursor = text.length();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (clearButtonFlag) {
            float x = getX();
            float y = getY();
            float width = getWidth();
            float height = getHeight();
            clearX = width - clearW - 6f;
            clearY = (height - clearH) / 2f;
            clearDrawable.draw(batch, x + clearX, y + clearY, clearW, clearH);
        }

    }
}