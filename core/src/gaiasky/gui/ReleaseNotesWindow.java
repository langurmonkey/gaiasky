package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextArea;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReleaseNotesWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(ReleaseNotesWindow.class);

    private final Path releaseNotesFile;

    public ReleaseNotesWindow(Stage stage, Skin skin, Path file) {
        super(I18n.msg("gui.releasenotes.title"), skin, stage);

        this.releaseNotesFile = file;
        this.setResizable(true);
        setAcceptText(I18n.msg("gui.ok"));

        // Build
        buildSuper();
    }

    @Override
    protected void build() {

        float taWidth = 1300f;
        float taHeight = 900f;

        try {
            String releaseNotes = Files.readString(releaseNotesFile).trim();

            OwnLabel title = new OwnLabel(Settings.getApplicationTitle(false) + "   " + Settings.settings.version.version, skin, "header");
            content.add(title).left().pad(pad10).padBottom(pad20).row();

            OwnTextArea releaseNotesText = new OwnTextArea(releaseNotes, skin, "disabled-nobg");
            releaseNotesText.setDisabled(true);
            releaseNotesText.setPrefRows(30);
            releaseNotesText.setWidth(taWidth - 15f);
            float fontHeight = releaseNotesText.getStyle().font.getLineHeight();
            releaseNotesText.offsets();
            releaseNotesText.setHeight((releaseNotesText.getLines() + 3) * fontHeight);

            releaseNotesText.clearListeners();

            OwnScrollPane scroll = new OwnScrollPane(releaseNotesText, skin, "default-nobg");
            scroll.setWidth(taWidth);
            scroll.setHeight(taHeight);
            scroll.setForceScroll(false, true);
            scroll.setSmoothScrolling(false);
            scroll.setFadeScrollBars(false);
            content.add(scroll).center().pad(pad10);
            content.pack();

        } catch (IOException e) {
            // Show error
            OwnLabel error = new OwnLabel(I18n.msg("error.file.read", releaseNotesFile.toString()), skin);
            content.add(error).center();
        }
    }

    @Override
    protected boolean accept() {
        // Write current version to $WORKDIR/.releasenotes.rev
        Path releaseNotesRev = SysUtils.getReleaseNotesRevisionFile();
        try {
            if (Files.exists(releaseNotesRev))
                Files.delete(releaseNotesRev);
            Files.writeString(releaseNotesRev, Integer.toString(Settings.settings.version.versionNumber));
        } catch (IOException e) {
            logger.error(e);
        }
        return true;
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }
}
