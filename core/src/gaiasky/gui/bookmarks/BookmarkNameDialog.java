/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.bookmarks;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.window.GenericDialog;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.LengthValidator;
import gaiasky.util.validator.RegexpValidator;

public class BookmarkNameDialog extends GenericDialog {
    private static int SEQ_NUM = 0;

    private OwnTextField bookmarkName;

    public BookmarkNameDialog(Stage stage, Skin skin) {
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

        var val = new RegexpValidator(new LengthValidator(1, 30), "[^,\\|\\\\]+");
        bookmarkName.setValidator(val);

        content.add(new OwnLabel(I18n.msg("gui.bookmark.name"), skin)).pad(pad, pad, 0, pad * 2).right();
        content.add(bookmarkName).pad(pad, 0, 0, pad);
        content.row();
    }

    public void resetName() {
        this.bookmarkName.setText("bookmark_" + (++SEQ_NUM));
    }

    @Override
    protected boolean accept() {
        var cam = GaiaSky.instance.getICamera();
        EventManager.publish(Event.BOOKMARKS_ADD, this, cam.getPos(), cam.getDirection(), cam.getUp(), GaiaSky.instance.time.getTime(), bookmarkName.getText(), false);
        return true;
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }
}
