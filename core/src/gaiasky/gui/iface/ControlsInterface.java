/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.iface;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.*;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.components.*;
import gaiasky.gui.main.ContainerPane;
import gaiasky.gui.main.KeyBindings;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Scene;
import gaiasky.util.CatalogManager;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.OwnTextHotkeyTooltip;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;

public class ControlsInterface extends TableGuiInterface implements IObserver {

    private final Table tableComponentButtons;
    private final Cell<Actor> activeComponentCell;
    private final OwnTextIconButton buttonLoad, buttonSettings, buttonLog, buttonHelp, buttonQuit;
    private final Array<OwnTextButton> componentButtons;
    private final OwnTextIconButton buttonMinimap;

    private final float buttonWidth = 48f, buttonHeight = 48f;

    /**
     * Map names to panes.
     **/
    private final ObjectMap<String, ContainerPane> paneMap;
    /**
     * Map names to buttons.
     **/
    private final ObjectMap<String, OwnTextButton> buttonMap;

    public ControlsInterface(Skin skin, Stage stage, Scene scene, CatalogManager catalogManager, ComponentTypes.ComponentType[] visibilityEntities, boolean[] visible) {
        super(skin);

        this.paneMap = new ObjectMap<>();
        this.buttonMap = new ObjectMap<>();

        final float pad10 = 10f;
        final float pad20 = 20f;

        componentButtons = new Array<>(7);

        // Table to the left, which holds the (components and action) buttons.
        Table tableButtons = new Table(skin);
        // Table to the right, which holds the panes.
        Table tableComponents = new Table(skin);

        // Component buttons, top-left.
        tableComponentButtons = new Table(skin);
        tableButtons.add(tableComponentButtons).left().top().padTop(pad20 * 5f).padRight(pad10).row();

        activeComponentCell = tableComponents.add().left().top();


        // TIME.
        TimeComponent timeComponent = new TimeComponent(skin, stage);
        timeComponent.initialize(getContentWidth());
        createComponentButton(skin, I18n.msg("gui.time"), timeComponent, "action.expandcollapse.pane/gui.time");

        // CAMERA.
        CameraComponent cameraComponent = new CameraComponent(skin, stage);
        cameraComponent.initialize(getContentWidth());
        createComponentButton(skin, pad10, pad10 * 6f, "menu-camera", I18n.msg("gui.camera"), cameraComponent, "action.expandcollapse.pane/gui.camera");

        // TYPE VISIBILITY.
        VisibilityComponent visibilityComponent = new VisibilityComponent(skin, stage);
        visibilityComponent.setVisibilityEntitites(visibilityEntities, visible);
        visibilityComponent.initialize(getContentWidth());
        createComponentButton(skin, pad10, pad10 * 12f, "menu-types", I18n.msg("gui.visibility"), visibilityComponent, "action.expandcollapse.pane/gui.visibility");

        // VISUALS.
        VisualSettingsComponent visualSettingsComponent = new VisualSettingsComponent(skin, stage);
        visualSettingsComponent.initialize(getContentWidth());
        createComponentButton(skin, pad10, pad10 * 17.5f, "menu-visuals", I18n.msg("gui.lighting"), visualSettingsComponent, "action.expandcollapse.pane/gui.lighting");

        // DATASETS.
        DatasetsComponent datasetsComponent = new DatasetsComponent(skin, stage, catalogManager);
        datasetsComponent.initialize(getContentWidth());
        createComponentButton(skin, pad10, pad10 * 23f, "menu-datasets", I18n.msg("gui.dataset.title"), datasetsComponent, "action.expandcollapse.pane/gui.dataset.title");

        // LOCATION LOG.
        LocationLogComponent locationLogComponent = new LocationLogComponent(skin, stage);
        locationLogComponent.initialize(getContentWidth());
        createComponentButton(skin, pad10, pad10 * 29f, "menu-location-log", I18n.msg("gui.locationlog"), locationLogComponent, null);

        // BOOKMARKS.
        BookmarksComponent bookmarksComponent = new BookmarksComponent(skin, stage);
        bookmarksComponent.setScene(scene);
        bookmarksComponent.initialize(getContentWidth());
        createComponentButton(skin, pad10, pad10 * 35f, "menu-bookmarks", I18n.msg("gui.bookmarks"), bookmarksComponent, "action.expandcollapse.pane/gui.bookmarks");

        // Spacing
        tableButtons.add().left().growY().row();

        // Actions, bottom-left.
        KeyBindings kb = KeyBindings.instance;
        Table tableActionButtons = new Table(skin);
        tableButtons.add(tableActionButtons).left().bottom().padBottom(pad10 * 8f);

        // MINIMAP.
        buttonMinimap = new OwnTextIconButton("", skin, "menu-map");
        buttonMinimap.setSize(buttonWidth, buttonHeight);
        buttonMinimap.setName("map");
        buttonMinimap.setChecked(Settings.settings.program.minimap.active);
        String[] minimapHotkey = kb.getStringKeys("action.toggle/gui.minimap.title", true);
        buttonMinimap.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.map"), minimapHotkey, skin));
        buttonMinimap.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.MINIMAP_DISPLAY_CMD, buttonMinimap, buttonMinimap.isChecked());
            }
            return false;
        });
        tableActionButtons.add(buttonMinimap).left().bottom().padBottom(pad10).row();

        // LOAD DATASET.
        buttonLoad = new OwnTextIconButton("", skin, "load");
        buttonLoad.setSize(buttonWidth, buttonHeight);
        buttonLoad.setName("loadcatalog");
        buttonLoad.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.loadcatalog"), kb.getStringKeys("action.loadcatalog", true), skin));
        buttonLoad.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_LOAD_CATALOG_ACTION, buttonLoad);
                buttonLoad.setCheckedNoFire(false);
            }
            return false;
        });
        tableActionButtons.add(buttonLoad).left().bottom().padBottom(pad10).row();

        // PREFERENCES.
        buttonSettings = new OwnTextIconButton("", skin, "preferences");
        buttonSettings.setSize(buttonWidth, buttonHeight);
        buttonSettings.setName("preferences");
        String[] prefsHotkey = kb.getStringKeys("action.preferences", true);
        buttonSettings.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.preferences"), prefsHotkey, skin));
        buttonSettings.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_PREFERENCES_ACTION, buttonSettings);
                buttonSettings.setCheckedNoFire(false);
            }
            return false;
        });
        tableActionButtons.add(buttonSettings).left().bottom().padBottom(pad10).row();

        // SESSION LOG.
        buttonLog = new OwnTextIconButton("", skin, "log");
        buttonLog.setSize(buttonWidth, buttonHeight);
        buttonLog.setName("show log");
        buttonLog.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.tooltip.log"), kb.getStringKeys("action.log", true), skin));
        buttonLog.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_LOG_CMD, buttonLog);
                buttonLog.setCheckedNoFire(false);
            }
            return false;
        });
        tableActionButtons.add(buttonLog).left().bottom().padBottom(pad10).row();

        // HELP/ABOUT.
        buttonHelp = new OwnTextIconButton("", skin, "help");
        buttonHelp.setSize(buttonWidth, buttonHeight);
        buttonHelp.setName("about");
        String[] helpHotkeys = kb.getStringKeys("action.help", true);
        buttonHelp.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.help"), helpHotkeys, skin));
        buttonHelp.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_ABOUT_ACTION, buttonHelp);
                buttonHelp.setCheckedNoFire(false);
            }
            return false;
        });
        tableActionButtons.add(buttonHelp).left().bottom().padBottom(pad10).row();

        // QUIT.
        buttonQuit = new OwnTextIconButton("", skin, "quit");
        buttonQuit.setSize(buttonWidth, buttonHeight);
        buttonQuit.setName("quit");
        buttonQuit.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.quit.title"), kb.getStringKeys("action.exit", true), skin));
        buttonQuit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_QUIT_ACTION, buttonQuit);
                buttonQuit.setCheckedNoFire(false);
            }
            return false;
        });
        tableActionButtons.add(buttonQuit).left().bottom();

        // Add buttons and components tables to main table.
        add(tableButtons).left().top().growY();
        add(tableComponents).left().top().padTop(pad10 * 8f);

        EventManager.instance.subscribe(this, Event.GUI_FOLD_CMD, Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD,
                Event.EXPAND_COLLAPSE_PANE_CMD, Event.MINIMAP_DISPLAY_CMD, Event.MINIMAP_TOGGLE_CMD);
    }

    private void createComponentButton(Skin skin, String title, GuiComponent component, String action) {
        createComponentButton(skin, (float) 10.0, 0f, "menu-time", title, component, action);
    }

    /**
     * Closes the given pane.
     *
     * @param pane The pane to close.
     */
    private void close(Actor pane) {
        pane.clearActions();
        pane.addAction(Actions.sequence(
                Actions.alpha(1f),
                Actions.fadeOut(Settings.settings.program.ui.getAnimationSeconds() * 0.5f),
                Actions.run(activeComponentCell::clearActor)
        ));
    }
    /** Reference to the task that closes the current pane. **/
    private Timer.Task closeTask;


    private OwnTextIconButton createComponentButton(Skin skin, float pad, float padTop, String buttonStyle, String title, GuiComponent component, String action) {
        OwnTextIconButton button = new OwnTextIconButton("", skin, buttonStyle);
        button.setSize(buttonWidth, buttonHeight);
        button.align(Align.center);
        componentButtons.add(button);
        tableComponentButtons.add(button).left().top().padBottom(pad).row();

        ContainerPane pane = new ContainerPane(skin, title, component.getActor());
        if (Settings.settings.program.ui.expandOnMouseOver) {
            pane.addListener(new InputListener() {
                @Override
                public boolean mouseMoved(InputEvent event, float x, float y) {
                    // Cancel previous close task.
                    if (closeTask != null) {
                        closeTask.cancel();
                    }
                    closeTask = new Timer.Task() {
                        @Override
                        public void run() {
                            close(pane);
                        }
                    };
                    Timer.schedule(closeTask, 4);
                    return false;
                }

            });
        }

        // Add to maps.
        String key = component.getClass().getSimpleName();
        paneMap.put(key, pane);
        buttonMap.put(key, button);

        if (!Settings.settings.program.ui.expandOnMouseOver) {
            // Add button tooltip.
            if (action != null && !action.isBlank()) {
                String[] shortcutKeys = KeyBindings.instance.getStringKeys(action, true);
                if (shortcutKeys != null && shortcutKeys.length > 0) {
                    button.addListener(new OwnTextHotkeyTooltip(title, shortcutKeys, skin));
                } else {
                    button.addListener(new OwnTextTooltip(title, skin));
                }
            } else {
                button.addListener(new OwnTextTooltip(title, skin));
            }

            button.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    if (button.isChecked()) {
                        //Add pane.
                        pane.clearActions();
                        activeComponentCell.clearActor();
                        selectComponentButton(button);
                        activeComponentCell.padTop(padTop);
                        activeComponentCell.setActor(pane);
                        pane.addAction(Actions.sequence(
                                Actions.alpha(0f),
                                Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds() * 0.5f)));
                    } else {
                        // Remove pane.
                        pane.clearActions();
                        pane.addAction(Actions.sequence(
                                Actions.alpha(1f),
                                Actions.fadeOut(Settings.settings.program.ui.getAnimationSeconds() * 0.5f),
                                Actions.run(activeComponentCell::clearActor)
                        ));
                    }
                }
                return false;
            });
        } else {
            button.addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, @Null Actor fromActor) {
                    if (pointer != -1) return;
                    Actor actor = event.getListenerActor();
                    if (fromActor != null && fromActor.isDescendantOf(actor)) return;
                    //Add pane.
                    pane.clearActions();
                    activeComponentCell.clearActor();
                    selectComponentButton(button);
                    activeComponentCell.padTop(padTop);
                    activeComponentCell.setActor(pane);
                    pane.addAction(Actions.sequence(
                            Actions.alpha(0f),
                            Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds() * 0.5f)));

                    // Add automatic close task.
                    if (closeTask != null) {
                        closeTask.cancel();
                    }
                    closeTask = new Timer.Task() {
                        @Override
                        public void run() {
                            close(pane);
                        }
                    };
                    Timer.schedule(closeTask, 4);
                }
            });
        }

        return button;
    }

    private void selectComponentButton(OwnTextButton button) {
        for (var b : componentButtons) {
            if (b != button) {
                b.setCheckedNoFire(false);
            }
        }
    }

    /**
     * Content width. To be used in all components.
     *
     * @return The width of the content.
     */
    private static float getContentWidth() {
        return 380f;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void update() {

    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        switch (event) {
            case GUI_FOLD_CMD -> {
                // We try to keep compatibility with the old window.
                // Only implement collapse.
                boolean collapse;
                if (data.length >= 1) {
                    collapse = (boolean) data[0];
                    if (collapse) {
                        for (var b : componentButtons) {
                            if (b.isChecked()) {
                                b.setChecked(false);
                            }
                        }
                    }
                } else {
                    // Toggle.
                    Button open = null;
                    for (var b : componentButtons) {
                        if (b.isChecked()) {
                            open = b;
                            break;
                        }
                    }
                    if (open != null) {
                        open.setChecked(false);
                    }
                }
            }
            case TOGGLE_EXPANDCOLLAPSE_PANE_CMD -> {
                String name = (String) data[0];
                if (buttonMap.containsKey(name)) {
                    Button button = buttonMap.get(name);
                    button.setChecked(!button.isChecked());
                }
            }
            case EXPAND_COLLAPSE_PANE_CMD -> {
                String name = (String) data[0];
                Boolean expand = (Boolean) data[1];
                if (buttonMap.containsKey(name)) {
                    Button button = buttonMap.get(name);
                    button.setChecked(expand);
                }
            }
            case MINIMAP_DISPLAY_CMD -> {
                boolean show = (Boolean) data[0];
                if (source != buttonMinimap) {
                    buttonMinimap.setCheckedNoFire(show);
                }
            }
            case MINIMAP_TOGGLE_CMD -> buttonMinimap.setCheckedNoFire(!buttonMinimap.isChecked());
            default -> {
            }
        }
    }

}
