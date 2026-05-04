/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.scene2d;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import gaiasky.gui.window.GenericDialog;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.FilePickerComponent.FilePickerTarget;
import gaiasky.util.scene2d.FilePickerComponent.ResultListener;

import java.nio.file.Path;
import java.util.function.Consumer;

public class FilePicker extends GenericDialog {
    private final FilePickerComponent component;

    public FilePicker(String title, final Skin skin, Stage stage, Path baseDir, FilePickerTarget target) {
        this(title, skin, stage, baseDir, target, true);
    }

    public FilePicker(String title,
                      final Skin skin,
                      Stage stage,
                      Path baseDir,
                      FilePickerTarget target,
                      boolean directoryBrowsingEnabled) {
        super(title, skin, stage);

        component = new FilePickerComponent(skin, stage, baseDir, target, directoryBrowsingEnabled);

        // Link component logic to Dialog buttons
        component.setSelectionValidListener(valid -> acceptButton.setDisabled(!valid));
        component.setOnDoubleAccept(() -> acceptButton.fire(new ChangeListener.ChangeEvent()));

        setCancelText(I18n.msg("gui.close"));
        setAcceptText(I18n.msg("gui.select"));

        buildSuper();
    }

    @Override
    protected void build() {
        content.clear();
        content.add(component).expand().fill();
    }

    public void setShowHidden(boolean showHidden) {
        component.setShowHidden(showHidden);
    }

    public void setFileFilter(FilePickerComponent.PathnameFilter f) {
        component.setFileFilter(f);
    }

    public void setAcceptedFiles(String text) {
        component.setAcceptedFiles(text);
    }

    public void setResultListener(ResultListener listener) {
        component.setResultListener(listener);
    }

    public void setShowHiddenConsumer(Consumer<Boolean> c) {
        component.setShowHiddenConsumer(c);
    }

    @Override
    public boolean accept() {
        component.result(true);
        return true;
    }

    @Override
    public void cancel() {
        component.result(false);
    }

    @Override
    public void dispose() {
    }


}