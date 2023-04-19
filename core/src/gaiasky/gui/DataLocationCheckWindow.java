/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnLabel;

public class DataLocationCheckWindow extends GenericDialog {
    public DataLocationCheckWindow(String title, Skin skin, Stage stage) {
        super(title, skin, stage);

        setAcceptText(I18n.msg("gui.dscheck.yes"));
        setAcceptButtonStyle("huge");
        setCancelText(I18n.msg("gui.dscheck.no"));

        buildSuper();

        setCancelButtonColors(Color.RED, Color.RED);

    }

    @Override
    protected void build() {

        content.clear();

        content.add(new OwnLabel(I18n.msg("gui.dscheck.3"), skin, "header")).left().pad(pad18).row();
        content.add(new OwnLabel(I18n.msg("gui.dscheck.1", Settings.settings.version.version), skin, "huge")).left().pad(pad18).row();
        content.add(new OwnLabel(I18n.msg("gui.dscheck.2"), skin, "huge", 85)).left().pad(pad18).padBottom(pad34).row();

        String location = Settings.settings.data.location;
        content.add(new Link(location, skin.get("link-large", LabelStyle.class), "file://" + location)).center().pad(pad18).row();
        content.pack();

        pack();
    }

    @Override
    protected boolean accept() {
        return true;
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {
    }
}
