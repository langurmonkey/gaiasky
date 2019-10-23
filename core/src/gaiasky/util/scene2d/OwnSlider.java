/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import gaiasky.util.math.MathUtilsd;

public class OwnSlider extends Slider {
    private float ownwidth = 0f, ownheight = 0f;
    private float mapMin, mapMax;
    private boolean map = false;

    public OwnSlider(float min, float max, float stepSize, float mapMin, float mapMax, Skin skin){
        super(min, max, stepSize, false, skin);
        setMapValues(mapMin, mapMax);
    }

    public OwnSlider(float min, float max, float stepSize, Skin skin){
        super(min, max, stepSize, false, skin);
    }

    public OwnSlider(float min, float max, float stepSize, boolean vertical, Skin skin) {
        super(min, max, stepSize, vertical, skin);
    }

    public OwnSlider(float min, float max, float stepSize, boolean vertical, Skin skin, String styleName) {
        super(min, max, stepSize, vertical, skin, styleName);
    }

    public void setMapValues(float mapMin, float mapMax){
        this.mapMin = mapMin;
        this.mapMax = mapMax;
        this.map = true;
    }

    public void removeMapValues(){
        this.mapMin = 0;
        this.mapMax = 0;
        this.map = false;
    }

    public float getMappedValue(){
        if(map){
            return MathUtilsd.lint(getValue(), getMinValue(), getMaxValue(), mapMin, mapMax);
        }else{
            return getValue();
        }
    }

    public void setMappedValue(double mappedValue){
        setMappedValue((float) mappedValue);
    }

    public void setMappedValue(float mappedValue){
        if(map){
            setValue(MathUtilsd.lint(mappedValue, mapMin, mapMax, getMinValue(), getMaxValue()));
        } else {
            setValue(mappedValue);
        }
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

}
