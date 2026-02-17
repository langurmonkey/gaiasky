/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
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
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SettingsManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.scene2d.OwnSliderReset;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

public class VisualSettingsComponent extends GuiComponent implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(VisualSettingsComponent.class);

    public VisualSettingsComponent(Skin skin,
                                   Stage stage) {
        super(skin, stage);
    }

    @Override
    public void initialize(float componentWidth) {
        /* Star brightness */
        var starBrightness = new OwnSliderReset(I18n.msg("gui.star.brightness"),
                                                           Constants.MIN_SLIDER,
                                                           Constants.MAX_SLIDER,
                                                           Constants.SLIDER_STEP_TINY,
                                                           Constants.MIN_STAR_BRIGHTNESS,
                                                           Constants.MAX_STAR_BRIGHTNESS,
                                                           2.22f,
                                                           skin);
        starBrightness.setTooltip(I18n.msg("gui.star.brightness.info"));
        starBrightness.setWidth(componentWidth);
        starBrightness.setMappedValue(Settings.settings.scene.star.brightness);
        starBrightness.connect(Event.STAR_BRIGHTNESS_CMD);

        /* Star brightness power */
        var magnitudeMultiplier = new OwnSliderReset(I18n.msg("gui.star.brightness.pow"),
                                                                Constants.MIN_STAR_BRIGHTNESS_POW,
                                                                Constants.MAX_STAR_BRIGHTNESS_POW,
                                                                Constants.SLIDER_STEP_WEENY,
                                                                1f,
                                                                skin);
        magnitudeMultiplier.setTooltip(I18n.msg("gui.star.brightness.pow.info"));
        magnitudeMultiplier.setWidth(componentWidth);
        magnitudeMultiplier.setValue(Settings.settings.scene.star.power);
        magnitudeMultiplier.connect(Event.STAR_BRIGHTNESS_POW_CMD);

        /* Star glow factor */
        var starGlowFactor = new OwnSliderReset(I18n.msg("gui.star.glowfactor"),
                                                           Constants.MIN_STAR_GLOW_FACTOR,
                                                           Constants.MAX_STAR_GLOW_FACTOR,
                                                           Constants.SLIDER_STEP_TINY * 0.1f,
                                                           0.075f,
                                                           skin);
        starGlowFactor.setTooltip(I18n.msg("gui.star.glowfactor.info"));
        starGlowFactor.setWidth(componentWidth);
        starGlowFactor.setMappedValue(Settings.settings.scene.star.glowFactor);
        starGlowFactor.connect(Event.STAR_GLOW_FACTOR_CMD);

        /* Point size */
        var pointSize = new OwnSliderReset(I18n.msg("gui.star.size"),
                                                      Constants.MIN_STAR_POINT_SIZE,
                                                      Constants.MAX_STAR_POINT_SIZE,
                                                      Constants.SLIDER_STEP_TINY,
                                                      3f,
                                                      skin);
        pointSize.setTooltip(I18n.msg("gui.star.size.info"));
        pointSize.setWidth(componentWidth);
        pointSize.setMappedValue(Settings.settings.scene.star.pointSize);
        pointSize.connect(Event.STAR_POINT_SIZE_CMD);

        /* Star min opacity */
        var starBaseLevel = new OwnSliderReset(I18n.msg("gui.star.opacity"),
                                                          Constants.MIN_STAR_MIN_OPACITY,
                                                          Constants.MAX_STAR_MIN_OPACITY,
                                                          Constants.SLIDER_STEP_TINY,
                                                          0f,
                                                          skin);
        starBaseLevel.setWidth(componentWidth);
        starBaseLevel.setMappedValue(Settings.settings.scene.star.opacity[0]);
        starBaseLevel.connect(Event.STAR_BASE_LEVEL_CMD);

        /* Ambient light */
        var ambientLight = new OwnSliderReset(I18n.msg("gui.light.ambient"),
                                                         Constants.MIN_AMBIENT_LIGHT,
                                                         Constants.MAX_AMBIENT_LIGHT,
                                                         Constants.SLIDER_STEP_TINY,
                                                         0f,
                                                         skin);
        ambientLight.setWidth(componentWidth);
        ambientLight.setMappedValue(Settings.settings.scene.renderer.ambient);
        ambientLight.connect(Event.AMBIENT_LIGHT_CMD);

        /* Label size */
        var labelSize = new OwnSliderReset(I18n.msg("gui.label.size"),
                                                      Constants.MIN_LABEL_SIZE,
                                                      Constants.MAX_LABEL_SIZE,
                                                      Constants.SLIDER_STEP_TINY,
                                                      1.3f,
                                                      skin);
        labelSize.setWidth(componentWidth);
        labelSize.setMappedValue(Settings.settings.scene.label.size);
        labelSize.connect(Event.LABEL_SIZE_CMD);

        /* Line width */
        var lineWidth = new OwnSliderReset(I18n.msg("gui.line.width"),
                                                      Constants.MIN_LINE_WIDTH,
                                                      Constants.MAX_LINE_WIDTH,
                                                      Constants.SLIDER_STEP_TINY,
                                                      Constants.MIN_LINE_WIDTH,
                                                      Constants.MAX_LINE_WIDTH,
                                                      1f,
                                                      skin);
        lineWidth.setWidth(componentWidth);
        lineWidth.setMappedValue(Settings.settings.scene.renderer.line.width);
        lineWidth.connect(Event.LINE_WIDTH_CMD);

        /* Elevation multiplier */
        var elevMult = new OwnSliderReset(I18n.msg("gui.elevation.multiplier"),
                                                     Constants.MIN_ELEVATION_MULT,
                                                     Constants.MAX_ELEVATION_MULT,
                                                     Constants.SLIDER_STEP_TINY,
                                                     1f,
                                                     skin);
        elevMult.setWidth(componentWidth);
        elevMult.setValue((float) MathUtilsDouble.roundAvoid(Settings.settings.scene.renderer.elevation.multiplier, 1));
        elevMult.connect(Event.ELEVATION_MULTIPLIER_CMD);

        /* Reset visual settings defaults */
        var resetDefaults = new OwnTextIconButton(I18n.msg("gui.resetdefaults"), skin, "reset");
        resetDefaults.align(Align.center);
        resetDefaults.setWidth(componentWidth);
        resetDefaults.addListener(new OwnTextTooltip(I18n.msg("gui.resetdefaults.tooltip"), skin));
        resetDefaults.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Read defaults from internal settings file.
                EventManager.publish(Event.RESET_VISUAL_SETTINGS_DEFAULTS, resetDefaults);
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

        EventManager.instance.subscribe(this,
                                        Event.RESET_VISUAL_SETTINGS_DEFAULTS);
    }

    private void resetVisualSettingsDefaults(Object source) {
        try {
            Path confFolder = Settings.assetsPath("conf");
            Path internalFolderConfFile = confFolder.resolve(SettingsManager.getConfigFileName(Settings.settings.runtime.openXr));
            Yaml yaml = new Yaml();
            Map<Object, Object> conf = yaml.load(Files.newInputStream(internalFolderConfFile));

            float br = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("brightness")).floatValue();
            float pow = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("power")).floatValue();
            float glo = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("glowFactor")).floatValue();
            float ss = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("pointSize")).floatValue();
            float pam = (((java.util.List<Double>) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("star")).get("opacity")).getFirst(
            )).floatValue();
            float amb = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("renderer")).get("ambient")).floatValue();
            float ls = ((Double) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("label")).get("size")).floatValue();
            float lw = ((Double) ((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) conf.get("scene")).get("renderer")).get("line")).get(
                    "width")).floatValue();
            float em = ((Double) ((Map<String, Object>) ((Map<String, Object>) ((Map<Object, Object>) conf.get("scene")).get("renderer")).get(
                    "elevation")).get(
                    "multiplier")).floatValue();

            // Post events to reset all.
            EventManager m = EventManager.instance;
            m.post(Event.STAR_BRIGHTNESS_CMD, source, br);
            m.post(Event.STAR_BRIGHTNESS_POW_CMD, source, pow);
            m.post(Event.STAR_GLOW_FACTOR_CMD, source, glo);
            m.post(Event.STAR_POINT_SIZE_CMD, source, ss);
            m.post(Event.STAR_BASE_LEVEL_CMD, source, pam);
            m.post(Event.AMBIENT_LIGHT_CMD, source, amb);
            m.post(Event.LABEL_SIZE_CMD, source, ls);
            m.post(Event.LINE_WIDTH_CMD, source, lw);
            m.post(Event.ELEVATION_MULTIPLIER_CMD, source, em);

        } catch (IOException e) {
            logger.error(e, "Error loading default configuration file");
        }
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        if (Objects.requireNonNull(event) == Event.RESET_VISUAL_SETTINGS_DEFAULTS) {
            resetVisualSettingsDefaults(source);
        }
    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
