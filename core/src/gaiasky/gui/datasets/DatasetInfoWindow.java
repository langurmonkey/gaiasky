/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.datasets;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.gui.window.GenericDialog;
import gaiasky.util.CatalogInfo;
import gaiasky.util.GlobalResources;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

/**
 * A window that displays information on a particular dataset.
 */
public class DatasetInfoWindow extends GenericDialog {
    private final CatalogInfo ci;

    public DatasetInfoWindow(CatalogInfo ci,
                             Skin skin,
                             Stage stage) {
        super(I18n.msg("gui.dataset.info") + " - " + ci.name, skin, stage);
        this.ci = ci;

        setAcceptText(I18n.msg("gui.ok"));
        setModal(false);

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        // Name.
        content.add(new OwnLabel(I18n.msg("gui.dataset.name"), skin, "hud-subheader")).top().right().padRight(pad18).padBottom(pad18);
        content.add(new OwnLabel(ci.name, skin)).top().left().padRight(pad18).padBottom(pad18).row();
        // Source.
        content.add(new OwnLabel(I18n.msg("gui.dataset.source"), skin, "hud-subheader")).top().right().padRight(pad18).padBottom(pad18);
        content.add(new OwnLabel(ci.source != null ? ci.source : "-", skin)).top().left().padRight(pad18).padBottom(pad18).row();
        // Type.
        content.add(new OwnLabel(I18n.msg("gui.dataset.type"), skin, "hud-subheader")).top().right().padRight(pad18).padBottom(pad18);
        content.add(new OwnLabel(ci.type.toString(), skin)).top().left().padRight(pad18).padBottom(pad18).row();
        // Num objects.
        content.add(new OwnLabel(I18n.msg("gui.dataset.numobjects"), skin, "hud-subheader")).top().right().padRight(pad18).padBottom(pad18);
        content.add(new OwnLabel(ci.nParticles > 0 ? Long.toString(ci.nParticles) : "?", skin)).top().left().padRight(pad18).padBottom(pad18).row();
        // Size bytes.
        content.add(new OwnLabel(I18n.msg("gui.dataset.sizebytes"), skin, "hud-subheader")).top().right().padRight(pad18).padBottom(pad18);
        content.add(new OwnLabel(ci.sizeBytes > 0 ? GlobalResources.humanReadableByteCount(ci.sizeBytes, true) : "?", skin)).top().left().padRight(pad18).padBottom(pad18).row();
        // Added
        content.add(new OwnLabel(I18n.msg("gui.dataset.loaded"), skin, "hud-subheader")).top().right().padRight(pad18).padBottom(pad18);
        content.add(new OwnLabel(ci.loadDateUTC.atZone(Settings.settings.program.timeZone.getTimeZone()).toString(), skin)).top().left().padRight(pad18).padBottom(pad18).row();
        // Description
        final OwnScrollPane scroll = getOwnScrollPane();
        content.add(new OwnLabel(I18n.msg("gui.dataset.description"), skin, "hud-subheader")).top().right().padRight(pad18).padBottom(pad18 * 2f);
        content.add(scroll).top().left().padRight(pad18).padBottom(pad18 * 2f).row();

    }

    private OwnScrollPane getOwnScrollPane() {
        String descriptionString = ci.description != null ? ci.description : ci.name;
        OwnTextArea descriptionTextArea = new OwnTextArea(descriptionString, skin, "no-disabled");
        descriptionTextArea.setWidth(600f);
        descriptionTextArea.setDisabled(true);
        float fontHeight = descriptionTextArea.getStyle().font.getLineHeight();
        descriptionTextArea.offsets();
        descriptionTextArea.setHeight((descriptionTextArea.getLines() + 3) * fontHeight);
        descriptionTextArea.clearListeners();
        OwnScrollPane scroll = new OwnScrollPane(descriptionTextArea, skin, "default-nobg");
        scroll.setWidth(640f);
        scroll.setHeight(300f);
        return scroll;
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
