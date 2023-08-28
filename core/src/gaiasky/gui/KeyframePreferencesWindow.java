/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.camera.rec.KeyframesManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnSelectBox;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.DoubleValidator;

import java.text.DecimalFormat;

public class KeyframePreferencesWindow extends GenericDialog {

    private final DecimalFormat nf3;
    public OwnTextField camrecFps;
    private OwnSelectBox<ComboBoxBean> posMethod;

    public KeyframePreferencesWindow(Stage stage, Skin skin) {
        super(I18n.msg("gui.keyframes.preferences"), skin, stage);
        setModal(true);
        this.nf3 = new DecimalFormat("0.000");

        setAcceptText(I18n.msg("gui.saveprefs"));
        setCancelText(I18n.msg("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {

        ComboBoxBean[] interpolation = new ComboBoxBean[] { new ComboBoxBean(I18n.msg("gui.interpolation.linear"), KeyframesManager.PathType.LINEAR.ordinal()), new ComboBoxBean(I18n.msg("gui.interpolation.catmull"), KeyframesManager.PathType.SPLINE.ordinal()) };

        OwnLabel generalTitle = new OwnLabel(I18n.msg("gui.general"), skin, "hud-header");

        // fps
        OwnLabel camfpsLabel = new OwnLabel(I18n.msg("gui.target.fps"), skin);
        camrecFps = new OwnTextField(nf3.format(Settings.settings.camrecorder.targetFps), skin, new DoubleValidator(Constants.MIN_FPS, Constants.MAX_FPS));
        camrecFps.setWidth(240f);

        OwnLabel interpTitle = new OwnLabel(I18n.msg("gui.keyframes.interp"), skin, "hud-header");

        // Camera position
        OwnLabel pos = new OwnLabel(I18n.msg("gui.cam.pos"), skin);
        posMethod = new OwnSelectBox<>(skin);
        posMethod.setItems(interpolation);
        posMethod.setSelectedIndex(Settings.settings.camrecorder.keyframe.position.ordinal());
        posMethod.setItems(interpolation);
        posMethod.setWidth(240f);

        // Camera orientation
        OwnLabel orientation = new OwnLabel(I18n.msg("gui.cam.orientation"), skin);
        OwnLabel orientationMethod = new OwnLabel("Slerp", skin);

        // Time
        OwnLabel time = new OwnLabel(I18n.msg("gui.time"), skin);
        OwnLabel timeMethod = new OwnLabel(I18n.msg("gui.interpolation.linear"), skin);

        content.add(generalTitle).left().top().colspan(2).padBottom(pad18).row();

        content.add(camfpsLabel).left().padRight(pad18).padBottom(pad18 * 3f);
        content.add(camrecFps).left().padBottom(pad18 * 3f).row();

        content.add(interpTitle).left().top().colspan(2).padBottom(pad18).row();

        content.add(pos).left().padRight(pad18).padBottom(pad18);
        content.add(posMethod).left().padBottom(pad18).row();

        content.add(orientation).left().padRight(pad18).padBottom(pad18);
        content.add(orientationMethod).left().padBottom(pad18).row();

        content.add(time).left().padRight(pad18).padBottom(pad18 * 3f);
        content.add(timeMethod).left().padBottom(pad18 * 3f).row();

    }

    @Override
    protected boolean accept() {
        EventManager.publish(Event.CAMRECORDER_FPS_CMD, this, Parser.parseDouble(camrecFps.getText()));
        Settings.settings.camrecorder.keyframe.position = KeyframesManager.PathType.values()[posMethod.getSelectedIndex()];
        return true;
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }
}
