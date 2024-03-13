/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.LengthValidator;
import gaiasky.util.validator.RegexpValidator;

/**
 * Generic dialog to pick a file name for a file.
 */
public class FileNameWindow extends GenericDialog {

    private final String defaultName;
    private OwnTextField fileName;

    public FileNameWindow(String defaultName, Stage stage, Skin skin) {
        super(I18n.msg("gui.filename.choose"), skin, stage);
        setModal(true);
        this.defaultName = defaultName;

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));

        buildSuper();

        pack();
    }

    @Override
    protected void build() {
        OwnLabel label = new OwnLabel(I18n.msg("gui.filename.filename") + ": ", skin);
        LengthValidator lengthValidator = new LengthValidator(3, 40);
        RegexpValidator nameValidator = new RegexpValidator(lengthValidator, "^[^*&%\\s\\+\\=\\\\\\/@#\\$&\\*()~]+$");
        fileName = new OwnTextField(defaultName, skin, nameValidator);
        fileName.setWidth(400f);

        content.add(label).padRight(pad18).padBottom(pad18);
        content.add(fileName).padBottom(pad18);
    }

    /**
     * Returns the file name text field
     *
     * @return The text field
     */
    public OwnTextField getFileNameField() {
        return fileName;
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
