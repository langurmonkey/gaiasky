/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.util.Constants;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnSliderReset;

/**
 * Component that appears in panorama mode and is used to control some of its parameters.
 */
public class PlanetariumComponent extends CubemapComponent {

    private OwnSliderReset angle, aperture;

    public PlanetariumComponent(Skin skin,
                                Stage stage) {
        super(skin, stage, "planetarium");
        widthMultiplier = 1.1f;
    }


    @Override
    public void initializeComponent(float componentWidth) {
        var modeCubemap = GaiaSky.settings().program.modeCubemap;
        aperture = new OwnSliderReset(I18n.msg("gui.planetarium.aperture"),
                                      Constants.MIN_PL_APERTURE,
                                      Constants.MAX_PL_APERTURE,
                                      Constants.SLIDER_STEP,
                                      180f,
                                      skin);
        aperture.setTooltip(I18n.msg("gui.planetarium.aperture"));
        aperture.setValueLabelTransform((value) -> String.format("%.1f°", value));
        aperture.setValue(modeCubemap.planetarium.aperture);
        aperture.connect(Event.PLANETARIUM_APERTURE_CMD);
        aperture.setWidth(componentWidth);

        // Skew angle
        angle = new OwnSliderReset(I18n.msg("gui.planetarium.angle"),
                                   Constants.MIN_PL_ZENITH_ANGLE,
                                   Constants.MAX_PL_ZENITH_ANGLE,
                                   Constants.SLIDER_STEP,
                                   50f,
                                   skin);
        angle.setTooltip(I18n.msg("gui.planetarium.angle"));
        angle.setValueLabelTransform((value) -> String.format("%.1f°", value));
        angle.setValue(modeCubemap.planetarium.angle);
        angle.connect(Event.PLANETARIUM_ANGLE_CMD);
        angle.setWidth(componentWidth);
    }

    @Override
    public void addToTable(Table t) {
        t.add(aperture).left().padBottom(pad9).row();
        t.add(angle).left().padBottom(pad20).row();
    }
}
