/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.scene2d.OwnSliderPlus;

public class VisualEffectsComponent extends GuiComponent implements IObserver {

    protected OwnSliderPlus starBrightness, starSize, starOpacity, ambientLight, labelSize, lineWidth, elevMult;

    protected INumberFormat nf;

    boolean flag = true;

    boolean hackProgrammaticChangeEvents = true;

    public VisualEffectsComponent(Skin skin, Stage stage) {
        super(skin, stage);
        this.nf = NumberFormatFactory.getFormatter("#0.0");
    }

    public void initialize() {
        float sliderWidth = 180 * GlobalConf.UI_SCALE_FACTOR;
        /** Star brightness **/
        starBrightness = new OwnSliderPlus(I18n.txt("gui.starbrightness"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_STAR_BRIGHT, Constants.MAX_STAR_BRIGHT, skin);
        starBrightness.setName("star brightness");
        starBrightness.setWidth(sliderWidth);
        starBrightness.setMappedValue(GlobalConf.scene.STAR_BRIGHTNESS);
        starBrightness.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.instance.post(Events.STAR_BRIGHTNESS_CMD, starBrightness.getMappedValue(), true);
                return true;
            }
            return false;
        });

        /** Star size **/
        starSize = new OwnSliderPlus(I18n.txt("gui.star.size"), Constants.MIN_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE, Constants.STEP_STAR_POINT_SIZE, skin);
        starSize.setName("star size");
        starSize.setWidth(sliderWidth);
        starSize.setMappedValue(GlobalConf.scene.STAR_POINT_SIZE);
        starSize.addListener(event -> {
            if (flag && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.STAR_POINT_SIZE_CMD, starSize.getMappedValue(), true);
                return true;
            }
            return false;
        });

        /** Star opacity **/
        starOpacity = new OwnSliderPlus(I18n.txt("gui.star.opacity"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_STAR_MIN_OPACITY, Constants.MAX_STAR_MIN_OPACITY, skin);
        starOpacity.setName("star opacity");
        starOpacity.setWidth(sliderWidth);
        starOpacity.setMappedValue(GlobalConf.scene.STAR_MIN_OPACITY);
        starOpacity.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.instance.post(Events.STAR_MIN_OPACITY_CMD, starOpacity.getMappedValue(), true);
                return true;
            }
            return false;
        });

        /** Ambient light **/
        ambientLight = new OwnSliderPlus(I18n.txt("gui.light.ambient"), Constants.MIN_AMBIENT_LIGHT, Constants.MAX_AMBIENT_LIGHT, Constants.SLIDER_STEP_TINY, skin);
        ambientLight.setName("ambient light");
        ambientLight.setWidth(sliderWidth);
        ambientLight.setMappedValue(GlobalConf.scene.AMBIENT_LIGHT);
        ambientLight.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.AMBIENT_LIGHT_CMD, ambientLight.getMappedValue());
                return true;
            }
            return false;
        });

        /** Label size **/
        labelSize = new OwnSliderPlus(I18n.txt("gui.label.size"), Constants.MIN_LABEL_SIZE, Constants.MAX_LABEL_SIZE, Constants.SLIDER_STEP_SMALL, skin);
        labelSize.setName("label size");
        labelSize.setWidth(sliderWidth);
        labelSize.setMappedValue(GlobalConf.scene.LABEL_SIZE_FACTOR);
        labelSize.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                float val = labelSize.getMappedValue();
                EventManager.instance.post(Events.LABEL_SIZE_CMD, val, true);
                return true;
            }
            return false;
        });

        /** Line width **/
        lineWidth = new OwnSliderPlus(I18n.txt("gui.line.width"), Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH, Constants.SLIDER_STEP_SMALL, Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH, skin);
        lineWidth.setName("line width");
        lineWidth.setWidth(sliderWidth);
        lineWidth.setMappedValue(GlobalConf.scene.LINE_WIDTH_FACTOR);
        lineWidth.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                float val = lineWidth.getMappedValue();
                EventManager.instance.post(Events.LINE_WIDTH_CMD, val, true);
                return true;
            }
            return false;
        });

        /** Elevation multiplier **/
        elevMult = new OwnSliderPlus(I18n.txt("gui.elevation.multiplier"), Constants.MIN_ELEVATION_MULT, Constants.MAX_ELEVATION_MULT, 0.1f, false, skin);
        elevMult.setName("elevation mult");
        elevMult.setWidth(sliderWidth);
        elevMult.setValue((float) MathUtilsd.roundAvoid(GlobalConf.scene.ELEVATION_MULTIPLIER, 1));
        elevMult.addListener(event -> {
            if (event instanceof ChangeEvent) {
                float val = elevMult.getValue();
                EventManager.instance.post(Events.ELEVATION_MUTLIPLIER_CMD, val, true);
                return true;
            }
            return false;
        });

        VerticalGroup lightingGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        lightingGroup.space(space6);
        lightingGroup.addActor(starBrightness);
        lightingGroup.addActor(starSize);
        lightingGroup.addActor(starOpacity);
        lightingGroup.addActor(ambientLight);
        lightingGroup.addActor(lineWidth);
        lightingGroup.addActor(labelSize);
        lightingGroup.addActor(elevMult);

        component = lightingGroup;

        EventManager.instance.subscribe(this, Events.STAR_POINT_SIZE_CMD, Events.STAR_BRIGHTNESS_CMD, Events.STAR_MIN_OPACITY_CMD, Events.LABEL_SIZE_CMD, Events.LINE_WIDTH_CMD);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case STAR_POINT_SIZE_CMD:
            if (!(boolean) data[1]) {
                flag = false;
                float newsize = (float) data[0];
                starSize.setMappedValue(newsize);
                flag = true;
            }
            break;
        case STAR_BRIGHTNESS_CMD:
            if (!(boolean) data[1]) {
                Float brightness = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                starBrightness.setMappedValue(brightness);
                hackProgrammaticChangeEvents = true;
            }
            break;
        case STAR_MIN_OPACITY_CMD:
            if (!(boolean) data[1]) {
                Float minopacity = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                starOpacity.setMappedValue(minopacity);
                hackProgrammaticChangeEvents = true;
            }
            break;
        case LABEL_SIZE_CMD:
            if(!(boolean) data[1]) {
                Float newsize = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                labelSize.setMappedValue(newsize);
                hackProgrammaticChangeEvents = true;
            }
            break;
        case LINE_WIDTH_CMD:
            if(!(boolean) data[1]) {
                Float newwidth = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                lineWidth.setMappedValue(newwidth);
                hackProgrammaticChangeEvents = true;
            }
            break;
        default:
            break;
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
