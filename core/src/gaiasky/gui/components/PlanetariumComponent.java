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
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.util.Constants;
import gaiasky.util.TextUtils;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnSelectBox;
import gaiasky.util.scene2d.OwnSliderReset;

/**
 * Component that appears in panorama mode and is used to control some of its parameters.
 */
public class PlanetariumComponent extends CubemapComponent {

    private OwnSelectBox<ComboBoxBean<CubemapProjection>> projectionBox;
    private OwnLabel warpNotice;
    private OwnSliderReset angle, aperture;

    public PlanetariumComponent(Skin skin,
                                Stage stage) {
        super(skin, stage, "planetarium");
        widthMultiplier = 1.1f;
    }


    @Override
    protected void setEnumOffset() {
        enumOffset = CubemapProjection.AZIMUTHAL_EQUIDISTANT.ordinal();
    }

    @Override
    public void initializeComponent(float componentWidth,
                                    CubemapProjection projection) {
        var modeCubemap = GaiaSky.settings().program.modeCubemap;
        var warp = modeCubemap.planetarium.sphericalMirrorWarp;

        // Projection box
        projectionBox = new OwnSelectBox<>(skin);

        var values = ComboBoxBean.getValues(CubemapProjection.getPlanetariumProjections());
        projectionBox.setItems(values);
        projectionBox.setWidth(componentWidth);
        if (projection.isPlanetarium())
            projectionBox.setSelectedIndex(projection.ordinal() - enumOffset);
        projectionBox.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                EventManager.publish(Event.PLANETARIUM_PROJECTION_CMD, projectionBox, projectionBox.getSelected().value);
            }
            return false;
        });

        if (warp == null) {
            projectionBox.setDisabled(true);

            // Notice
            var text = I18n.msg("gui.planetarium.sphericalmirror.nowarpfile");
            text = TextUtils.breakCharacters(text, 30);
            warpNotice = new OwnLabel(text, skin);
            warpNotice.setColor(ColorUtils.gGreenC);
        }


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
        if (warpNotice != null) {
            t.add(warpNotice).left().padBottom(pad20).row();
        }
        var label = new OwnLabel(I18n.msg("gui.360.notice.projection"), skin);
        t.add(label).left().padBottom(pad9).row();
        t.add(projectionBox).left().padBottom(pad30).row();
        t.add(aperture).left().padBottom(pad9).row();
        t.add(angle).left().padBottom(pad30).row();
    }
}
