/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.ControlsWindow;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SettingsManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.scene2d.OwnSliderPlus;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class VisualSettingsComponent extends GuiComponent implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(VisualSettingsComponent.class);
    boolean hackProgrammaticChangeEvents = true;
    private OwnSliderPlus starBrightness, magnitudeMultiplier, starGlowFactor, pointSize, starBaseLevel;
    private OwnSliderPlus ambientLight, labelSize, lineWidth, elevMult;

    public VisualSettingsComponent(Skin skin, Stage stage) {
        super(skin, stage);
    }

    public void initialize() {
        float contentWidth = ControlsWindow.getContentWidth();
        /* Star brightness */
        starBrightness = new OwnSliderPlus(I18n.msg("gui.star.brightness"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP_TINY, Constants.MIN_STAR_BRIGHTNESS, Constants.MAX_STAR_BRIGHTNESS, skin);
        starBrightness.addListener(new OwnTextTooltip(I18n.msg("gui.star.brightness.info"), skin));
        starBrightness.setWidth(contentWidth);
        starBrightness.setMappedValue(Settings.settings.scene.star.brightness);
        starBrightness.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.publish(Event.STAR_BRIGHTNESS_CMD, starBrightness, starBrightness.getMappedValue());
                return true;
            }
            return false;
        });

        /* Star brightness power */
        magnitudeMultiplier = new OwnSliderPlus(I18n.msg("gui.star.brightness.pow"), Constants.MIN_STAR_BRIGHTNESS_POW, Constants.MAX_STAR_BRIGHTNESS_POW, Constants.SLIDER_STEP_TINY, skin);
        magnitudeMultiplier.addListener(new OwnTextTooltip(I18n.msg("gui.star.brightness.pow.info"), skin));
        magnitudeMultiplier.setWidth(contentWidth);
        magnitudeMultiplier.setMappedValue(Settings.settings.scene.star.power);
        magnitudeMultiplier.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.publish(Event.STAR_BRIGHTNESS_POW_CMD, magnitudeMultiplier, magnitudeMultiplier.getValue());
                return true;
            }
            return false;
        });

        /* Star glow factor */
        starGlowFactor = new OwnSliderPlus(I18n.msg("gui.star.glowfactor"), Constants.MIN_STAR_GLOW_FACTOR, Constants.MAX_STAR_GLOW_FACTOR, Constants.SLIDER_STEP_TINY * 0.1f, skin);
        starGlowFactor.addListener(new OwnTextTooltip(I18n.msg("gui.star.glowfactor.info"), skin));
        starGlowFactor.setWidth(contentWidth);
        starGlowFactor.setMappedValue(Settings.settings.scene.star.glowFactor);
        starGlowFactor.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.publish(Event.STAR_GLOW_FACTOR_CMD, starGlowFactor, starGlowFactor.getValue());
                return true;
            }
            return false;
        });

        /* Point size */
        pointSize = new OwnSliderPlus(I18n.msg("gui.star.size"), Constants.MIN_STAR_POINT_SIZE, Constants.MAX_STAR_POINT_SIZE, Constants.SLIDER_STEP_TINY, skin);
        pointSize.addListener(new OwnTextTooltip(I18n.msg("gui.star.size.info"), skin));
        pointSize.setWidth(contentWidth);
        pointSize.setMappedValue(Settings.settings.scene.star.pointSize);
        pointSize.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.publish(Event.STAR_POINT_SIZE_CMD, pointSize, pointSize.getMappedValue());
                return true;
            }
            return false;
        });

        /* Star min opacity */
        starBaseLevel = new OwnSliderPlus(I18n.msg("gui.star.opacity"), Constants.MIN_STAR_MIN_OPACITY, Constants.MAX_STAR_MIN_OPACITY, Constants.SLIDER_STEP_TINY, skin);
        starBaseLevel.addListener(new OwnTextTooltip(I18n.msg("gui.star.opacity"), skin));
        starBaseLevel.setWidth(contentWidth);
        starBaseLevel.setMappedValue(Settings.settings.scene.star.opacity[0]);
        starBaseLevel.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.publish(Event.STAR_BASE_LEVEL_CMD, starBaseLevel, starBaseLevel.getMappedValue());
                return true;
            }
            return false;
        });

        /* Ambient light */
        ambientLight = new OwnSliderPlus(I18n.msg("gui.light.ambient"), Constants.MIN_AMBIENT_LIGHT, Constants.MAX_AMBIENT_LIGHT, Constants.SLIDER_STEP_TINY, skin);
        ambientLight.setWidth(contentWidth);
        ambientLight.setMappedValue(Settings.settings.scene.renderer.ambient);
        ambientLight.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.AMBIENT_LIGHT_CMD, ambientLight, ambientLight.getMappedValue());
                return true;
            }
            return false;
        });

        /* Label size */
        labelSize = new OwnSliderPlus(I18n.msg("gui.label.size"), Constants.MIN_LABEL_SIZE, Constants.MAX_LABEL_SIZE, Constants.SLIDER_STEP_TINY, skin);
        labelSize.setWidth(contentWidth);
        labelSize.setMappedValue(Settings.settings.scene.label.size);
        labelSize.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                float val = labelSize.getMappedValue();
                EventManager.publish(Event.LABEL_SIZE_CMD, labelSize, val);
                return true;
            }
            return false;
        });

        /* Line width */
        lineWidth = new OwnSliderPlus(I18n.msg("gui.line.width"), Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH, Constants.SLIDER_STEP_TINY, Constants.MIN_LINE_WIDTH, Constants.MAX_LINE_WIDTH, skin);
        lineWidth.setWidth(contentWidth);
        lineWidth.setMappedValue(Settings.settings.scene.lineWidth);
        lineWidth.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                float val = lineWidth.getMappedValue();
                EventManager.publish(Event.LINE_WIDTH_CMD, lineWidth, val);
                return true;
            }
            return false;
        });

        /* Elevation multiplier */
        elevMult = new OwnSliderPlus(I18n.msg("gui.elevation.multiplier"), Constants.MIN_ELEVATION_MULT, Constants.MAX_ELEVATION_MULT, Constants.SLIDER_STEP_TINY, false, skin);
        elevMult.setWidth(contentWidth);
        elevMult.setValue((float) MathUtilsDouble.roundAvoid(Settings.settings.scene.renderer.elevation.multiplier, 1));
        elevMult.addListener(event -> {
            if (event instanceof ChangeEvent) {
                float val = elevMult.getValue();
                EventManager.publish(Event.ELEVATION_MULTIPLIER_CMD, elevMult, val);
                return true;
            }
            return false;
        });

        /* Reset defaults */
        OwnTextIconButton resetDefaults = new OwnTextIconButton(I18n.msg("gui.resetdefaults"), skin, "reset");
        resetDefaults.align(Align.center);
        resetDefaults.setWidth(contentWidth);
        resetDefaults.addListener(new OwnTextTooltip(I18n.msg("gui.resetdefaults.tooltip"), skin));
        resetDefaults.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Read defaults from internal settings file
                try {
                    Path confFolder = Settings.assetsPath("conf");
                    Path internalFolderConfFile = confFolder.resolve(SettingsManager.getConfigFileName(Settings.settings.runtime.openXr));
                    Yaml yaml = new Yaml();
                    Map<Object, Object> conf = yaml.load(Files.newInputStream(internalFolderConfFile));

                    float br = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("brightness")).floatValue();
                    float pow = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("power")).floatValue();
                    float glo = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("glowFactor")).floatValue();
                    float ss = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("pointSize")).floatValue();
                    float pam = (((java.util.List<Double>) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("opacity")).get(0)).floatValue();
                    float amb = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("renderer")).get("ambient")).floatValue();
                    float ls = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("label")).get("size")).floatValue();
                    float lw = ((Double) ((Map<String, Object>) conf.get("scene")).get("lineWidth")).floatValue();
                    float em = ((Double) ((Map<String, Object>) ((Map<String, Object>) ((Map<Object, Object>) conf.get("scene")).get("renderer")).get("elevation")).get("multiplier")).floatValue();

                    // Events
                    EventManager m = EventManager.instance;
                    m.post(Event.STAR_BRIGHTNESS_CMD, resetDefaults, br);
                    m.post(Event.STAR_BRIGHTNESS_POW_CMD, resetDefaults, pow);
                    m.post(Event.STAR_GLOW_FACTOR_CMD, resetDefaults, glo);
                    m.post(Event.STAR_POINT_SIZE_CMD, resetDefaults, ss);
                    m.post(Event.STAR_BASE_LEVEL_CMD, resetDefaults, pam);
                    m.post(Event.AMBIENT_LIGHT_CMD, resetDefaults, amb);
                    m.post(Event.LABEL_SIZE_CMD, resetDefaults, ls);
                    m.post(Event.LINE_WIDTH_CMD, resetDefaults, lw);
                    m.post(Event.ELEVATION_MULTIPLIER_CMD, resetDefaults, em);

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
        lightingGroup.addActor(magnitudeMultiplier);
        lightingGroup.addActor(starGlowFactor);
        lightingGroup.addActor(pointSize);
        lightingGroup.addActor(starBaseLevel);
        lightingGroup.addActor(ambientLight);
        lightingGroup.addActor(lineWidth);
        lightingGroup.addActor(labelSize);
        lightingGroup.addActor(elevMult);
        lightingGroup.addActor(resetDefaults);

        component = lightingGroup;

        EventManager.instance.subscribe(this, Event.STAR_POINT_SIZE_CMD, Event.STAR_BRIGHTNESS_CMD, Event.STAR_BRIGHTNESS_POW_CMD, Event.STAR_GLOW_FACTOR_CMD, Event.STAR_BASE_LEVEL_CMD, Event.LABEL_SIZE_CMD, Event.LINE_WIDTH_CMD);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case STAR_POINT_SIZE_CMD -> {
            if (source != pointSize) {
                hackProgrammaticChangeEvents = false;
                float newSize = (float) data[0];
                pointSize.setMappedValue(newSize);
                hackProgrammaticChangeEvents = true;
            }
        }
        case STAR_BRIGHTNESS_CMD -> {
            if (source != starBrightness) {
                Float brightness = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                starBrightness.setMappedValue(brightness);
                hackProgrammaticChangeEvents = true;
            }
        }
        case STAR_BRIGHTNESS_POW_CMD -> {
            if (source != magnitudeMultiplier) {
                Float pow = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                magnitudeMultiplier.setMappedValue(pow);
                hackProgrammaticChangeEvents = true;
            }
        }
        case STAR_GLOW_FACTOR_CMD -> {
            if (source != starGlowFactor) {
                Float glowFactor = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                starGlowFactor.setMappedValue(glowFactor);
                hackProgrammaticChangeEvents = true;
            }
        }
        case STAR_BASE_LEVEL_CMD -> {
            if (source != starBaseLevel) {
                Float baseLevel = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                starBaseLevel.setMappedValue(baseLevel);
                hackProgrammaticChangeEvents = true;
            }
        }
        case LABEL_SIZE_CMD -> {
            if (source != labelSize) {
                Float newLabelSize = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                labelSize.setMappedValue(newLabelSize);
                hackProgrammaticChangeEvents = true;
            }
        }
        case LINE_WIDTH_CMD -> {
            if (source != lineWidth) {
                Float newWidth = (Float) data[0];
                hackProgrammaticChangeEvents = false;
                lineWidth.setMappedValue(newWidth);
                hackProgrammaticChangeEvents = true;
            }
        }
        default -> {
        }

        }
    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
