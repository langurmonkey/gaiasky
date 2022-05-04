/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This application displays an error in a window.
 */
public class ErrorDialog implements ApplicationListener {

    private Exception cause;
    private String message;
    private Stage ui;

    public ErrorDialog(Exception cause, String message) {
        this.cause = cause;
        this.message = message;
    }

    @Override
    public void create() {
        final float pad10 = 10f;
        final float pad30 = 30f;

        ScreenViewport svp = new ScreenViewport();
        System.out.println(Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
        int w = Gdx.graphics.getWidth();
        float upp = w > 1000 ? 1f : 1.6f;
        svp.setUnitsPerPixel(upp);
        svp.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        ui = new Stage(svp);
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
        OwnLabel subtitle = new OwnLabel(I18n.msg("notif.error", cause.getLocalizedMessage()), skin, "ui-21");
        // Notification
        OwnLabel urlLabel = new OwnLabel(I18n.msg("error.crash.exception.1"), skin, "header-s");
        Link url = new Link(Settings.REPO_ISSUES, skin, Settings.REPO_ISSUES);
        OwnLabel applicationMessageLabel = new OwnLabel(I18n.msg("error.crash.applicationmessage"), skin, "header-s");
        OwnLabel applicationMessage = new OwnLabel(message, skin, "ui-15");

        // Stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Throwable stack = cause;
        if (cause.getCause() != null) {
            stack = cause.getCause();
        }
        stack.printStackTrace(pw);
        String stackStr = sw.toString();
        long lines = TextUtils.countLines(stackStr);
        OwnTextArea stackTraceTextArea = new OwnTextArea(stackStr, skin, "default");
        stackTraceTextArea.setPrefRows(15);
        stackTraceTextArea.setWidth(800f);
        stackTraceTextArea.setHeight(lines * 35f);

        OwnScrollPane stackTraceScroll = new OwnScrollPane(stackTraceTextArea, skin, "minimalist");
        stackTraceScroll.setWidth(820f);
        stackTraceScroll.setHeight(400f);
        stackTraceScroll.setFadeScrollBars(false);
        OwnLabel stackTraceLabel = new OwnLabel(I18n.msg("error.crash.stacktrace"), skin, "header-s");

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
        b.pad(pad10);

        // Add to table
        t.add(title).left().padBottom(pad10).row();
        t.add(subtitle).left().padBottom(pad30).row();

        t.add(urlLabel).left().padBottom(pad10).row();
        t.add(url).left().padBottom(pad30).row();

        t.add(applicationMessageLabel).left().padBottom(pad10).row();
        t.add(applicationMessage).left().padBottom(pad30).row();

        t.add(stackTraceLabel).left().padBottom(pad10).row();
        t.add(stackTraceScroll).left().padBottom(80f).row();

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
