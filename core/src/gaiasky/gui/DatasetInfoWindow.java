/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.scene.Mapper;
import gaiasky.util.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector2d;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextArea;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.FloatValidator;
import gaiasky.util.validator.IValidator;
import gaiasky.util.validator.TextFieldComparatorValidator;

/**
 * Dataset preferences dialog.
 */
public class DatasetInfoWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DatasetInfoWindow.class);

    private final CatalogInfo ci;
    private OwnTextField highlightSizeFactor, fadeInMin, fadeInMax, fadeOutMin, fadeOutMax;
    private OwnCheckBox allVisible, fadeIn, fadeOut;

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
        // Desc.
        String descriptionString = ci.description != null ? ci.description : ci.name;
        OwnTextArea descriptionTextArea = new OwnTextArea(descriptionString, skin, "no-disabled");
        descriptionTextArea.setWidth(500f);
        descriptionTextArea.setHeight(200f);
        descriptionTextArea.setDisabled(true);
        content.add(new OwnLabel(I18n.msg("gui.dataset.description"), skin, "hud-subheader")).top().right().padRight(pad18).padBottom(pad18 * 2f);
        content.add(descriptionTextArea).top().left().padRight(pad18).padBottom(pad18 * 2f).row();

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
