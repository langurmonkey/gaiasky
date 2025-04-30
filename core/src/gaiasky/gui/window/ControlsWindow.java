/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.components.*;
import gaiasky.gui.main.KeyBindings;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Scene;
import gaiasky.util.CatalogManager;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.scene2d.*;
import net.jafama.FastMath;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControlsWindow extends CollapsibleWindow implements IObserver {

    private final CatalogManager catalogManager;
    /**
     * The user interface stage
     */
    protected Stage ui;
    protected Skin skin;
    protected VerticalGroup mainVertical;
    protected OwnScrollPane windowScroll;
    protected Table guiLayout;
    protected OwnTextIconButton buttonMinimap = null;
    protected TiledDrawable separator;

    /**
     * The scene.
     */
    private Scene scene;
    /**
     * Entities that will go in the visibility check boxes
     */
    private ComponentType[] visibilityEntities;
    private boolean[] visible;
    /**
     * Access panes
     **/
    private Map<String, CollapsiblePane> panes;

    public ControlsWindow(final String title, final Skin skin, final Stage ui, final CatalogManager catalogManager) {
        super(title, skin);
        this.setName(title);
        this.skin = skin;
        this.ui = ui;
        this.catalogManager = catalogManager;

        // Global resources
        TextureRegion separatorTextureRegion = ((TextureRegionDrawable) skin.newDrawable("separator-dash")).getRegion();
        separatorTextureRegion.getTexture().setWrap(TextureWrap.Repeat, TextureWrap.ClampToEdge);
        this.separator = new TiledDrawable(separatorTextureRegion);

        EventManager.instance.subscribe(this, Event.GUI_SCROLL_POSITION_CMD, Event.GUI_FOLD_CMD, Event.GUI_MOVE_CMD, Event.RECALCULATE_CONTROLS_WINDOW_SIZE, Event.EXPAND_COLLAPSE_PANE_CMD, Event.TOGGLE_EXPANDCOLLAPSE_PANE_CMD, Event.MINIMAP_DISPLAY_CMD, Event.MINIMAP_TOGGLE_CMD);
    }

    /**
     * Content width. To be used in all components.
     *
     * @return The width of the content.
     */
    public static float getContentWidth() {
        return 352f;
    }

    public void initialize() {

        /* Global layout */
        guiLayout = new Table();
        guiLayout.pad(0);
        guiLayout.align(Align.left);

        List<Actor> mainActors = new ArrayList<>();
        panes = new HashMap<>();

        /* ----TIME GROUP---- */
        TimeComponent timeComponent = new TimeComponent(skin, ui);
        timeComponent.initialize(getContentWidth());

        String shortcut = KeyBindings.instance.getStringKeys("action.expandcollapse.pane/gui.time");

        CollapsiblePane time = new CollapsiblePane(ui, I18n.msg("gui.time"), timeComponent.getActor(), getContentWidth(), skin, true, shortcut);
        time.align(Align.left);
        mainActors.add(time);
        panes.put(timeComponent.getClass().getSimpleName(), time);

        /* ----CAMERA---- */
        CameraComponent cameraComponent = new CameraComponent(skin, ui);
        cameraComponent.initialize(getContentWidth());

        shortcut = KeyBindings.instance.getStringKeys("action.expandcollapse.pane/gui.camera");

        CollapsiblePane camera = new CollapsiblePane(ui, I18n.msg("gui.camera"), cameraComponent.getActor(), getContentWidth(), skin, false, shortcut);
        camera.align(Align.left);
        mainActors.add(camera);
        panes.put(cameraComponent.getClass().getSimpleName(), camera);

        /* ----OBJECT TOGGLES GROUP---- */
        VisibilityComponent visibilityComponent = new VisibilityComponent(skin, ui);
        visibilityComponent.setVisibilityEntitites(visibilityEntities, visible);
        visibilityComponent.initialize(getContentWidth());

        shortcut = KeyBindings.instance.getStringKeys("action.expandcollapse.pane/gui.visibility");

        CollapsiblePane visibility = new CollapsiblePane(ui, I18n.msg("gui.visibility"), visibilityComponent.getActor(), getContentWidth(), skin, false, shortcut);
        visibility.align(Align.left);
        mainActors.add(visibility);
        panes.put(visibilityComponent.getClass().getSimpleName(), visibility);

        /* ----LIGHTING GROUP---- */
        VisualSettingsComponent visualSettingsComponent = new VisualSettingsComponent(skin, ui);
        visualSettingsComponent.initialize(getContentWidth());

        shortcut = KeyBindings.instance.getStringKeys("action.expandcollapse.pane/gui.lighting");

        CollapsiblePane visualEffects = new CollapsiblePane(ui, I18n.msg("gui.lighting"), visualSettingsComponent.getActor(), getContentWidth(), skin, false, shortcut);
        visualEffects.align(Align.left);
        mainActors.add(visualEffects);
        panes.put(visualSettingsComponent.getClass().getSimpleName(), visualEffects);

        /* ----DATASETS---- */
        DatasetsComponent datasetsComponent = new DatasetsComponent(skin, ui, catalogManager);
        datasetsComponent.initialize(getContentWidth());

        shortcut = KeyBindings.instance.getStringKeys("action.expandcollapse.pane/gui.dataset.title");

        CollapsiblePane datasets = new CollapsiblePane(ui, I18n.msg("gui.dataset.title"), datasetsComponent.getActor(), getContentWidth(), skin, false, shortcut);
        datasets.align(Align.left);
        mainActors.add(datasets);
        panes.put(datasetsComponent.getClass().getSimpleName(), datasets);

        /* ----LOCATION LOG---- */
        LocationLogComponent locationLogComponent = new LocationLogComponent(skin, ui);
        locationLogComponent.initialize(getContentWidth());

        CollapsiblePane locationLog = new CollapsiblePane(ui, I18n.msg("gui.locationlog"), locationLogComponent.getActor(), getContentWidth(), skin, false, null);
        locationLog.align(Align.left);
        mainActors.add(locationLog);
        panes.put(locationLogComponent.getClass().getSimpleName(), locationLog);

        /* ----BOOKMARKS---- */
        BookmarksComponent bookmarksComponent = new BookmarksComponent(skin, ui);
        bookmarksComponent.setScene(scene);
        bookmarksComponent.initialize(getContentWidth());

        shortcut = KeyBindings.instance.getStringKeys("action.expandcollapse.pane/gui.bookmarks");

        CollapsiblePane bookmarks = new CollapsiblePane(ui, I18n.msg("gui.bookmarks"), bookmarksComponent.getActor(), getContentWidth(), skin, false, shortcut);
        bookmarks.align(Align.left);
        mainActors.add(bookmarks);
        panes.put(bookmarksComponent.getClass().getSimpleName(), bookmarks);

        Table buttonsTable;
        /* BUTTONS */
        float bw = 48f, bh = 48f;
        KeyBindings kb = KeyBindings.instance;
        buttonMinimap = new OwnTextIconButton("", skin, "menu-map");
        buttonMinimap.setSize(bw, bh);
        buttonMinimap.setName("map");
        buttonMinimap.setChecked(Settings.settings.program.minimap.active);
        String minimapHotkey = kb.getStringKeys("action.toggle/gui.minimap.title");
        buttonMinimap.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.map"), minimapHotkey, skin));
        buttonMinimap.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.MINIMAP_DISPLAY_CMD, buttonMinimap, buttonMinimap.isChecked());
            }
            return false;
        });
        OwnTextButton buttonLoad = new OwnTextIconButton("", skin, "load");
        buttonLoad.setSize(bw, bh);
        buttonLoad.setName("loadcatalog");
        buttonLoad.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.loadcatalog"), kb.getStringKeys("action.loadcatalog"), skin));
        buttonLoad.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_LOAD_CATALOG_ACTION, buttonLoad);
                buttonLoad.setCheckedNoFire(false);
            }
            return false;
        });
        OwnTextButton buttonSettings = new OwnTextIconButton("", skin, "preferences");
        buttonSettings.setSize(bw, bh);
        buttonSettings.setName("preferences");
        String prefsHotkey = kb.getStringKeys("action.preferences");
        buttonSettings.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.preferences"), prefsHotkey, skin));
        buttonSettings.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_PREFERENCES_ACTION, buttonSettings);
                buttonSettings.setCheckedNoFire(false);
            }
            return false;
        });
        OwnTextButton buttonLog = new OwnTextIconButton("", skin, "log");
        buttonLog.setSize(bw, bh);
        buttonLog.setName("show log");
        buttonLog.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.tooltip.log"), kb.getStringKeys("action.log"), skin));
        buttonLog.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_LOG_CMD, buttonLog);
                buttonLog.setCheckedNoFire(false);
            }
            return false;
        });
        OwnTextButton buttonHelp = new OwnTextIconButton("", skin, "help");
        buttonHelp.setSize(bw, bh);
        buttonHelp.setName("about");
        String helpHotkey = kb.getStringKeys("action.help");
        buttonHelp.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.help"), helpHotkey, skin));
        buttonHelp.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_ABOUT_ACTION, buttonHelp);
                buttonHelp.setCheckedNoFire(false);
            }
            return false;
        });
        OwnTextButton buttonQuit = new OwnTextIconButton("", skin, "quit");
        buttonQuit.setSize(bw, bh);
        buttonQuit.setName("quit");
        buttonQuit.addListener(new OwnTextHotkeyTooltip(I18n.msg("gui.quit.title"), kb.getStringKeys("action.exit"), skin));
        buttonQuit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_QUIT_ACTION, buttonQuit);
                buttonQuit.setCheckedNoFire(false);
            }
            return false;
        });

        float pad = 3f;
        buttonsTable = new Table(skin);
        buttonsTable.add(buttonMinimap).pad(pad).top().left();
        buttonsTable.add(buttonLoad).pad(pad).top().left();
        buttonsTable.add(buttonSettings).pad(pad).top().left();
        buttonsTable.add(buttonLog).pad(pad).top().left();
        buttonsTable.add(buttonHelp).pad(pad).top().left();
        buttonsTable.add(buttonQuit).pad(pad).top().left();

        buttonsTable.pack();

        /* ADD GROUPS TO VERTICAL LAYOUT */

        int padBottom = FastMath.round(16f);
        int padSides = FastMath.round(8f);
        int padSeparator = FastMath.round(15f);

        guiLayout.padTop(padSides);

        int size = mainActors.size();
        for (int i = 0; i < size; i++) {
            Actor actor = mainActors.get(i);
            guiLayout.add(actor).prefWidth(188f).left().padBottom(padBottom).padLeft(padSides);
            if (i < size - 1) {
                // Not last
                guiLayout.row();
                guiLayout.add(new Image(separator)).left().fill(true, false).padTop(padSeparator).padLeft(padSides);
                guiLayout.row();
            }
        }
        guiLayout.align(Align.top | Align.left);

        windowScroll = new OwnScrollPane(guiLayout, skin, "minimalist-nobg");
        windowScroll.setName("control panel scroll");
        windowScroll.setFadeScrollBars(false);
        windowScroll.setScrollingDisabled(true, false);
        windowScroll.setOverscroll(false, false);
        windowScroll.setSmoothScrolling(true);
        windowScroll.pack();
        windowScroll.setWidth(guiLayout.getWidth() + windowScroll.getStyle().vScroll.getMinWidth());

        mainVertical = new VerticalGroup();
        mainVertical.space(padSides);
        mainVertical.align(Align.right).align(Align.top);
        mainVertical.addActor(windowScroll);
        // Add buttons only in desktop version
        mainVertical.addActor(buttonsTable);
        mainVertical.pack();

        /* ADD TO MAIN WINDOW */
        add(mainVertical).top().left().expand();
        setPosition(0, FastMath.round(Gdx.graphics.getHeight() - getHeight()));

        setWidth(mainVertical.getWidth());

        pack();
        recalculateSize();
    }

    public void recalculateSize() {
        // Save position
        float topy = getY() + getHeight();

        // Calculate new size
        guiLayout.pack();
        if (windowScroll != null) {
            float unitsPerPixel = ((ScreenViewport) ui.getViewport()).getUnitsPerPixel();
            windowScroll.setHeight(Math.min(guiLayout.getHeight(), ui.getHeight() - 120 * unitsPerPixel));
            windowScroll.pack();

            mainVertical.setHeight(windowScroll.getHeight() + 30 * unitsPerPixel);
            mainVertical.pack();

            setHeight(windowScroll.getHeight() + 40 * unitsPerPixel);
        }
        pack();
        validate();

        // Restore position
        setY(topy - getHeight());
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void setVisibilityToggles(ComponentType[] entities, boolean[] visible) {
        this.visibilityEntities = entities;
        this.visible = visible;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case GUI_SCROLL_POSITION_CMD -> this.windowScroll.setScrollY((float) data[0]);
            case GUI_FOLD_CMD -> {
                boolean collapse;
                if (data.length >= 1) {
                    collapse = (boolean) data[0];
                } else {
                    // Toggle
                    collapse = !isCollapsed();
                }
                if (collapse) {
                    collapse();
                } else {
                    expand();
                }
            }
            case GUI_MOVE_CMD -> {
                float x = (float) data[0];
                float y = (float) data[1];
                float width = Gdx.graphics.getWidth();
                float height = Gdx.graphics.getHeight();
                float windowWidth = getWidth();
                float windowHeight = getHeight();
                x = MathUtilsDouble.clamp(x * width, 0, width - windowWidth);
                y = MathUtilsDouble.clamp(y * height - windowHeight, 0, height - windowHeight);
                setPosition(Math.round(x), FastMath.round(y));
            }
            case RECALCULATE_CONTROLS_WINDOW_SIZE -> recalculateSize();
            case EXPAND_COLLAPSE_PANE_CMD ->  {
                String name = (String) data[0];
                if(panes.containsKey(name)) {
                    boolean expand = (Boolean) data[1];
                    CollapsiblePane pane = panes.get(name);
                    if(expand) {
                        pane.expandPane();
                    } else {
                        pane.collapsePane();
                    }
                }
            }
            case TOGGLE_EXPANDCOLLAPSE_PANE_CMD -> {
                String name = (String) data[0];
                CollapsiblePane pane = panes.get(name);
                pane.togglePane();
            }
            case MINIMAP_DISPLAY_CMD -> {
                boolean show = (Boolean) data[0];
                if (source != buttonMinimap) {
                    buttonMinimap.setCheckedNoFire(show);
                }
            }
            case MINIMAP_TOGGLE_CMD -> {
                buttonMinimap.setCheckedNoFire(!buttonMinimap.isChecked());
            }
            default -> {
            }
        }

    }

    public CollapsiblePane getCollapsiblePane(String name) {
        return panes.getOrDefault(name, null);
    }

}
