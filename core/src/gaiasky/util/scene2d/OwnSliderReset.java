/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import gaiasky.util.i18n.I18n;

import java.text.DecimalFormat;
import java.util.function.Function;

/**
 * Wrapper around {@link OwnSliderPlus} that adds a "Reset value" button to the right of the slider.
 */
public class OwnSliderReset extends Table implements Disableable {
    private Skin skin;
    private OwnSliderPlus slider;
    private OwnImageButton resetButton;
    private Float resetValue;
    private final float RESET_BUTTON_SIZE = 20f;
    private final float SLIDER_BUTTON_SPACE = 1f;

    // Constructors matching OwnSliderPlus
    public OwnSliderReset(String title, float min, float max, boolean logarithmic,
                          float resetValue, Skin skin, String labelStyle) {
        initializeSlider(title, min, max, logarithmic, skin, labelStyle);
        setResetValue(resetValue);
    }

    public OwnSliderReset(String title, float min, float max, float stepSize,
                          float mapMin, float mapMax, float resetValue, Skin skin, String labelStyle) {
        slider = new OwnSliderPlus(title, min, max, stepSize, mapMin, mapMax, skin, labelStyle);
        this.skin = skin;
        initializeLayout();
        setResetValue(resetValue);
    }

    public OwnSliderReset(String title, float min, float max, float stepSize,
                          float mapMin, float mapMax, float resetValue, Skin skin) {
        this(title, min, max, stepSize, mapMin, mapMax, resetValue, skin, "default");
    }

    public OwnSliderReset(String title, float min, float max, float stepSize, float resetValue,
                          Skin skin, String style) {
        slider = new OwnSliderPlus(title, min, max, stepSize, skin, style);
        this.skin = skin;
        initializeLayout();
        setResetValue(resetValue);
    }

    public OwnSliderReset(String title, float min, float max,
                          boolean logarithmic, float resetValue, Skin skin) {
        slider = new OwnSliderPlus(title, min, max, logarithmic, skin);
        this.skin = skin;
        initializeLayout();
        setResetValue(resetValue);
    }

    public OwnSliderReset(String title, float min, float max, float stepSize, float resetValue, Skin skin) {
        this(title, min, max, stepSize, false, resetValue, skin, "default");
    }

    public OwnSliderReset(String title, float min, float max, float stepSize,
                          boolean vertical, float resetValue, Skin skin, String labelStyleName) {
        slider = new OwnSliderPlus(title, min, max, stepSize, vertical, skin, labelStyleName);
        this.skin = skin;
        initializeLayout();
        setResetValue(resetValue);
    }

    private void initializeSlider(String title, float min, float max, boolean logarithmic,
                                  Skin skin, String labelStyle) {
        slider = new OwnSliderPlus(title, min, max, logarithmic, skin, labelStyle);
        this.skin = skin;
        initializeLayout();
    }

    private void initializeLayout() {
        // Add slider to take most of the space
        add(slider).expandX()
                .fillX();
    }

    /**
     * Controls the strength of the non-linear value mapping when logarithmic mode is enabled.
     * <p>
     * The slider applies a power-curve transform of the form {@code t^exp} to its normalized
     * position {@code t ∈ [0,1]}. This produces a customizable alternative to a true logarithmic scale:
     * <ul>
     *   <li>{@code exp = 1.0} → linear mapping (no non-linearity)</li>
     *   <li>{@code exp < 1.0} → expanded high-end resolution (e.g., 0.5 = square-root)</li>
     *   <li>{@code exp > 1.0} → expanded low-end resolution</li>
     * </ul>
     * <p>
     * The parameter must be strictly positive.
     */
    public void setLogarithmicExponent(double exp) {
        slider.setLogarithmicExponent(exp);
    }

