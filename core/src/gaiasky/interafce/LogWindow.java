/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.desktop.util.SysUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.Logger;
import gaiasky.util.scene2d.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

public class LogWindow extends GenericDialog {

    private final DateTimeFormatter format;
    private Table logs;
    private ScrollPane scroll;

    // Current number of messages in window
    private int nmsg = 0;

    private float w, h, pad;

    public LogWindow(Stage stage, Skin skin) {
        super(I18n.msg("gui.log.title"), skin, stage);

        this.format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM).withLocale(I18n.locale).withZone(ZoneOffset.UTC);
        this.setResizable(true);
        setCancelText(I18n.msg("gui.close"));

        // Build
        buildSuper();

    }

    @Override
    protected void build() {
        w = Math.min(1200f, Gdx.graphics.getWidth() - 200);
        h = Math.min(860f, Gdx.graphics.getHeight() - 150);
        pad = 16f;

        logs = new Table(skin);
        List<MessageBean> list = NotificationsInterface.historical;
        for (MessageBean mb : list) {
            addMessage(mb);
        }

        scroll = new OwnScrollPane(logs, skin, "minimalist-nobg");
        scroll.setFadeScrollBars(true);
        scroll.setScrollingDisabled(false, false);
        scroll.setSmoothScrolling(true);
        scroll.setHeight(h);
        scroll.setWidth(w);
        scroll.pack();
        updateScroll();

        HorizontalGroup buttons = new HorizontalGroup();
        buttons.pad(pad);
        buttons.space(pad);

        Button reload = new OwnTextIconButton("", skin, "reload");
        reload.setName("update log");
        reload.pad(pad5);
        reload.addListener(new OwnTextTooltip(I18n.msg("gui.log.update"), skin));
        reload.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                update();
            }
            return false;
        });
        buttons.addActor(reload);

        Button export = new OwnTextButton(I18n.msg("gui.log.export"), skin);
        export.setName("export log");
        export.pad(pad5);
        export.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                export();
            }
            return false;
        });
        buttons.addActor(export);

        content.add(scroll).padBottom(pad).row();
        content.add(buttons).align(Align.center);
    }

    public void update() {
        if (logs != null) {
            List<MessageBean> list = NotificationsInterface.historical;
            if (list.size() > nmsg) {
                for (int i = nmsg; i < list.size(); i++) {
                    addMessage(list.get(i));
                }
            }
            updateScroll();
        }
    }

    public void export() {
        String filename = Instant.now().toString() + "_gaiasky.log";
        filename = filename.replace(":", "-");
        Path gshome = SysUtils.getDataDir();
        Path log = gshome.resolve(filename);

        try {
            FileWriter fw = new FileWriter(log.toFile());
            BufferedWriter bw = new BufferedWriter(fw);
            for (MessageBean mb : NotificationsInterface.historical) {
                fw.write(format.format(mb.date) + " - " + mb.msg + '\n');
            }
            bw.flush();
            bw.close();
            Logger.getLogger(this.getClass()).info("Log file written to " + log.toAbsolutePath());
        } catch (Exception e) {
            Logger.getLogger(this.getClass()).error(e);
        }

    }

    public void updateScroll() {
        scroll.setScrollPercentX(0);
        scroll.setScrollPercentY(1);
        scroll.invalidate();
    }

    private void addMessage(MessageBean mb) {
        Label date = new OwnLabel(format.format(mb.date), skin);
        Label msg = new OwnLabel(mb.msg, skin);
        logs.add(date).left().padRight(pad);
        logs.add(msg).left().row();
        nmsg++;
    }

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }

}
