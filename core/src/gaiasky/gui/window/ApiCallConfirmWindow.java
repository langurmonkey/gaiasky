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

/**
 * Confirmation dialog for scripting API calls received through the
 * <code>gaiasky://</code> protocol handler. Any web page can trigger a
 * <code>gaiasky://</code> URL (via a link, iframe, or redirect), so reflective
 * API calls are only executed after the user has explicitly accepted them.
 * <p>
 * Dataset downloads through <code>gaiasky://load</code> use
 * {@link DatasetUrlConfirmWindow} instead.
 */
public class ApiCallConfirmWindow extends GenericDialog {
    /** The human-readable call, for display purposes. **/
    private final String call;
    /** The runnable to run if the user accepts. **/
    private final Runnable onAccept;
    /** The runnable to run if the user cancels. **/
    private final Runnable onCancel;

    /**
     * Creates the confirmation dialog.
     *
     * @param apiUrl   The full URL that triggered the call.
     * @param call     The API call, e.g. "camera/go_to_object".
     * @param skin     The UI skin.
     * @param stage    The UI stage.
     * @param onAccept Runnable to run if the user accepts.
     * @param onCancel Runnable to run if the user cancels.
     */
    public ApiCallConfirmWindow(String apiUrl,
                                String call,
                                Skin skin,
                                Stage stage,
                                Runnable onAccept,
                                Runnable onCancel) {
        super(I18n.msg("gui.url.apicall.confirm.title"), skin, stage);
        // The API URL, for display purposes.
        this.call = call;
        this.onAccept = onAccept;
        this.onCancel = onCancel;

        setAcceptText(I18n.msg("gui.url.apicall.confirm.accept"));
        setCancelText(I18n.msg("gui.url.confirm.cancel"));

        buildSuper();

        setAcceptButtonStyle("huge");
        setCancelButtonStyle("huge");
    }

    @Override
    protected void build() {
        content.clear();

        content.add(new OwnLabel(I18n.msg("gui.url.apicall.confirm.info"), skin, "header")).left().pad(pad18).row();
        var callLabel = new OwnLabel(I18n.msg("gui.url.apicall.confirm.call", call), skin, "default");
        callLabel.setWrap(true);
        content.add(callLabel).left().growX().pad(pad18).padBottom(pad18).row();
        content.add(new OwnLabel(I18n.msg("gui.url.apicall.confirm.warn"), skin, "huge", 85)).left().pad(pad18).padBottom(pad34).row();

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
}