    private void setResetValue(Float value) {
        this.resetValue = value;

        if (value != null && resetButton == null) {
            var size = RESET_BUTTON_SIZE;
            // Create reset button
            resetButton = new OwnImageButton(skin, "reload-s");
            resetButton.setSize(size, size);
            resetButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (resetValue != null) {
                        slider.setMappedValue(resetValue);
                    }
                }
            });
            resetButton.addListener(new OwnTextTooltip(I18n.msg("gui.reset"), skin));
            // Add button to the right of the slider
            add(resetButton).size(size, size)
                    .padLeft(SLIDER_BUTTON_SPACE);

        } else if (value == null && resetButton != null) {
            // Remove reset button
            resetButton.remove();
            resetButton = null;
        }
    }

    @Override
    public boolean addListener (EventListener listener) {
        // The listeners should go to the underlying slider.
        return slider.addListener(listener);
    }

    // Delegate all OwnSliderPlus methods
    public void setUp(String title, float mapMin, float mapMax, boolean logarithmic, String labelStyleName) {
        slider.setUp(title, mapMin, mapMax, logarithmic, labelStyleName);
    }

    public void setUp(String title, float mapMin, float mapMax, boolean logarithmic, DecimalFormat nf, String labelStyleName) {
        slider.setUp(title, mapMin, mapMax, logarithmic, nf, labelStyleName);
    }

    public void setValueLabelTransform(Function<Float, String> transform) {
        slider.setValueLabelTransform(transform);
    }

    public void setNumberFormatter(DecimalFormat nf) {
        slider.setNumberFormatter(nf);
    }

    public void setDisplayValueMapped(boolean displayValueMapped) {
        slider.setDisplayValueMapped(displayValueMapped);
    }

    public void setMapValues(float mapMin, float mapMax, boolean logarithmic) {
        slider.setMapValues(mapMin, mapMax, logarithmic);
    }

    public void removeMapValues() {
        slider.removeMapValues();
    }

    public String getValueString() {
        return slider.getValueString();
    }

    public boolean setValue(float value) {
        return slider.setValue(value);
    }

    public float getMappedValue() {
        return slider.getMappedValue();
    }

    public void setMappedValue(double mappedValue) {
        slider.setMappedValue(mappedValue);
    }

    public void setMappedValue(float mappedValue) {
        slider.setMappedValue(mappedValue);
    }

    public void setValuePrefix(String valuePrefix) {
        slider.setValuePrefix(valuePrefix);
    }

    public void setValueSuffix(String valueSuffix) {
        slider.setValueSuffix(valueSuffix);
    }

    @Override
    public void setWidth(float width) {
        slider.setWidth(resetButton != null ? width - RESET_BUTTON_SIZE - SLIDER_BUTTON_SPACE : width);
    }

    @Override
    public void setHeight(float height) {
        slider.setHeight(height);
    }

    @Override
    public void setSize(float width, float height) {
        slider.setSize(width, height);
    }

    @Override
    public float getPrefWidth() {
        float prefWidth = slider.getPrefWidth();
        if (resetButton != null) {
            prefWidth += resetButton.getWidth() + 2f; // Add button width and padding
        }
        return prefWidth;
    }

    @Override
    public float getPrefHeight() {
        return slider.getPrefHeight();
    }

    public boolean isDisabled() {
        return slider != null && slider.isDisabled();
    }

    public void setDisabled(boolean disabled) {
        slider.setDisabled(disabled);
        if (resetButton != null) {
            resetButton.setDisabled(disabled);
        }
    }

    public void setLabelColor(Color c) {
        slider.setLabelColor(c);
    }

    public void setLabelColor(float r, float g, float b, float a) {
        slider.setLabelColor(r, g, b, a);
    }

    public void restoreLabelColor() {
        slider.restoreLabelColor();
    }

    // Delegate Slider methods
    public float getValue() {
        return slider.getValue();
    }

    public float getMinValue() {
        return slider.getMinValue();
    }

    public float getMaxValue() {
        return slider.getMaxValue();
    }

    public float getStepSize() {
        return slider.getStepSize();
    }

    public void setStepSize(float stepSize) {
        slider.setStepSize(stepSize);
    }

    public boolean isDragging() {
        return slider.isDragging();
    }

    // Delegate Actor methods that might be needed
    @Override
    public void setColor(Color color) {
        super.setColor(color);
        slider.setColor(color);
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        super.setColor(r, g, b, a);
        slider.setColor(r, g, b, a);
    }

    // Get direct access to the internal slider if needed
    public OwnSliderPlus getSlider() {
        return slider;
    }

    public Button getResetButton() {
        return resetButton;
    }

    public Float getResetValue() {
        return resetValue;
    }

    public void setProgrammaticChangeEvents(boolean programmaticChangeEvents) {
        slider.setProgrammaticChangeEvents(programmaticChangeEvents);
    }
}

