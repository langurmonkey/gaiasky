/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.util.math.MathUtilsd;

import java.text.DecimalFormat;

public class OwnSlider extends Slider {
    private float ownwidth = 0f, ownheight = 0f;
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

    public OwnSlider(float min, float max, float stepSize, float mapMin, float mapMax, boolean vertical, Skin skin) {
        super(min, max, stepSize, vertical, skin);
        if (vertical) {
            padX = -8;
        } else {
            padY = -1;
        }
        this.skin = skin;
        setUp(mapMin, mapMax);
    }

    public OwnSlider(float min, float max, float stepSize, float mapMin, float mapMax, Skin skin) {
        this(min, max, stepSize, mapMin, mapMax, false, skin);
    }

    public OwnSlider(float min, float max, float stepSize, Skin skin) {
        this(min, max, stepSize, min, max, false, skin);
    }

    public OwnSlider(float min, float max, float stepSize, boolean vertical, Skin skin) {
        this(min, max, stepSize, min, max, vertical, skin);
    }

    public OwnSlider(float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
        super(min, max, stepSize, vertical, skin, styleName);
    }

    public void setUp(float mapMin, float mapMax) {
        setUp(mapMin, mapMax, new DecimalFormat("####0.0#"));
    }

    public void setUp(float mapMin, float mapMax, DecimalFormat nf) {
        this.nf = nf;
        setMapValues(mapMin, mapMax);

        this.valueLabel = new OwnLabel(getValueString(), skin);
        this.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                this.valueLabel.setText(getValueString());
                return true;
            }
            return false;
        });
    }

    public void setMapValues(float mapMin, float mapMax) {
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
            return MathUtilsd.lint(getValue(), getMinValue(), getMaxValue(), mapMin, mapMax);
        } else {
            return getValue();
        }
    }

    public void setMappedValue(double mappedValue) {
        setMappedValue((float) mappedValue);
    }

    public void setMappedValue(float mappedValue) {
        if (map) {
            setValue(MathUtilsd.lint(mappedValue, mapMin, mapMax, getMinValue(), getMaxValue()));
        } else {
            setValue(mappedValue);
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
        ownwidth = width;
        super.setWidth(width);
    }

    @Override
    public void setHeight(float height) {
        ownheight = height;
        super.setHeight(height);
    }

    @Override
    public void setSize(float width, float height) {
        ownwidth = width;
        ownheight = height;
        super.setSize(width, height);
    }

    @Override
    public float getPrefWidth() {
        if (ownwidth != 0) {
            return ownwidth;
        } else {
            return super.getPrefWidth();
        }
    }

    @Override
    public float getPrefHeight() {
        if (ownheight != 0) {
            return ownheight;
        } else {
            return super.getPrefHeight();
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
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

    @Override
    public void setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        if (valueLabel != null)
            valueLabel.setDisabled(disabled);
    }
}
