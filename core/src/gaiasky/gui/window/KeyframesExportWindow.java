/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import gaiasky.util.GlobalResources;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnTextArea;

public class KeyframesExportWindow extends FileNameWindow {

    public OwnCheckBox useOptFlowCam;

    public KeyframesExportWindow(String defaultName, Stage stage, Skin skin) {
        super(defaultName, stage, skin);
    }

    @Override
    protected void build() {
        super.build();

        content.row();

        // Add checkbox.
        useOptFlowCam = new OwnCheckBox("Use OptFlowCam method to compute camera path", skin, 20f);
        useOptFlowCam.setChecked(false);
        // Info pane.
        String plInfoStr = I18n.msg("gui.keyframes.export.optflowcam");
        var ssLines = GlobalResources.countOccurrences(plInfoStr, '\n');
        TextArea plInfo = new OwnTextArea(plInfoStr, skin, "info");
        plInfo.setDisabled(true);
        plInfo.setPrefRows(ssLines + 6);
        plInfo.setWidth(600f);
        plInfo.clearListeners();

        content.add(useOptFlowCam).colspan(2).left().padTop(20f).row();
        content.add(plInfo).colspan(2).right().padTop(20f);
    }
}
