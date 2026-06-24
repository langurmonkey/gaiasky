/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.gui.main.KeyBindings;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.text.DecimalFormat;

/**
 * Component that appears in stereoscopic mode and is used to control some of its parameters.
 */
public class StereoComponent extends GuiComponent implements IObserver {

    protected OwnSelectBox<ComboBoxBean<Settings.StereoProfile>> profile;
    protected OwnSliderReset k, ipd, screenDistance;
    protected Button backButton;

    public StereoComponent(Skin skin,
                           Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Event.STEREO_PROFILE_CMD);
    }


    @Override
    public void initialize(float componentWidth) {
        KeyBindings kb = KeyBindings.instance;
        // Profile title
        var profileLabel = new OwnLabel(I18n.msg("gui.stereo.notice.profile"), skin);

        // Stereo profile
        profile = new OwnSelectBox<>(skin);

        profile.setItems(ComboBoxBean.getValues(Settings.StereoProfile.class));
        profile.setWidth(componentWidth);
        profile.setSelectedIndex(GaiaSky.settings().program.modeStereo.profile.ordinal());
        profile.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                EventManager.publish(Event.STEREO_PROFILE_CMD, profile, profile.getSelected().value);
            }
            return false;
        });

        // Comfort slider
        k = new OwnSliderReset(I18n.msg("gui.stereo.k.short"),
                               Constants.MIN_STEREO_K,
                               Constants.MAX_STEREO_K,
                               Constants.SLIDER_STEP_TINY,
                               0.2f,
                               skin);
        k.setName("stereo k");
        k.setWidth(componentWidth);
        k.setTooltip(I18n.msg("gui.stereo.k"));
        k.setValue((float) GaiaSky.settings().program.modeStereo.k);
        k.connect(Event.STEREO_K_CMD);

        // IPD
        ipd = new OwnSliderReset(I18n.msg("gui.stereo.ipd.short"),
                                 Constants.MIN_STEREO_IPD,
                                 Constants.MAX_STEREO_IPD,
                                 Constants.SLIDER_STEP_TINY,
                                 64f,
                                 skin);
        ipd.setTooltip(I18n.msg("gui.stereo.ipd"));
        ipd.setName("stereo ipd");
        ipd.setWidth(componentWidth);
        var nf = new DecimalFormat("###0");
        ipd.setValueLabelTransform((val) -> nf.format(val) + " " + I18n.msg("gui.unit.mm"));
        ipd.setValue((float) GaiaSky.settings().program.modeStereo.ipd);
        ipd.connect(Event.STEREO_IPD_CMD);

        // Screen dist
        screenDistance = new OwnSliderReset(I18n.msg("gui.stereo.screen"), Constants.MIN_STEREO_SD, Constants.MAX_STEREO_SD, true, 600f, skin);
        screenDistance.setTooltip(I18n.msg("gui.stereo.screen"));
        screenDistance.setLogarithmicExponent(3.0);
        screenDistance.setName("stereo screen dist");
        screenDistance.setWidth(componentWidth);
        screenDistance.setValueLabelTransform((val) -> nf.format(val) + " " + I18n.msg("gui.unit.mm"));
        screenDistance.setMappedValue((float) GaiaSky.settings().program.modeStereo.screenDistance);
        screenDistance.connect(Event.STEREO_SCREEN_DIST_CMD);

        // Go back button
        backButton = new OwnTextIconButton(I18n.msg("gui.stereo.notice.back"), skin, "back");
        backButton.setName("exit stereo mode");
        backButton.setWidth(componentWidth);
        backButton.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.stereo.notice.back"),
                                                        kb.getStringKeys("action.toggle/element.stereomode", true),
                                                        skin));
        backButton.addListener(event -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                EventManager.publish(Event.STEREOSCOPIC_CMD, this, !GaiaSky.settings().program.modeStereo.active);
            }
            return false;
        });

        /*
         * ADD TO CONTENT
         */
        Table t = new Table(skin);
        t.add(profileLabel).left().padBottom(pad9).row();
        t.add(profile).left().padBottom(pad20).row();
        t.add(k).left().padBottom(pad9).row();
        t.add(ipd).left().padBottom(pad9).row();
        t.add(screenDistance).left().padBottom(pad20).row();
        t.add(backButton).left();

        component = t;
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (event == Event.STEREO_PROFILE_CMD) {
            Settings.StereoProfile newProfile = (Settings.StereoProfile) data[0];

            if (source != profile) {
                profile.setProgrammaticChangeEvents(false);
                profile.setSelectedIndex(newProfile.ordinal());
                profile.setProgrammaticChangeEvents(true);
            }
        }
    }

    @Override
    public void dispose() {

    }
}
