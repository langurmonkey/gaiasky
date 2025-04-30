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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

/**
 * Gaia Sky main error dialog implementation. This is implemented as a standalone application that
 * displays an error to the user. Used when a very bad, unrecoverable crash happens.
 */
public class ErrorDialog implements ApplicationListener {

    private final Exception cause;
    private Stage ui;
    private ScreenViewport vp;
    private SpriteBatch sb;
    private Skin skin;

    public ErrorDialog(Exception cause) {
        this.cause = cause;
    }

    @Override
    public void create() {
        var height = Gdx.graphics.getHeight();
        var unitsPerPixel = 1f / ((float) height / 1000f) * Settings.settings.program.ui.scale;
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        this.vp = vp;
        this.sb = initializeSpriteBatch();
        ui = new Stage(this.vp, this.sb);
        FileHandle fh = Gdx.files.internal("skins/" + Settings.settings.program.ui.theme + "/" + Settings.settings.program.ui.theme + ".json");
        if (!fh.exists()) {
            // Default to dark-green
            Settings.settings.program.ui.theme = "dark-green";
            fh = Gdx.files.internal("skins/" + Settings.settings.program.ui.theme + "/" + Settings.settings.program.ui.theme + ".json");
        }
        skin = new Skin(fh);
        // Linear filtering.
        ObjectMap<String, BitmapFont> fonts = skin.getAll(BitmapFont.class);
        for (String key : fonts.keys()) {
            fonts.get(key)
                    .getRegion()
                    .getTexture()
                    .setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        rebuildUI();

        Gdx.input.setInputProcessor(ui);
    }

    private void rebuildUI() {

        Table t = new Table(skin);
        t.setFillParent(true);
        ui.clear();
        ui.addActor(t);

        // Title
        OwnLabel title = new OwnLabel(I18n.msg("error.crash.title"), skin, "header-large");
        // Subtitle
        String msg;
        if (cause != null) {
            if (cause.getLocalizedMessage() != null) {
                msg = cause.getLocalizedMessage();
            } else if (cause.getMessage() != null) {
                msg = cause.getMessage();
            } else {
                msg = cause.getClass()
                        .getSimpleName();
            }
        } else {
            msg = "-";
        }
        OwnLabel subtitle = new OwnLabel(I18n.msg("notif.error", TextUtils.breakCharacters(msg, 40)), skin, "default");
        // Notification
        OwnLabel urlLabel = new OwnLabel(I18n.msg("error.crash.exception.1"), skin, "header-s");
        Link url = new Link(Settings.REPO_ISSUES, skin, Settings.REPO_ISSUES);
        OwnLabel applicationMessageLabel = new OwnLabel(I18n.msg("error.crash.applicationmessage"), skin, "header-s");
        OwnLabel crashLocLabel = new OwnLabel(I18n.msg("error.crash.exception.2"), skin);
        OwnLabel crashLoc = new OwnLabel(TextUtils.capString(SysUtils.getCrashReportsDir()
                                                                     .toString(), 50), skin, "hud-subheader");
        crashLoc.addListener(new OwnTextTooltip(SysUtils.getCrashReportsDir()
                                                        .toString(), skin));

        // Crash image
        Image img = new Image(new Texture(Gdx.files.internal("img/crash.png")));

        // Close button
        Button b = new OwnTextButton(I18n.msg("gui.close"), skin, "default");
        b.setWidth(400f);
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
        b.pad(10f);

        Table left = new Table(skin);
        // Add to table
        left.add(title)
                .left()
                .padBottom(10f)
                .row();
        left.add(subtitle)
                .left()
                .padBottom(30f)
                .row();

        left.add(urlLabel)
                .left()
                .padBottom(10f)
                .row();
        left.add(url)
                .left()
                .padBottom(30f)
                .row();

        left.add(applicationMessageLabel)
                .left()
                .padBottom(10f)
                .row();
        left.add(crashLocLabel)
                .left()
                .padBottom(10f)
                .row();
        left.add(crashLoc)
                .left()
                .padBottom(30f)
                .row();

        t.add(left)
                .left();
        t.add(img)
                .left()
                .row();

        // Stack trace
        int lines = 0;
        if (cause != null && cause.getStackTrace() != null) {
            var stringBuilder = new StringBuilder();
            var st = cause.getStackTrace();
            for (var elem : st) {
                stringBuilder.append(elem.toString())
                        .append("\n");
                lines++;
            }
            OwnTextArea stackTraceTextArea = new OwnTextArea(stringBuilder.toString(), skin, "default");
            stackTraceTextArea.setPrefRows(15f);
            stackTraceTextArea.setWidth(800f);
            stackTraceTextArea.setHeight(lines * 35f);

            OwnScrollPane stackTraceScroll = new OwnScrollPane(stackTraceTextArea, skin, "minimalist");
            stackTraceScroll.setWidth(820f);
            stackTraceScroll.setHeight(400f);
            stackTraceScroll.setFadeScrollBars(false);
            OwnLabel stackTraceLabel = new OwnLabel(I18n.msg("error.crash.stacktrace"), skin, "header-s");

            t.add(stackTraceLabel)
                    .colspan(2)
                    .left()
                    .padBottom(10f)
                    .row();
            t.add(stackTraceScroll)
                    .colspan(2)
                    .left()
                    .padBottom(10f)
                    .row();

            OwnTextButton copy = new OwnTextButton(I18n.msg("error.crash.copy"), skin);
            copy.addListener((event) -> {
                if (event instanceof ChangeListener.ChangeEvent) {
                    Gdx.app.getClipboard()
                            .setContents(stringBuilder.toString());
                }
                return false;
            });
            t.add(copy)
                    .colspan(2)
                    .left()
                    .padBottom(80f)
                    .row();

        }

        t.add(b)
                .colspan(2)
                .center()
                .row();
    }

    private SpriteBatch initializeSpriteBatch() {
        var spriteShader = new ShaderProgram(Gdx.files.internal("shader/2d/spritebatch.vertex.glsl"),
                                             Gdx.files.internal("shader/2d/spritebatch.fragment.glsl"));
        // Sprite batch - uses screen resolution
        return new SpriteBatch(100, spriteShader);
    }

    @Override
    public void resize(int width, int height) {
        var unitsPerPixel = 1f / ((float) height / 1000f) * Settings.settings.program.ui.scale;
        vp.setUnitsPerPixel(unitsPerPixel);
        vp.update(width, height, true);
        sb.getProjectionMatrix()
                .setToOrtho2D(0, 0, width, height);
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
