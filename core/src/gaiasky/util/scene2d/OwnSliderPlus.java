/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Null;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.scene2d.OwnSlider.OwnSliderStyle;

import java.text.DecimalFormat;
import java.util.function.Function;

public class OwnSliderPlus extends Slider {

    private final Skin skin;
    private OwnSliderPlus me;
    private float ownWidth = 0f, ownHeight = 0f;
    private float mapMin, mapMax;
    private boolean map = false;
    private OwnLabel titleLabel, valueLabel;
    private boolean displayValueMapped = false;
    private String valuePrefix, valueSuffix;
    private DecimalFormat nf;
    // This function is applied to the value of this slider in order to
    // produce the label to be displayed.
    private Function<Float, String> valueLabelTransform;
    private Color labelColorBackup;

    public OwnSliderPlus(String title, float min, float max, float stepSize, float mapMin, float mapMax, Skin skin, String labelStyle) {
        super(min, max, stepSize, false, skin.get("big-horizontal", OwnSliderStyle.class));
        this.skin = skin;
        setUp(title, mapMin, mapMax, labelStyle);
    }

    public OwnSliderPlus(String title, float min, float max, float stepSize, float mapMin, float mapMax, Skin skin) {
        this(title, min, max, stepSize, mapMin, mapMax, skin, "default");
    }

    public OwnSliderPlus(String title, float min, float max, float stepSize, Skin skin) {
        super(min, max, stepSize, false, skin.get("big-horizontal", OwnSliderStyle.class));
        this.skin = skin;
        setUp(title, min, max, "default");
    }

    public OwnSliderPlus(String title, float min, float max, float stepSize, Skin skin, String style) {
        super(min, max, stepSize, false, skin.get(style, OwnSliderStyle.class));
        this.skin = skin;
        setUp(title, min, max, "default");
    }

    public OwnSliderPlus(String title, float min, float max, float stepSize, boolean vertical, Skin skin) {
        super(min, max, stepSize, vertical, skin.get("big-horizontal", OwnSliderStyle.class));
        this.skin = skin;
        setUp(title, min, max, "default");
    }

    public OwnSliderPlus(String title, float min, float max, float stepSize, boolean vertical, Skin skin, String labelStyleName) {
        super(min, max, stepSize, vertical, skin.get("big-horizontal", OwnSliderStyle.class));
        this.skin = skin;
        setUp(title, min, max, labelStyleName);
    }

    public void setUp(String title, float mapMin, float mapMax, String labelStyleName) {
        setUp(title, mapMin, mapMax, new DecimalFormat("####0.###"), labelStyleName);
    }

    public void setUp(String title, float mapMin, float mapMax, DecimalFormat nf, String labelStyleName) {
        this.me = this;
        this.nf = nf;
        setMapValues(mapMin, mapMax);

        if (title != null && !title.isEmpty()) {
            this.titleLabel = new OwnLabel(title, skin, labelStyleName);
        } else {
            this.titleLabel = null;
        }

        this.valueLabel = new OwnLabel(getValueString(), skin, labelStyleName);
        this.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                this.valueLabel.setText(getValueString());
                return true;
            }
            return false;
        });
        this.addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (actor == me) {
                    if (focused)
                        me.setLabelColor(ColorUtils.ddBrownC);
                    else
                        me.restoreLabelColor();
                }
            }
        });
    }

    public void setValueLabelTransform(Function<Float, String> transform) {
        this.valueLabelTransform = transform;
    }

    public void setNumberFormatter(DecimalFormat nf) {
        this.nf = nf;
    }

    public void setDisplayValueMapped(boolean displayValueMapped) {
        this.displayValueMapped = displayValueMapped;
    }

    public void setMapValues(float mapMin, float mapMax) {
        this.mapMin = mapMin;
        this.mapMax = mapMax;
        this.map = mapMin != getMinValue() || mapMax != getMaxValue();
    }

    public void removeMapValues() {
        this.mapMin = 0;
        this.mapMax = 0;
        this.map = false;
    }

    public String getValueString() {
        float actualValue = displayValueMapped ? getMappedValue() : getValue();
        String valueString = valueLabelTransform != null ? valueLabelTransform.apply(actualValue) : nf.format(actualValue);
        return (valuePrefix != null ? valuePrefix : "") + valueString + (valueSuffix != null ? valueSuffix : "");
    }

    @Override
    public boolean setValue(float value) {
        boolean result = super.setValue(value);
        this.valueLabel.setText(getValueString());
        return result;
    }

    public float getMappedValue() {
        if (map) {
            return MathUtilsDouble.lint(getValue(), getMinValue(), getMaxValue(), mapMin, mapMax);
        } else {
            return getValue();
        }
    }

    public void setMappedValue(double mappedValue) {
        setMappedValue((float) mappedValue);
    }

    public void setMappedValue(float mappedValue) {
        if (map) {
            setValue(MathUtilsDouble.lint(mappedValue, mapMin, mapMax, getMinValue(), getMaxValue()));
        } else {
            setValue(mappedValue);
        }
    }

    public void setValuePrefix(String valuePrefix) {
        this.valuePrefix = valuePrefix;
    }

    public void setValueSuffix(String valueSuffix) {
        this.valueSuffix = valueSuffix;
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
    public float getPrefWidth() {
        if (ownWidth != 0) {
            return ownWidth;
        } else {
            return super.getPrefWidth();
        }
    }

    @Override
    public float getPrefHeight() {
        if (ownHeight != 0) {
            return ownHeight;
        } else {
            return super.getPrefHeight();
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        float padX = 4.8f;
        float padY = 3.2f;
        if (titleLabel != null) {
            titleLabel.setPosition(getX() + padX, getY() + getHeight() - titleLabel.getHeight() - padY);
            titleLabel.draw(batch, parentAlpha);
        }
        if (valueLabel != null) {
            valueLabel.setPosition(getX() + getPrefWidth() - (valueLabel.getPrefWidth() + padX * 2f), getY() + getHeight() - valueLabel.getHeight() - padY);
            valueLabel.draw(batch, parentAlpha);
        }
    }

    protected @Null Drawable getBackgroundDrawable() {
        Drawable bg = super.getBackgroundDrawable();
        if (hasKeyboardFocus() && !isDisabled()) {
            bg = ((OwnSliderStyle) getStyle()).backgroundFocused;
        }
        return bg;
    }

    protected Drawable getKnobBeforeDrawable() {
        Drawable knobBefore = super.getKnobBeforeDrawable();
        if (hasKeyboardFocus() && !isDisabled()) {
            knobBefore = ((OwnSliderStyle) getStyle()).knobBeforeFocused;
        }
        return knobBefore;
    }

    @Override
    public void setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        if (valueLabel != null)
            valueLabel.setDisabled(disabled);
        if (titleLabel != null)
            titleLabel.setDisabled(disabled);
    }

    public void setLabelColor(Color c) {
        setLabelColor(c.r, c.g, c.b, c.a);
    }

    public void setLabelColor(float r, float g, float b, float a) {
        if (this.titleLabel != null) {
            labelColorBackup = this.titleLabel.getColor().cpy();
            this.titleLabel.setColor(r, g, b, a);
            if (this.valueLabel != null) {
                this.valueLabel.setColor(r, g, b, a);
            }
        }
    }

    public void restoreLabelColor() {
        if (labelColorBackup != null && this.titleLabel != null) {
            this.titleLabel.setColor(labelColorBackup);
            if (this.valueLabel != null) {
                this.valueLabel.setColor(labelColorBackup);
            }
        }
    }

}