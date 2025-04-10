/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.bookmarks;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.window.GenericDialog;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.scene2d.Separator;
import gaiasky.util.validator.FolderValidator;
import gaiasky.util.validator.IValidator;
import gaiasky.util.validator.LengthValidator;
import gaiasky.util.validator.StringValidator;

public class BookmarkNameDialog extends GenericDialog implements IObserver {
    private static int SEQ_NUM = 0;

    private OwnTextField bookmarkName;
    private OwnLabel errorText;
    private OwnCheckBox positionCb, orientationCb, timeCb, settingsCb, focusCb;

    public BookmarkNameDialog(Stage stage,
                              Skin skin) {
        super(I18n.msg("context.bookmark.pos"), skin, stage);

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));

        buildSuper();
    }

    @Override
    protected void build() {
        float inputWidth = 360f;
        float pad = 8f;

        bookmarkName = new OwnTextField("", skin);
        bookmarkName.setMaxLength(30);
        bookmarkName.setWidth(inputWidth);
        bookmarkName.addListener((event -> {
            if (event instanceof ChangeEvent) {
                check();
                return true;
            }
            return false;
        }));

        // Validator: folder, character, length.
        IValidator folderValidator = new FolderValidator();
        IValidator stringValidator = new StringValidator(folderValidator, new Character[]{'{', '}', '|', ','});
        var val = new LengthValidator(stringValidator, 1, 30);
        bookmarkName.setValidator(val);

        // Check boxes for bookmarked components.
        positionCb = new OwnCheckBox(I18n.msg("gui.bookmark.cb.position"), skin, pad10);
        positionCb.setChecked(true);
        positionCb.addListener((event -> {
            if (event instanceof ChangeEvent) {
                check();
                return true;
            }
            return false;
        }));
        orientationCb = new OwnCheckBox(I18n.msg("gui.bookmark.cb.orientation"), skin, pad10);
        orientationCb.setChecked(true);
        orientationCb.addListener((event -> {
            if (event instanceof ChangeEvent) {
                check();
                return true;
            }
            return false;
        }));
        timeCb = new OwnCheckBox(I18n.msg("gui.bookmark.cb.time"), skin, pad10);
        timeCb.setChecked(true);
        timeCb.addListener((event -> {
            if (event instanceof ChangeEvent) {
                check();
                return true;
            }
            return false;
        }));
        focusCb = new OwnCheckBox(I18n.msg("gui.bookmark.cb.focus"), skin, pad10);
        focusCb.setChecked(false);
        updateFocusCheckbox();
        focusCb.addListener((event -> {
            if (event instanceof ChangeEvent) {
                check();
                return true;
            }
            return false;
        }));
        settingsCb = new OwnCheckBox(I18n.msg("gui.bookmark.cb.settings"), skin, pad10);
        settingsCb.setChecked(false);
        settingsCb.addListener((event -> {
            if (event instanceof ChangeEvent) {
                check();
                return true;
            }
            return false;
        }));

        // Error text
        errorText = new OwnLabel("", skin, "default-red");

        content.add(new OwnLabel(I18n.msg("gui.bookmark.name"), skin)).left().padRight(pad).padBottom(pad34);
        content.add(bookmarkName).pad(pad, 0, pad20, pad).left().padBottom(pad34).row();

        content.add(new Separator(skin, "gray")).left().colspan(2).fillX().padBottom(pad).row();
        content.add(positionCb).pad(pad, 0, pad, pad).left().colspan(2).row();
        content.add(orientationCb).pad(pad, 0, pad, pad).left().colspan(2).row();
        content.add(timeCb).pad(pad, 0, pad, pad).left().colspan(2).row();
        content.add(focusCb).pad(pad, 0, pad, pad).left().colspan(2).row();
        content.add(settingsCb).pad(pad, 0, pad, pad).left().colspan(2).padBottom(pad20).row();

        content.add(errorText).center().colspan(2);

        EventManager.instance.subscribe(this, Event.CAMERA_MODE_CMD);
    }

    public void resetName() {
        this.bookmarkName.setText("bookmark_" + (++SEQ_NUM));
    }

    @Override
    protected boolean accept() {
        var cam = GaiaSky.instance.getICamera();
        var pos = positionCb.isChecked() ? cam.getPos() : null;
        var dir = orientationCb.isChecked() ? cam.getDirection() : null;
        var up = orientationCb.isChecked() ? cam.getUp() : null;
        var time = timeCb.isChecked() ? GaiaSky.instance.time.getTime() : null;
        var focus = focusCb.isChecked() ? GaiaSky.instance.getCameraManager().getFocus().getName() : null;
        var settings = settingsCb.isChecked() ? Settings.settings.clone() : null;

        if (check()) {
            EventManager.publish(Event.BOOKMARKS_ADD, this, pos, dir, up, time, focus, settings, bookmarkName.getText(), false);
            return true;
        }
        return false;
    }

    /**
     * Checks that the inputs are fine, and returns true if there are no errors.
     *
     * @return True if no errors are found.
     */
    private boolean check() {
        if (!bookmarkName.isValid()) {
            error(I18n.msg("gui.bookmark.error.name"));
            acceptButton.setDisabled(true);
            return false;
        }
        if (!positionCb.isChecked() && !orientationCb.isChecked() && !timeCb.isChecked() && !focusCb.isChecked() && !settingsCb.isChecked()) {
            error(I18n.msg("gui.bookmark.error.cb"));
            acceptButton.setDisabled(true);
            return false;
        }

        error(null);
        acceptButton.setDisabled(false);
        return true;
    }

    private void updateFocusCheckbox() {
        if (focusCb != null) {
            focusCb.setDisabled(!GaiaSky.instance.getCameraManager().getMode().isFocus());
        }
    }

    private void error(String info) {
        if (info == null) {
            errorText.setText("");
        } else {
            errorText.setText(info);
        }
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.CAMERA_MODE_CMD) {
            updateFocusCheckbox();
        }
    }
}
