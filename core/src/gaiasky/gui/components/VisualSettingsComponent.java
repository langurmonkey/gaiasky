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
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.scene2d.OwnSliderReset;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;

import java.util.Objects;

/**
 * GUI component that allows to change various visual settings in Gaia Sky, such as star brightness,
 * magnitude multiplier, glow factor, label sizes, and line widths.
 * <p>
 * This component provides sliders and buttons that interact with the {@link gaiasky.event.EventManager}
 * to publish commands to the rest of the application. It also supports resetting these values
 * to their defaults by reading from the internal configuration file.
 */
public class VisualSettingsComponent extends GuiComponent implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(VisualSettingsComponent.class);

    public VisualSettingsComponent(Skin skin,
                                   Stage stage) {
        super(skin, stage);
    }

    private final float STAR_BRIGHTNESS_DEFAULT = 2.22f;
    private final float STAR_BR_POWER_DEFAULT = 1f;
    private final float STAR_GLOW_FACTOR_DEFAULT = 0.035f;
    private final float POINT_SIZE_DEFAULT = 3f;
    private final float STAR_BASE_LEVEL_DEFAULT = 0.0f;
    private final float AMBIENT_LIGHT_DEFAULT = 0.0f;
    private final float LABEL_SIZE_DEFAULT = 1.3f;
    private final float LINE_WIDTH_DEFAULT = 1.0f;
    private final float ELEVATION_DEFAULT = 1.0f;


    @Override
    public void initialize(float componentWidth) {
        /* Star brightness */
        var starBrightness = new OwnSliderReset(I18n.msg("gui.star.brightness"),
                                                Constants.MIN_SLIDER,
                                                Constants.MAX_SLIDER,
                                                Constants.SLIDER_STEP_TINY,
                                                Constants.MIN_STAR_BRIGHTNESS,
                                                Constants.MAX_STAR_BRIGHTNESS,
                                                STAR_BRIGHTNESS_DEFAULT,
                                                skin);
        starBrightness.setTooltip(I18n.msg("gui.star.brightness.info"));
        starBrightness.setWidth(componentWidth);
        starBrightness.setDisplayValueMapped(false);
        starBrightness.setMappedValue(GaiaSky.settings().scene.star.brightness);
        starBrightness.connect(Event.STAR_BRIGHTNESS_CMD);

        /* Star brightness power */
        var starBrightnessPow = new OwnSliderReset(I18n.msg("gui.star.brightness.pow"),
                                                   Constants.MIN_STAR_BRIGHTNESS_POW,
                                                   Constants.MAX_STAR_BRIGHTNESS_POW,
                                                   Constants.SLIDER_STEP_WEENY,
                                                   STAR_BR_POWER_DEFAULT,
                                                   skin);
        starBrightnessPow.setTooltip(I18n.msg("gui.star.brightness.pow.info"));
        starBrightnessPow.setWidth(componentWidth);
        starBrightnessPow.setValue(GaiaSky.settings().scene.star.power);
        starBrightnessPow.connect(Event.STAR_BRIGHTNESS_POW_CMD);

        /* Star glow factor */
        var starGlowFactor = new OwnSliderReset(I18n.msg("gui.star.glowfactor"),
                                                Constants.MIN_STAR_GLOW_FACTOR,
                                                Constants.MAX_STAR_GLOW_FACTOR,
                                                Constants.SLIDER_STEP_WEENY,
                                                STAR_GLOW_FACTOR_DEFAULT,
                                                skin);
        starGlowFactor.setTooltip(I18n.msg("gui.star.glowfactor.info"));
        starGlowFactor.setWidth(componentWidth);
        starGlowFactor.setMappedValue(GaiaSky.settings().scene.star.glowFactor);
        starGlowFactor.connect(Event.STAR_GLOW_FACTOR_CMD);

        /* Point size */
        var pointSize = new OwnSliderReset(I18n.msg("gui.star.size"),
                                           Constants.MIN_STAR_POINT_SIZE,
                                           Constants.MAX_STAR_POINT_SIZE,
                                           Constants.SLIDER_STEP_TINY,
                                           POINT_SIZE_DEFAULT,
                                           skin);
        pointSize.setTooltip(I18n.msg("gui.star.size.info"));
        pointSize.setWidth(componentWidth);
        pointSize.setMappedValue(GaiaSky.settings().scene.star.pointSize);
        pointSize.connect(Event.STAR_POINT_SIZE_CMD);

        /* Star min opacity */
        var starBaseLevel = new OwnSliderReset(I18n.msg("gui.star.opacity"),
                                               Constants.MIN_STAR_MIN_OPACITY,
                                               Constants.MAX_STAR_MIN_OPACITY,
                                               Constants.SLIDER_STEP_TINY,
                                               STAR_BASE_LEVEL_DEFAULT,
                                               skin);
        starBaseLevel.setTooltip(I18n.msg("gui.star.opacity.info"));
        starBaseLevel.setWidth(componentWidth);
        starBaseLevel.setMappedValue(GaiaSky.settings().scene.star.opacity[0]);
        starBaseLevel.connect(Event.STAR_BASE_LEVEL_CMD);

        /* Ambient light */
        var ambientLight = new OwnSliderReset(I18n.msg("gui.light.ambient"),
                                              Constants.MIN_AMBIENT_LIGHT,
                                              Constants.MAX_AMBIENT_LIGHT,
                                              Constants.SLIDER_STEP_TINY,
                                              AMBIENT_LIGHT_DEFAULT,
                                              skin);
        ambientLight.setWidth(componentWidth);
        ambientLight.setMappedValue(GaiaSky.settings().scene.renderer.ambient);
        ambientLight.connect(Event.AMBIENT_LIGHT_CMD);

        /* Label size */
        var labelSize = new OwnSliderReset(I18n.msg("gui.label.size"),
                                           Constants.MIN_LABEL_SIZE,
                                           Constants.MAX_LABEL_SIZE,
                                           Constants.SLIDER_STEP_TINY,
                                           LABEL_SIZE_DEFAULT,
                                           skin);
        labelSize.setWidth(componentWidth);
        labelSize.setMappedValue(GaiaSky.settings().scene.label.size);
        labelSize.connect(Event.LABEL_SIZE_CMD);

        /* Line width */
        var lineWidth = new OwnSliderReset(I18n.msg("gui.line.width"),
                                           Constants.MIN_LINE_WIDTH,
                                           Constants.MAX_LINE_WIDTH,
                                           Constants.SLIDER_STEP_TINY,
                                           Constants.MIN_LINE_WIDTH,
                                           Constants.MAX_LINE_WIDTH,
                                           LINE_WIDTH_DEFAULT,
                                           skin);
        lineWidth.setWidth(componentWidth);
        lineWidth.setMappedValue(GaiaSky.settings().scene.renderer.line.width);
        lineWidth.connect(Event.LINE_WIDTH_CMD);

        /* Elevation multiplier */
        var elevMult = new OwnSliderReset(I18n.msg("gui.elevation.multiplier"),
                                          Constants.MIN_ELEVATION_MULT,
                                          Constants.MAX_ELEVATION_MULT,
                                          Constants.SLIDER_STEP_TINY,
                                          ELEVATION_DEFAULT,
                                          skin);
        elevMult.setWidth(componentWidth);
        elevMult.setValue((float) MathUtilsDouble.roundAvoid(GaiaSky.settings().scene.renderer.elevation.multiplier, 1));
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
        lightingGroup.addActor(starBrightnessPow);
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
        // Post events to reset all.
        EventManager m = EventManager.instance;
        m.post(Event.STAR_BRIGHTNESS_CMD, source, STAR_BRIGHTNESS_DEFAULT);
        m.post(Event.STAR_BRIGHTNESS_POW_CMD, source, STAR_BR_POWER_DEFAULT);
        m.post(Event.STAR_GLOW_FACTOR_CMD, source, STAR_GLOW_FACTOR_DEFAULT);
        m.post(Event.STAR_POINT_SIZE_CMD, source, POINT_SIZE_DEFAULT);
        m.post(Event.STAR_BASE_LEVEL_CMD, source, STAR_BASE_LEVEL_DEFAULT);
        m.post(Event.AMBIENT_LIGHT_CMD, source, AMBIENT_LIGHT_DEFAULT);
        m.post(Event.LABEL_SIZE_CMD, source, LABEL_SIZE_DEFAULT);
        m.post(Event.LINE_WIDTH_CMD, source, LINE_WIDTH_DEFAULT);
        m.post(Event.ELEVATION_MULTIPLIER_CMD, source, ELEVATION_DEFAULT);
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (Objects.requireNonNull(event) == Event.RESET_VISUAL_SETTINGS_DEFAULTS) {
            resetVisualSettingsDefaults(source);
        }
    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
