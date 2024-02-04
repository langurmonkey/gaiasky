/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Gaia Sky main error dialog implementation. This is implemented as a standalone application that
 * displays an error to the user. Used when a very bad, unrecoverable crash happens.
 */
public class ErrorDialog implements ApplicationListener {

    private final Exception cause;
    private final String message;
    private Stage ui;

    public ErrorDialog(Exception cause, String message) {
        this.cause = cause;
        this.message = message;
    }

    @Override
    public void create() {
        ui = new Stage();
        FileHandle fh = Gdx.files.internal("skins/" + Settings.settings.program.ui.theme + "/" + Settings.settings.program.ui.theme + ".json");
        if (!fh.exists()) {
            // Default to dark-green
            Settings.settings.program.ui.theme = "dark-green";
            fh = Gdx.files.internal("skins/" + Settings.settings.program.ui.theme + "/" + Settings.settings.program.ui.theme + ".json");
        }
        Skin skin = new Skin(fh);

        Table t = new Table(skin);
        t.setFillParent(true);
        ui.addActor(t);

        // Title
        OwnLabel title = new OwnLabel(I18n.msg("error.crash.title"), skin, "header-large");
        // Subtitle
        String msg;
        if(cause != null) {
            if (cause.getLocalizedMessage() != null) {
                msg = cause.getLocalizedMessage();
            } else if (cause.getMessage() != null){
                msg = cause.getMessage();
            } else {
                msg = cause.getClass().getSimpleName();
            }
        } else {
            msg = "-";
        }
        OwnLabel subtitle = new OwnLabel(I18n.msg("notif.error", TextUtils.breakCharacters(msg, 40)), skin, "default");
        // Notification
        OwnLabel urlLabel = new OwnLabel(I18n.msg("error.crash.exception.1"), skin, "header-s");
        Link url = new Link(Settings.REPO_ISSUES, skin, Settings.REPO_ISSUES);
        OwnLabel applicationMessageLabel = new OwnLabel(I18n.msg("error.crash.applicationmessage"), skin, "header-s");
        OwnLabel applicationMessage = new OwnLabel(message != null ? message : "-", skin, "default");

        // Stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (cause != null && cause.getCause() != null) {
            cause.getCause().printStackTrace(pw);
        }
        String stackStr = sw.toString();
        long lines = TextUtils.countLines(stackStr);
        OwnTextArea stackTraceTextArea = new OwnTextArea(stackStr, skin, "default");
        stackTraceTextArea.setPrefRows(15);
        stackTraceTextArea.setWidth(800);
        stackTraceTextArea.setHeight(lines * 35);

        OwnScrollPane stackTraceScroll = new OwnScrollPane(stackTraceTextArea, skin, "minimalist");
        stackTraceScroll.setWidth(820);
        stackTraceScroll.setHeight(400);
        stackTraceScroll.setFadeScrollBars(false);
        OwnLabel stackTraceLabel = new OwnLabel(I18n.msg("error.crash.stacktrace"), skin, "header-s");

        // Close button
        Button b = new OwnTextButton(I18n.msg("gui.close"), skin, "default");
        b.setWidth(400);
        b.addListener((event) -> {
            if (event instanceof ChangeListener.ChangeEvent) {
                Gdx.app.postRunnable(() -> {
                    this.dispose();
                    Gdx.app.exit();
                    System.exit(1);
                });
            }
            return true;
        });
        b.pad(10);

        // Add to table
        t.add(title).left().padBottom(10).row();
        t.add(subtitle).left().padBottom(30).row();

        t.add(urlLabel).left().padBottom(10).row();
        t.add(url).left().padBottom(30).row();

        t.add(applicationMessageLabel).left().padBottom(10).row();
        t.add(applicationMessage).left().padBottom(30).row();

        t.add(stackTraceLabel).left().padBottom(10).row();
        t.add(stackTraceScroll).left().padBottom(80).row();

        t.add(b).center().row();

        Gdx.input.setInputProcessor(ui);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        ui.act(Gdx.graphics.getDeltaTime());
        ui.draw();

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        ui.dispose();
    }

}
