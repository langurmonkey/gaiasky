/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import gaiasky.GaiaSky;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnLabel;

public class QuitWindow extends GenericDialog {

    private OwnCheckBox doNotAsk;

    public QuitWindow(Stage ui, Skin skin) {
        super(I18n.msg("gui.quit.title"), skin, ui);

        setAcceptText(I18n.msg("gui.yes"));
        setCancelText(I18n.msg("gui.no"));

        buildSuper();
    }

    @Override
    protected void build() {
        content.clear();

        content.add(new OwnLabel(I18n.msg("gui.quit.sure"), skin)).left().padBottom(pad18 * 2f).row();

        doNotAsk = new OwnCheckBox(I18n.msg("gui.donotask"), skin, pad10);
        doNotAsk.setChecked(false);

        bottom.add(doNotAsk).right().row();

        var task = new Task() {
            @Override
            public void run() {
                stage.setKeyboardFocus(acceptButton);
            }
        };
        Timer.schedule(task, 0.2f);
    }

    @Override
    protected boolean accept() {
        // Update exit confirmation
        Settings.settings.program.exitConfirmation = !doNotAsk.isChecked();
        // Only run if it does not have an accept runnable already.
        // Otherwise, it comes from the exit hook.
        GaiaSky.postRunnable(() -> {
            // Exit GDX app.
            Gdx.app.exit();
        });
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
