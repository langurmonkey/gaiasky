/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pools;
import gaiasky.util.math.MathUtilsDouble;

import java.text.DecimalFormat;

public class OwnSlider extends Slider {
    private float ownWidth = 0f, ownHeight = 0f;
    private float mapMin, mapMax;
    private boolean map = false;
    private Skin skin;
    private boolean displayValueMapped = false;
    private boolean showValueLabel = true;
    private OwnLabel valueLabel;
    private String valuePrefix, valueSuffix;
    private float padX = 4.8f;
    private float padY = 4.8f;
    private DecimalFormat nf;
    private EventListener labelListener;
    private boolean programmaticChangeEvents = true;

    public OwnSlider(float min,
                     float max,
                     float stepSize,
                     float mapMin,
                     float mapMax,
                     boolean vertical,
                     Skin skin) {
        super(min, max, stepSize, vertical, skin.get("default-" + (vertical ? "vertical" : "horizontal"), OwnSliderStyle.class));
        if (vertical) {
            padX = -8;
        } else {
            padY = -1;
        }
        this.skin = skin;
        setUp(mapMin, mapMax);
    }

    public OwnSlider(float min,
                     float max,
                     float stepSize,
                     float mapMin,
                     float mapMax,
                     Skin skin) {
        this(min, max, stepSize, mapMin, mapMax, false, skin);
    }

    public OwnSlider(float min,
                     float max,
                     float stepSize,
                     Skin skin) {
        this(min, max, stepSize, min, max, false, skin);
    }

    public OwnSlider(float min,
                     float max,
                     float stepSize,
                     boolean vertical,
                     Skin skin) {
        this(min, max, stepSize, min, max, vertical, skin);
    }

    public OwnSlider(float min,
                     float max,
                     float stepSize,
                     boolean vertical,
                     Skin skin,
                     String styleName) {
        super(min, max, stepSize, vertical, skin.get(styleName, OwnSliderStyle.class));
    }

    public void setValueFormatter(DecimalFormat df) {
        this.nf = df;
    }

    public void setUp(float mapMin,
                      float mapMax) {
        setUp(mapMin, mapMax, new DecimalFormat("####0.0#"));
    }

    public void setUp(float mapMin,
                      float mapMax,
                      DecimalFormat nf) {
        this.nf = nf;
        setMapValues(mapMin, mapMax);

        this.valueLabel = new OwnLabel(getValueString(), skin);

        this.labelListener = (event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                this.valueLabel.setText(getValueString());
                return true;
            }
            return false;
        };
        addListener(this.labelListener);
    }

    public void setMapValues(float mapMin,
                             float mapMax) {
        this.mapMin = mapMin;
        this.mapMax = mapMax;
        this.map = true;
    }

    public void removeMapValues() {
        this.mapMin = 0;
        this.mapMax = 0;
        this.map = false;
    }

    public String getValueString() {
        return (valuePrefix != null ? valuePrefix : "") + nf.format((displayValueMapped ? getMappedValue() : getValue())) + (valueSuffix != null ? valueSuffix : "");
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
        // Force label listener to update even when programmatic change events are off.
        if (!programmaticChangeEvents && labelListener != null) {
            ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
            labelListener.handle(changeEvent);
            Pools.free(changeEvent);
        }

    }

    public void setDisplayValueMapped(boolean displayValueMapped) {
        this.displayValueMapped = displayValueMapped;
    }

    public void showValueLabel(boolean showValueLabel) {
        this.showValueLabel = showValueLabel;
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
    public void setSize(float width,
                        float height) {
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
    public void draw(Batch batch,
                     float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (valueLabel != null && showValueLabel) {
            if (this.isVertical()) {
                valueLabel.setPosition(getX() + padX, getY() + getPrefHeight() - (valueLabel.getPrefHeight() + padY));
            } else {
                valueLabel.setPosition(getX() + getPrefWidth() - (valueLabel.getPrefWidth() + padX), getY() + padY);
            }
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
    public void setProgrammaticChangeEvents(boolean programmaticChangeEvents) {
        this.programmaticChangeEvents = programmaticChangeEvents;
        super.setProgrammaticChangeEvents(programmaticChangeEvents);
    }

    @Override
    public void setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        if (valueLabel != null)
            valueLabel.setDisabled(disabled);
    }

    static public class OwnSliderStyle extends SliderStyle {
        public @Null Drawable backgroundFocused, knobBeforeFocused;

        public OwnSliderStyle() {
        }

        public OwnSliderStyle(OwnSliderStyle style) {
            super(style);
            backgroundFocused = style.backgroundFocused;
            knobBeforeFocused = style.knobBeforeFocused;
        }
    }
}
