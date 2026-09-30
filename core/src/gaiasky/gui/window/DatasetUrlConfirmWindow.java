/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;

import java.net.URI;
import java.util.Locale;

/**
 * Confirmation dialog for dataset URLs received through the
 * <code>gaiasky://</code> protocol handler. Asks the user for confirmation
 * before downloading and installing a dataset package from the internet.
 */
public class DatasetUrlConfirmWindow extends GenericDialog {
    /** The URL of the dataset package. **/
    private final String datasetUrl;
    /** The runnable to run if the user accepts. **/
    private final Runnable onAccept;
    /** The runnable to run if the user cancels. **/
    private final Runnable onCancel;

    /**
     * Creates the confirmation dialog.
     *
     * @param datasetUrl The URL of the dataset package.
     * @param skin       The UI skin.
     * @param stage      The UI stage.
     * @param onAccept   Runnable to run if the user accepts.
     * @param onCancel   Runnable to run if the user cancels.
     */
    public DatasetUrlConfirmWindow(String datasetUrl,
                                   Skin skin,
                                   Stage stage,
                                   Runnable onAccept,
                                   Runnable onCancel) {
        super(I18n.msg("gui.url.confirm.title"), skin, stage);
        this.datasetUrl = datasetUrl;
        this.onAccept = onAccept;
        this.onCancel = onCancel;

        setAcceptText(I18n.msg("gui.url.confirm.accept"));
        setCancelText(I18n.msg("gui.url.confirm.cancel"));

        buildSuper();

        setAcceptButtonStyle("huge");
        setCancelButtonStyle("huge");
    }

    @Override
    protected void build() {
        content.clear();

        content.add(new OwnLabel(I18n.msg("gui.url.confirm.info"), skin, "header")).left().pad(pad18).row();
        var urlLabel = new OwnLabel(I18n.msg("gui.url.confirm.url", datasetUrl), skin, "default");
        urlLabel.setWrap(true);
        content.add(urlLabel).left().growX().pad(pad18).padBottom(pad18).row();
        content.add(new OwnLabel(I18n.msg("gui.url.confirm.warn"), skin, "huge", 85)).left().pad(pad18).padBottom(pad34).row();

        content.pack();
        pack();
    }

    @Override
    protected boolean accept() {
        if (onAccept != null) {
            onAccept.run();
        }
        return true;
    }

    @Override
    protected void cancel() {
        if (onCancel != null) {
            onCancel.run();
        }
    }

    @Override
    public void dispose() {
    }

    /**
     * Returns true if the given dataset URL points to one of the configured
     * data mirrors. Used to decide whether a confirmation dialog is needed
     * before downloading.
     *
     * @param datasetUrl The dataset URL.
     *
     * @return True if the URL host matches one of the data mirrors.
     */
    public static boolean isFromDataMirror(String datasetUrl) {
        try {
            var uri = new URI(datasetUrl);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            for (var mirror : gaiasky.GaiaSky.settings().program.url.dataMirrors) {
                var mirrorHost = new URI(mirror).getHost();
                if (mirrorHost != null && mirrorHost.equalsIgnoreCase(host)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}