/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interafce.ControlsWindow;
import gaiasky.util.*;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.scene2d.OwnSliderPlus;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class VisualEffectsComponent extends GuiComponent implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(VisualEffectsComponent.class);

    private OwnSliderPlus starBrightness, starBrightnessPower, starSize, starMinOpacity;
    private OwnSliderPlus ambientLight, labelSize, lineWidth, elevMult;

    boolean flag = true;

    boolean hackProgrammaticChangeEvents = true;

    public VisualEffectsComponent(Skin skin, Stage stage) {
        super(skin, stage);
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        float contentWidth = ControlsWindow.getContentWidth();
        /* Star brightness */
        starBrightness = new OwnSliderPlus(I18n.txt("gui.starbrightness"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP_TINY, Constants.MIN_STAR_BRIGHTNESS, Constants.MAX_STAR_BRIGHTNESS, skin);
        starBrightness.setName("star brightness");
        starBrightness.setWidth(contentWidth);
        starBrightness.setMappedValue(Settings.settings.scene.star.brightness);
        starBrightness.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.instance.post(Events.STAR_BRIGHTNESS_CMD, starBrightness.getMappedValue(), true);
                return true;
            }
            return false;
        });

        /* Star brightness power */
        starBrightnessPower = new OwnSliderPlus(I18n.txt("gui.starbrightness.pow"), Constants.MIN_STAR_BRIGHTNESS_POW, Constants.MAX_STAR_BRIGHTNESS_POW, Constants.SLIDER_STEP_TINY, skin);
        starBrightnessPower.setName("star brightness power");
        starBrightnessPower.setWidth(contentWidth);
        starBrightnessPower.setMappedValue(Settings.settings.scene.star.power);
        starBrightnessPower.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.instance.post(Events.STAR_BRIGHTNESS_POW_CMD, starBrightnessPower.getValue(), true);
                return true;
            }
            return false;
        });

        /* Star size */
        starSize = new OwnSliderPlus(I18n.txt("gui.star.size"), Constants.MIN_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE, Constants.SLIDER_STEP_TINY, skin);
        starSize.setName("star size");
        starSize.setWidth(contentWidth);
        starSize.setMappedValue(Settings.settings.scene.star.pointSize);
        starSize.addListener(event -> {
            if (flag && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.STAR_POINT_SIZE_CMD, starSize.getMappedValue(), true);
                return true;
            }
            return false;
        });

        /* Star min opacity */
        starMinOpacity = new OwnSliderPlus(I18n.txt("gui.star.opacity"), Constants.MIN_STAR_MIN_OPACITY, Constants.MAX_STAR_MIN_OPACITY, Constants.SLIDER_STEP_TINY, skin);
        starMinOpacity.setName("star min opacity");
        starMinOpacity.setWidth(contentWidth);
        starMinOpacity.setMappedValue(Settings.settings.scene.star.opacity[0]);
        starMinOpacity.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.instance.post(Events.STAR_MIN_OPACITY_CMD, starMinOpacity.getMappedValue(), true);
                return true;
            }
            return false;
        });

        /* Ambient light */
        ambientLight = new OwnSliderPlus(I18n.txt("gui.light.ambient"), Constants.MIN_AMBIENT_LIGHT, Constants.MAX_AMBIENT_LIGHT, Constants.SLIDER_STEP_TINY, skin);
        ambientLight.setName("ambient light");
        ambientLight.setWidth(contentWidth);
        ambientLight.setMappedValue(Settings.settings.scene.renderer.ambient);
        ambientLight.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.AMBIENT_LIGHT_CMD, ambientLight.getMappedValue());
                return true;
            }
            return false;
        });

        /* Label size */
        labelSize = new OwnSliderPlus(I18n.txt("gui.label.size"), Constants.MIN_LABEL_SIZE, Constants.MAX_LABEL_SIZE, Constants.SLIDER_STEP_TINY, skin);
        labelSize.setName("label size");
        labelSize.setWidth(contentWidth);
        labelSize.setMappedValue(Settings.settings.scene.label.size);
        labelSize.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                float val = labelSize.getMappedValue();
                EventManager.instance.post(Events.LABEL_SIZE_CMD, val, true);
                return true;
            }
            return false;
        });

        /* Line width */
        lineWidth = new OwnSliderPlus(I18n.txt("gui.line.width"), Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH, Constants.SLIDER_STEP_TINY, Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH, skin);
        lineWidth.setName("line width");
        lineWidth.setWidth(contentWidth);
        lineWidth.setMappedValue(Settings.settings.scene.lineWidth);
        lineWidth.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                float val = lineWidth.getMappedValue();
                EventManager.instance.post(Events.LINE_WIDTH_CMD, val, true);
                return true;
            }
            return false;
        });

        /* Elevation multiplier */
        elevMult = new OwnSliderPlus(I18n.txt("gui.elevation.multiplier"), Constants.MIN_ELEVATION_MULT, Constants.MAX_ELEVATION_MULT, 0.1f, false, skin);
        elevMult.setName("elevation mult");
        elevMult.setWidth(contentWidth);
        elevMult.setValue((float) MathUtilsd.roundAvoid(Settings.settings.scene.renderer.elevation.multiplier, 1));
        elevMult.addListener(event -> {
            if (event instanceof ChangeEvent) {
                float val = elevMult.getValue();
                EventManager.instance.post(Events.ELEVATION_MULTIPLIER_CMD, val, true);
                return true;
            }
            return false;
        });

        /* Reset defaults */
        OwnTextIconButton resetDefaults = new OwnTextIconButton(I18n.txt("gui.resetdefaults"), skin, "reset");
        resetDefaults.align(Align.center);
        resetDefaults.setWidth(contentWidth);
        resetDefaults.addListener(new OwnTextTooltip(I18n.txt("gui.resetdefaults.tooltip"), skin));
        resetDefaults.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Read defaults from internal settings file
                try {
                    Path confFolder = Settings.settings.assetsPath("conf");
                    Path internalFolderConfFile = confFolder.resolve(SettingsManager.getConfigFileName(Settings.settings.runtime.openVr || Settings.settings.runtime.OVR));
                    Yaml yaml = new Yaml();
                    Map<Object, Object> conf = yaml.load(Files.newInputStream(internalFolderConfFile));

                    float br = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("brightness")).floatValue();
                    float pow = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("power")).floatValue();
                    float ss = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("pointSize")).floatValue();
                    float pam = (((java.util.List<Double>) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("opacity")).get(0)).floatValue();
                    float amb = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("renderer")).get("ambient")).floatValue();
                    float ls = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("label")).get("size")).floatValue();
                    float lw = ((Double) ((Map<String, Object>) conf.get("scene")).get("lineWidth")).floatValue();
                    float em = ((Double) ((Map<String, Object>) ((Map<String, Object>) ((Map<Object, Object>) conf.get("scene")).get("renderer")).get("elevation")).get("multiplier")).floatValue();

                    // Events
                    EventManager m = EventManager.instance;
                    m.post(Events.STAR_BRIGHTNESS_CMD, br, false);
                    m.post(Events.STAR_BRIGHTNESS_POW_CMD, pow, false);
                    m.post(Events.STAR_POINT_SIZE_CMD, ss, false);
                    m.post(Events.STAR_MIN_OPACITY_CMD, pam, false);
                    m.post(Events.AMBIENT_LIGHT_CMD, amb, false);
                    m.post(Events.LABEL_SIZE_CMD, ls, false);
                    m.post(Events.LINE_WIDTH_CMD, lw, false);
                    m.post(Events.ELEVATION_MULTIPLIER_CMD, em, false);

                } catch (IOException e) {
                    logger.error(e, "Error loading default configuration file");
                }

                return true;
            }
            return false;
        });

        /* Add to group */
        VerticalGroup lightingGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        lightingGroup.space(pad9);
        lightingGroup.addActor(starBrightness);
        lightingGroup.addActor(starBrightnessPower);
        lightingGroup.addActor(starSize);
        lightingGroup.addActor(starMinOpacity);
        lightingGroup.addActor(ambientLight);
        lightingGroup.addActor(lineWidth);
        lightingGroup.addActor(labelSize);
        lightingGroup.addActor(elevMult);
        lightingGroup.addActor(resetDefaults);

        component = lightingGroup;

        EventManager.instance.subscribe(this, Events.STAR_POINT_SIZE_CMD, Events.STAR_BRIGHTNESS_CMD, Events.STAR_BRIGHTNESS_POW_CMD, Events.STAR_MIN_OPACITY_CMD, Events.LABEL_SIZE_CMD, Events.LINE_WIDTH_CMD);
    }

    @Override
    public void notify(final Events event, final Object... data) {
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
        case STAR_BRIGHTNESS_POW_CMD:
            if (!(boolean) data[1]) {
                Float pow = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                starBrightnessPower.setMappedValue(pow);
                hackProgrammaticChangeEvents = true;
            }
            break;
        case STAR_MIN_OPACITY_CMD:
            if (!(boolean) data[1]) {
                Float minopacity = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                starMinOpacity.setMappedValue(minopacity);
                hackProgrammaticChangeEvents = true;
            }
            break;
        case LABEL_SIZE_CMD:
            if (!(boolean) data[1]) {
                Float newsize = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                labelSize.setMappedValue(newsize);
                hackProgrammaticChangeEvents = true;
            }
            break;
        case LINE_WIDTH_CMD:
            if (!(boolean) data[1]) {
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
