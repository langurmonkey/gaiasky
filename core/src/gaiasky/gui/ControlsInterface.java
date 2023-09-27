/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import gaiasky.gui.components.TimeComponent;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.OwnTextIconButton;

public class ControlsInterface extends TableGuiInterface {

    private final Stage ui;
    private final Table tableComponentButtons, tableActionButtons;
    private final Table activeComponentTable;
    private final OwnTextIconButton buttonTime, buttonCamera, buttonTypes, buttonVisuals, buttonDatasets, buttonLocation, buttonBookmarks, buttonMinimap;
    private final OwnTextIconButton buttonLoad, buttonSettings, buttonLog, buttonHelp, buttonQuit;
    private final Array<OwnTextButton> componentButtons;

    public ControlsInterface(Skin skin, Stage stage) {
        super(skin);
        this.ui = stage;

        float pad = 10f;
        componentButtons = new Array<>(7);

        // Components, top-left.
        tableComponentButtons = new Table(skin);
        activeComponentTable = new Table(skin);
        activeComponentTable.setBackground("table-bg");
        activeComponentTable.pad(pad * 3f);

        Table tableComponents = new Table(skin);
        tableComponents.add(tableComponentButtons).left().top().padRight(pad);
        tableComponents.add(activeComponentTable).left().top();

        add(tableComponents).left().top().padTop(pad * 8f).row();


        // TIME.
        buttonTime = new OwnTextIconButton("", skin, "menu-time");
        componentButtons.add(buttonTime);
        tableComponentButtons.add(buttonTime).left().top().padBottom(pad).row();

        TimeComponent timeComponent = new TimeComponent(skin, ui);
        timeComponent.initialize(getContentWidth());

        buttonTime.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                activeComponentTable.clear();
                if (buttonTime.isChecked()) {
                    selectComponentButton(buttonTime);
                    activeComponentTable.add(timeComponent.getActor());
                }
            }
            return false;
        });


        // CAMERA.
        buttonCamera = new OwnTextIconButton("", skin, "menu-camera");
        componentButtons.add(buttonCamera);
        tableComponentButtons.add(buttonCamera).left().top().padBottom(pad).row();

        // TYPE VISIBILITY.
        buttonTypes = new OwnTextIconButton("", skin, "menu-types");
        componentButtons.add(buttonTypes);
        tableComponentButtons.add(buttonTypes).left().top().padBottom(pad).row();

        // VISUALS.
        buttonVisuals = new OwnTextIconButton("", skin, "menu-visuals");
        componentButtons.add(buttonVisuals);
        tableComponentButtons.add(buttonVisuals).left().top().padBottom(pad).row();

        // DATASETS.
        buttonDatasets = new OwnTextIconButton("", skin, "menu-datasets");
        componentButtons.add(buttonDatasets);
        tableComponentButtons.add(buttonDatasets).left().top().padBottom(pad).row();

        // LOCATION LOG.
        buttonLocation = new OwnTextIconButton("", skin, "menu-location-log");
        componentButtons.add(buttonLocation);
        tableComponentButtons.add(buttonLocation).left().top().padBottom(pad).row();

        // BOOKMARKS.
        buttonBookmarks = new OwnTextIconButton("", skin, "menu-bookmarks");
        componentButtons.add(buttonBookmarks);
        tableComponentButtons.add(buttonBookmarks).left().top().padBottom(pad).row();

        // Spacing
        add().left().growY().row();

        // Actions, bottom-left.
        tableActionButtons = new Table(skin);
        add(tableActionButtons).left().bottom().padBottom(pad * 8f);

        buttonMinimap = new OwnTextIconButton("", skin, "menu-map");
        tableActionButtons.add(buttonMinimap).left().bottom().padBottom(pad).row();

        buttonLoad = new OwnTextIconButton("", skin, "load");
        tableActionButtons.add(buttonLoad).left().bottom().padBottom(pad).row();

        buttonSettings = new OwnTextIconButton("", skin, "preferences");
        tableActionButtons.add(buttonSettings).left().bottom().padBottom(pad).row();

        buttonLog = new OwnTextIconButton("", skin, "log");
        tableActionButtons.add(buttonLog).left().bottom().padBottom(pad).row();

        buttonHelp = new OwnTextIconButton("", skin, "help");
        tableActionButtons.add(buttonHelp).left().bottom().padBottom(pad).row();

        buttonQuit = new OwnTextIconButton("", skin, "quit");
        tableActionButtons.add(buttonQuit).left().bottom();

    }

    private void selectComponentButton(OwnTextButton button) {
        for (var b : componentButtons) {
            if (b != button) {
                b.setProgrammaticChangeEvents(false);
                b.setChecked(false);
                b.setProgrammaticChangeEvents(true);
            }
        }
    }

    /**
     * Content width. To be used in all components.
     *
     * @return The width of the content.
     */
    private static float getContentWidth() {
        return 352f;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void update() {

    }
}
