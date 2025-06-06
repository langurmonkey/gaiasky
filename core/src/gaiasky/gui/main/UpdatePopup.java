/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.gui.window.GenericDialog;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnLabel;

/**
 * New version notification pop-up. Shows up whenever a new version of Gaia Sky is available.
 */
public class UpdatePopup extends GenericDialog {
    private final String tagVersion;

    public UpdatePopup(String tagVersion, Stage ui, Skin skin) {
        super(I18n.msg("gui.newversion.new"), skin, ui);

        this.tagVersion = tagVersion;

        setAcceptText(I18n.msg("gui.close"));
        setCancelText(null);

        buildSuper();
        setModal(false);
    }

    @Override
    protected void build() {
        float padb = 8f;
        content.clear();
        content.pad(16f);
        content.add(new OwnLabel(I18n.msg("gui.newversion.new.current") + ":", skin)).left().padRight(padb).padBottom(padb);
        content.add(new OwnLabel(Settings.settings.version.version, skin)).left().padBottom(padb).row();

        content.add(new OwnLabel(I18n.msg("gui.newversion.new.new") + ":", skin)).left().padRight(padb).padBottom(padb * 2);
        content.add(new OwnLabel(tagVersion, skin, "header")).left().padBottom(padb * 2).row();

        Label.LabelStyle linkStyle = skin.get("link", Label.LabelStyle.class);
        content.add(new Link(I18n.msg("gui.newversion.getit"), linkStyle, Settings.HOMEPAGE_DOWNLOADS)).center().colspan(2);
    }

    @Override
    protected boolean accept() {
        // Do nothing
        return true;
    }

    @Override
    protected void cancel() {
        // Do nothing
    }

    @Override
    public void dispose() {

    }
}
