/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashGui extends AbstractGui {
    protected Throwable crash;
    private CrashWindow crashWindow;

    public CrashGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel, final Throwable crash) {
        this(skin, graphics, unitsPerPixel, crash, false);
    }

    public CrashGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel, final Throwable crash, final Boolean vr) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        this.crash = crash;
        this.vr = vr;
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        stage = new Stage(vp, sb);
        if (vr) {
            vp.update(Settings.settings.graphics.backBufferResolution[0], Settings.settings.graphics.backBufferResolution[1], true);
        } else {
            vp.update(GaiaSky.instance.graphics.getWidth(), GaiaSky.instance.graphics.getHeight(), true);
        }

        // Dialog
        crashWindow = new CrashWindow(stage, skin, crash);

        rebuildGui();

    }

    @Override
    public void update(double dt) {
        super.update(dt);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
    }

    public void rebuildGui() {
        if (stage != null) {
            stage.clear();
            crashWindow.show(stage);
        }
    }

    private static class CrashWindow extends GenericDialog {
        private final Throwable crash;

        public CrashWindow(Stage ui, Skin skin, Throwable crash) {
            super(I18n.msg("gui.crash.title"), skin, ui);
            this.crash = crash;

            setAcceptText(I18n.msg("gui.exit"));

            buildSuper();
        }

        @Override
        protected void build() {
            content.clear();

            // Crash image
            Image img = new Image(new Texture(Gdx.files.internal("img/crash.png")));
            content.add(img).center().padBottom(pad18 * 2f).row();

            // Delete data folder and try again
            content.add(new OwnLabel(I18n.msg("gui.crash.info.1"), skin)).left().padBottom(pad10).row();
            OwnLabel dataLocationLabel = new OwnLabel(TextUtils.capString(Settings.settings.data.location, 50), skin, "hud-subheader");
            dataLocationLabel.addListener(new OwnTextTooltip(Settings.settings.data.location, skin));
            content.add(dataLocationLabel).left().padBottom(pad18 * 3f).row();

            // Crash log
            content.add(new OwnLabel(I18n.msg("gui.crash.info.2"), skin)).left().padBottom(pad10).row();
            OwnLabel cloc = new OwnLabel(TextUtils.capString(SysUtils.getCrashReportsDir().toString(), 50), skin, "hud-subheader");
            cloc.addListener(new OwnTextTooltip(SysUtils.getCrashReportsDir().toString(), skin));
            content.add(cloc).left().padBottom(pad10).row();
            content.add(new OwnLabel(I18n.msg("gui.crash.info.3"), skin)).left().padBottom(pad10).row();
            content.add(new Link(Settings.REPO_ISSUES, skin.get("link", Label.LabelStyle.class), Settings.REPO_ISSUES)).left().padBottom(pad18 * 3f).row();

            // Stack trace
            float taw = 1000f;
            float tah = 400f;
            content.add(new OwnLabel(I18n.msg("gui.crash.stack"), skin, "big")).left().padBottom(pad10).row();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            crash.printStackTrace(pw);
            String sts = sw.toString();
            int lines = 1;
            for (char c : sts.toCharArray()) {
                if (c == '\n')
                    lines++;
            }

            TextArea stackTrace = new OwnTextArea(sts, skin.get("regular", TextField.TextFieldStyle.class));
            stackTrace.setDisabled(true);
            stackTrace.setPrefRows(lines);
            stackTrace.setWidth(taw + 20f);
            OwnScrollPane stScroll = new OwnScrollPane(stackTrace, skin, "default-nobg");
            stScroll.setWidth(taw);
            stScroll.setHeight(tah);
            stScroll.setForceScroll(false, true);
            stScroll.setSmoothScrolling(true);
            stScroll.setFadeScrollBars(false);
            content.add(stScroll).center();

        }

        @Override
        protected boolean accept() {
            GaiaSky.postRunnable(() -> Gdx.app.exit());
            return true;
        }

        @Override
        protected void cancel() {
            GaiaSky.postRunnable(() -> Gdx.app.exit());
        }

        @Override
        public void dispose() {

        }

    }

}
