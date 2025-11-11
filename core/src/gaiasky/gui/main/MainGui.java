/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.api.IGuiInterface;
import gaiasky.gui.iface.*;
import gaiasky.gui.minimap.MinimapInterface;
import gaiasky.gui.window.*;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.view.FocusView;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.ProgramSettings.UpdateSettings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.FileChooser;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.update.VersionCheckEvent;
import gaiasky.util.update.VersionChecker;

import java.nio.file.Files;
import java.text.DecimalFormat;
import java.time.Instant;

import static gaiasky.event.Event.*;

/**
 * Aggregates and manages the main user interface mode, with all its sub-interfaces.
 */
public class MainGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(MainGui.class);
    private final GlobalResources globalResources;
    private final FocusView view;
    private CatalogManager catalogManager;
    protected ControlsWindow controlsWindow;
    protected ControlsInterface controlsInterface;
    protected Container<CameraInfoInterface> fi;
    protected Container<TopInfoInterface> ti;
    protected Container<NotificationsInterface> ni;
    protected CameraInfoInterface focusInterface;
    protected NotificationsInterface notificationsInterface;
    protected MessagesInterface messagesInterface;
    protected CustomInterface customInterface;
    protected RunStateInterface runStateInterface;
    protected TopInfoInterface topInfoInterface;
    protected PopupNotificationsInterface popupNotificationsInterface;
    protected MinimapInterface minimapInterface;
    protected MinimapWindow minimapWindow;
    protected ConsoleInterface consoleInterface;
    protected LoadProgressInterface loadProgressInterface;
    protected LocationInfoInterface locationInfoInterface;
    protected LogWindow logWindow;
    protected DataInfoWindow dataInfoWindow;
    protected ArchiveViewWindow archiveViewWindow;
    protected DecimalFormat nf;
    protected Label pointerXCoord, pointerYCoord;
    protected float pad, pad5;
    protected Scene scene;
    private ComponentType[] visibilityEntities;
    private boolean[] visible;

    /**
     * Creates a {@link MainGui} with the given skin, graphics instance, units per pixel, and global resources.
     *
     * @param skin            The {@link Skin} to use.
     * @param graphics        The {@link Graphics} instance.
     * @param unitsPerPixel   The units per pixel to use, as a floating point number.
     * @param globalResources Reference to the {@link GlobalResources} object.
     */
    public MainGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel, final GlobalResources globalResources) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        this.globalResources = globalResources;
        this.view = new FocusView();
    }

    @Override
    public void initialize(final AssetManager assetManager, final SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        this.stage = new Stage(vp, sb);
        vp.update(graphics.getWidth(), graphics.getHeight(), true);
    }

    public void initialize(Stage ui) {
        this.stage = ui;
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        interfaces = new Array<>();
        catalogManager = assetManager.get("gaiasky-assets", GaiaSkyAssets.class).catalogManager;

        buildGui();

        // We must subscribe to the desired events
        EventManager.instance.subscribe(this, FOV_CMD, UPDATE_DATA_INFO_CMD, SHOW_DATA_INFO_CMD,
                                        SHOW_ARCHIVE_VIEW_CMD, UPDATE_ARCHIVE_VIEW_CMD, SHOW_PLAYCAMERA_CMD, REMOVE_KEYBOARD_FOCUS_CMD,
                                        REMOVE_GUI_COMPONENT_CMD, ADD_GUI_COMPONENT_CMD, SHOW_LOG_CMD, RA_DEC_UPDATED, LON_LAT_UPDATED,
                                        CONTEXT_MENU_CMD, SHOW_LAND_AT_LOCATION_CMD, DISPLAY_POINTER_COORDS_CMD, MINIMAP_TOGGLE_CMD,
                                        MINIMAP_DISPLAY_CMD, SHOW_PROCEDURAL_GEN_CMD, SHOW_PROCEDURAL_GALAXY_CMD, CONSOLE_CMD);
    }

    protected void buildGui() {
        pad = 16f;
        pad5 = 8f;
        // Component types name init
        for (ComponentType ct : ComponentType.values()) {
            ct.getName();
        }
        nf = new DecimalFormat("##0.##");

        // NOTIFICATIONS INTERFACE - BOTTOM LEFT
        notificationsInterface = new NotificationsInterface(skin, lock, true, true, true, true);
        notificationsInterface.pad(pad5);
        ni = new Container<>(notificationsInterface);
        ni.setFillParent(true);
        ni.bottom().left();
        ni.pad(0, pad, pad, 0);
        interfaces.add(notificationsInterface);

        if (Settings.settings.program.ui.newUI) {
            // CONTROLS INTERFACE - TOP LEFT
            controlsInterface = new ControlsInterface(skin, stage, scene, catalogManager, visibilityEntities, visible);
            controlsInterface.setFillParent(true);
            controlsInterface.top().left();
            controlsInterface.pad(pad5);
            interfaces.add(controlsInterface);
        } else {
            // CONTROLS WINDOW
            addControlsWindow();
        }

        // FOCUS INFORMATION - BOTTOM RIGHT
        focusInterface = new CameraInfoInterface(skin);
        fi = new Container<>(focusInterface);
        fi.setFillParent(true);
        fi.bottom().right();
        fi.pad(0, 0, pad, pad);
        interfaces.add(focusInterface);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, 300f, 200f, 0);
        interfaces.add(messagesInterface);

        // TOP INFO - TOP CENTER
        topInfoInterface = new TopInfoInterface(skin, scene);
        topInfoInterface.top();
        topInfoInterface.pad(pad5, pad, pad5, pad);
        ti = new Container<>(topInfoInterface);
        ti.setFillParent(true);
        ti.top().left();
        ti.pad(pad);
        interfaces.add(topInfoInterface);

        // MINIMAP
        initializeMinimap(stage);

        // INPUT STATE
        runStateInterface = new RunStateInterface(skin, true);
        runStateInterface.setFillParent(true);
        runStateInterface.center().bottom();
        runStateInterface.pad(0, 0, pad, 0);
        interfaces.add(runStateInterface);

        // POPUP NOTIFICATIONS
        popupNotificationsInterface = new PopupNotificationsInterface(skin);
        popupNotificationsInterface.setFillParent(true);
        popupNotificationsInterface.right().top();
        interfaces.add(popupNotificationsInterface);

        // LOAD PROGRESS INTERFACE
        addLoadProgressInterface();

        // CUSTOM OBJECTS INTERFACE
        customInterface = new CustomInterface(stage, skin, lock);
        interfaces.add(customInterface);

        // CONSOLE INTERFACE
        consoleInterface = new ConsoleInterface(skin, GaiaSky.instance.getConsoleManager());
        consoleInterface.setFillParent(true);
        consoleInterface.bottom().left().padLeft(70f);

        // LOCATION INFO INTERFACE
        locationInfoInterface = new LocationInfoInterface(skin);
        interfaces.add(locationInfoInterface);

        // MOUSE X/Y COORDINATES
        pointerXCoord = new OwnLabel("", skin, "default");
        pointerXCoord.setAlignment(Align.bottom);
        pointerXCoord.setVisible(Settings.settings.program.pointer.coordinates);
        pointerYCoord = new OwnLabel("", skin, "default");
        pointerYCoord.setAlignment(Align.right | Align.center);
        pointerYCoord.setVisible(Settings.settings.program.pointer.coordinates);

        /* ADD TO UI */
        rebuildGui();

        /* VERSION CHECK */
        if (Settings.settings.program.update.lastCheck == null
                || (Instant.now().toEpochMilli() -
                Settings.settings.program.update.lastCheck.toEpochMilli() > UpdateSettings.VERSION_CHECK_INTERVAL_MS)) {
            // Start check for new versions.
            final Timer.Task t = getVersionCheckTask();
            Timer.schedule(t, 10);
        }

    }

    private Timer.Task getVersionCheckTask() {
        VersionChecker vc = new VersionChecker(Settings.settings.program.url.versionCheck);
        vc.setListener(event -> {
            if (event instanceof VersionCheckEvent vce) {
                if (!vce.isFailed()) {
                    // Check version
                    String tagVersion = vce.getTag();
                    Integer versionNumber = vce.getVersionNumber();

                    Settings.settings.program.update.lastCheck = Instant.now();

                    if (versionNumber > Settings.settings.version.versionNumber) {
                        logger.info(I18n.msg("gui.newversion.available", Settings.settings.version.version, tagVersion));
                        // There's a new version!
                        UpdatePopup newVersion = new UpdatePopup(tagVersion, stage, skin);
                        newVersion.pack();
                        float ww = newVersion.getWidth();
                        float margin = 8f;
                        newVersion.setPosition(graphics.getWidth() - ww - margin, margin);
                        stage.addActor(newVersion);
                    } else {
                        // No new version
                        logger.info(I18n.msg("gui.newversion.nonew", Settings.settings.program.update.getLastCheckedString()));
                    }

                } else {
                    // Handle failed case
                    // Do nothing
                    logger.info(I18n.msg("gui.newversion.fail"));
                }
            }
            return false;
        });

        // Start in 10 seconds
        Thread vct = new Thread(vc);
        return new Timer.Task() {
            @Override
            public void run() {
                logger.info(I18n.msg("gui.newversion.checking"));
                vct.start();
            }
        };
    }

    protected void rebuildGui() {
        if (stage != null) {
            stage.clear();
            boolean collapsed;
            if (controlsWindow != null) {
                collapsed = controlsWindow.isCollapsed();
                controlsWindow.recalculateSize();
                if (collapsed)
                    controlsWindow.collapseInstant();
                controlsWindow.setPosition(0, graphics.getHeight() * unitsPerPixel - controlsWindow.getHeight());
                stage.addActor(controlsWindow);
            }
            if (ni != null) {
                stage.addActor(ni);
            }
            if (controlsInterface != null) {
                stage.addActor(controlsInterface);
            }
            if (messagesInterface != null) {
                stage.addActor(messagesInterface);
            }
            if (fi != null) {
                stage.addActor(fi);
            }
            if (runStateInterface != null) {
                stage.addActor(runStateInterface);
            }
            if (ti != null) {
                stage.addActor(ti);
            }
            if (minimapInterface != null) {
                stage.addActor(minimapInterface);
            }
            if (loadProgressInterface != null) {
                stage.addActor(loadProgressInterface);
            }
            if (locationInfoInterface != null) {
                stage.addActor(locationInfoInterface);
            }
            if (pointerXCoord != null && pointerYCoord != null) {
                stage.addActor(pointerXCoord);
                stage.addActor(pointerYCoord);
            }
            if (customInterface != null) {
                customInterface.reAddObjects();
            }
            if (popupNotificationsInterface != null) {
                stage.addActor(popupNotificationsInterface);
            }

            /* CAPTURE SCROLL FOCUS */
            stage.addListener(event -> {
                if (event instanceof InputEvent ie) {

                    if (ie.getType() == Type.mouseMoved) {
                        Actor scrollPanelAncestor = GuiUtils.getScrollPaneAncestor(ie.getTarget());
                        stage.setScrollFocus(scrollPanelAncestor);
                    } else if (ie.getType() == Type.touchDown) {
                        if (ie.getTarget() instanceof TextField)
                            stage.setKeyboardFocus(ie.getTarget());
                    }
                }
                return false;
            });

            /* KEYBOARD FOCUS */
            stage.addListener((event) -> {
                if (event instanceof InputEvent ie) {
                    if (ie.getType() == Type.touchDown && !ie.isHandled()) {
                        stage.setKeyboardFocus(null);
                    }
                }
                return false;
            });
        }
    }

    /**
     * Removes the focus from this Gui and returns true if the focus was in the
     * GUI, false otherwise.
     *
     * @return true if the focus was in the GUI, false otherwise.
     */
    public boolean cancelTouchFocus() {
        if (stage.getScrollFocus() != null) {
            stage.setScrollFocus(null);
            stage.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public void update(double dt) {
        stage.act((float) dt);
        for (IGuiInterface i : interfaces) {
            if (i.isOn())
                i.update();
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case SHOW_PROCEDURAL_GEN_CMD -> {
                var planet = (FocusView) data[0];

                // Check for cubemap textures, for they are incompatible with the procedural generation.
                if (Mapper.model.has(planet.getEntity())) {
                    var w = findActor("procedural-window");
                    // Only one instance
                    if (w != null && w.hasParent()) {
                        if (!w.isVisible())
                            w.setVisible(true);
                    } else {
                        ProceduralGenerationWindow proceduralWindow = new ProceduralGenerationWindow(planet, stage, skin);
                        proceduralWindow.setName("procedural-window");
                        proceduralWindow.show(stage);
                    }
                }

            }
            case SHOW_PROCEDURAL_GALAXY_CMD -> {
                var focus = (FocusView) data[0];

                if (focus == null || !focus.isValid() || !focus.isBillboardDataset()) {
                    focus = null;
                }
                var w = findActor("procedural-gal-window");
                // Only one instance
                if (w != null && w.hasParent() && w instanceof GalaxyGenerationWindow ggw) {
                    if (!w.isVisible()) {
                        ggw.reinitialize(focus);
                        w.setVisible(true);
                    }
                } else {
                    GalaxyGenerationWindow window = new GalaxyGenerationWindow(focus, scene, stage, skin);
                    window.setName("procedural-gal-window");
                    window.show(stage);
                }
            }
            case SHOW_LAND_AT_LOCATION_CMD -> {
                var target = (FocusView) data[0];
                var landAtLocation = new LandAtWindow(target.getEntity(), stage, skin);
                landAtLocation.show(stage);
            }
            case SHOW_PLAYCAMERA_CMD -> {
                var fc = new FileChooser(I18n.msg("gui.camera.title"),
                                         skin,
                                         stage,
                                         SysUtils.getDefaultCameraDir(),
                                         FileChooser.FileChooserTarget.FILES);
                fc.setShowHidden(Settings.settings.program.fileChooser.showHidden);
                fc.setShowHiddenConsumer((showHidden) -> Settings.settings.program.fileChooser.showHidden = showHidden);
                fc.setAcceptText(I18n.msg("gui.camera.run"));
                fc.setFileFilter(pathname -> pathname.getFileName().toString().endsWith(".dat") || pathname.getFileName()
                        .toString()
                        .endsWith(".gsc"));
                fc.setAcceptedFiles("*.dat, *.gsc");
                fc.setResultListener((success, result) -> {
                    if (success) {
                        if (Files.exists(result) && Files.exists(result)) {
                            EventManager.publish(PLAY_CAMERA_CMD, fc, true, result);
                            return true;
                        } else {
                            logger.error("Selection must be a file: " + result.toAbsolutePath());
                        }
                    }
                    return false;
                });
                fc.show(stage);
            }
            case SHOW_LOG_CMD -> {
                if (logWindow == null) {
                    logWindow = new LogWindow(stage, skin);
                }
                logWindow.update();
                if (!logWindow.isVisible() || !logWindow.hasParent())
                    logWindow.show(stage);
            }
            case UPDATE_DATA_INFO_CMD -> {
                if (dataInfoWindow != null && dataInfoWindow.isVisible() && dataInfoWindow.hasParent()) {
                    var object = (FocusView) data[0];
                    dataInfoWindow.update(object);
                }
            }
            case SHOW_DATA_INFO_CMD -> {
                var object = (FocusView) data[0];
                if (dataInfoWindow == null) {
                    dataInfoWindow = new DataInfoWindow(stage, skin);
                }
                dataInfoWindow.update(object);
                if (!dataInfoWindow.isVisible() || !dataInfoWindow.hasParent())
                    dataInfoWindow.show(stage);
            }
            case UPDATE_ARCHIVE_VIEW_CMD -> {
                if (archiveViewWindow != null && archiveViewWindow.isVisible() && archiveViewWindow.hasParent()) {
                    // Update
                    var starFocus = (FocusView) data[0];
                    archiveViewWindow.update(starFocus);
                }
            }
            case SHOW_ARCHIVE_VIEW_CMD -> {
                var starFocus = (FocusView) data[0];
                if (archiveViewWindow == null) {
                    archiveViewWindow = new ArchiveViewWindow(stage, skin);
                }
                archiveViewWindow.update(starFocus);
                if (!archiveViewWindow.isVisible() || !archiveViewWindow.hasParent())
                    archiveViewWindow.show(stage);
            }
            case REMOVE_KEYBOARD_FOCUS_CMD -> stage.setKeyboardFocus(null);
            case REMOVE_GUI_COMPONENT_CMD -> {
                var name = (String) data[0];
                var methodName = "remove" + TextUtils.capitalise(name);
                try {
                    var method = ClassReflection.getMethod(this.getClass(), methodName);
                    method.invoke(this);
                } catch (ReflectionException e) {
                    logger.error(e);
                }
                rebuildGui();
            }
            case ADD_GUI_COMPONENT_CMD -> {
                var name = (String) data[0];
                var methodName = "add" + TextUtils.capitalise(name);
                try {
                    Method method = ClassReflection.getMethod(this.getClass(), methodName);
                    method.invoke(this);
                } catch (ReflectionException e) {
                    logger.error(e);
                }
                rebuildGui();
            }
            case RA_DEC_UPDATED -> {
                if (Settings.settings.program.pointer.coordinates) {
                    Stage ui = pointerYCoord.getStage();
                    float uiScale = Settings.settings.program.ui.scale;
                    var ra = (Double) data[0];
                    var dec = (Double) data[1];
                    var x = (Integer) data[4];
                    var y = (Integer) data[5];

                    pointerXCoord.setText(I18n.msg("gui.focusinfo.pointer.ra", nf.format(ra)));
                    pointerXCoord.setPosition(x / uiScale, 1.6f);
                    pointerYCoord.setText(I18n.msg("gui.focusinfo.pointer.dec", nf.format(dec)));
                    pointerYCoord.setPosition(ui.getWidth() + 1.6f, ui.getHeight() - y / uiScale);
                }
            }
            case LON_LAT_UPDATED -> {
                if (Settings.settings.program.pointer.coordinates) {
                    var ui = pointerYCoord.getStage();
                    var uiScale = Settings.settings.program.ui.scale;
                    var lon = (Double) data[0];
                    var lat = (Double) data[1];
                    var x = (Integer) data[2];
                    var y = (Integer) data[3];

                    pointerXCoord.setText(I18n.msg("gui.focusinfo.pointer.lon", nf.format(lon)));
                    pointerXCoord.setPosition(x / uiScale, 1.6f);
                    pointerYCoord.setText(I18n.msg("gui.focusinfo.pointer.lat", nf.format(lat)));
                    pointerYCoord.setPosition(ui.getWidth() + 1.6f, ui.getHeight() - y / uiScale);
                }
            }
            case DISPLAY_POINTER_COORDS_CMD -> {
                Boolean display = (Boolean) data[0];
                pointerXCoord.setVisible(display);
                pointerYCoord.setVisible(display);
            }
            case CONTEXT_MENU_CMD -> {
                final var candidate = (Entity) data[0];
                var screenX = (int) data[1];
                var screenY = (int) data[2];
                FocusView focusView = null;
                if (candidate != null) {
                    view.setEntity(candidate);
                    focusView = view;
                }
                SceneContextMenu popup = new SceneContextMenu(skin, "default", screenX, screenY, focusView, catalogManager, scene);
                var h = (int) getGuiStage().getHeight();
                float px = screenX * unitsPerPixel;
                float py = h - screenY * unitsPerPixel - 32f;
                popup.showMenu(stage, px, py);
            }
            case MINIMAP_TOGGLE_CMD -> {
                if (Settings.settings.program.minimap.inWindow) {
                    toggleMinimapWindow(stage);
                } else {
                    toggleMinimapInterface(stage);
                }
            }
            case MINIMAP_DISPLAY_CMD -> {
                var show = (Boolean) data[0];
                if (Settings.settings.program.minimap.inWindow) {
                    showMinimapWindow(stage, show);
                } else {
                    showMinimapInterface(stage, show);
                }
            }
            case CONSOLE_CMD -> {
                boolean show;
                if (data != null && data.length > 0) {
                    show = (Boolean) data[0];
                } else {
                    show = !consoleInterface.hasParent();
                }
                if (show) {
                    interfaces.add(consoleInterface);
                    // Add to ui.
                    if (!consoleInterface.hasParent() || consoleInterface.getParent() != stage.getRoot()) {
                        stage.addActor(consoleInterface);
                        consoleInterface.showConsole();

                    }
                } else if (consoleInterface != null) {
                    interfaces.removeValue(consoleInterface, true);
                    // Remove from ui.
                    consoleInterface.closeConsole();
                }
            }
            default -> {
            }
        }
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible) {
        this.visibilityEntities = entities;
        ComponentType[] values = ComponentType.values();
        this.visible = new boolean[values.length];
        for (int i = 0; i < values.length; i++)
            this.visible[i] = visible.get(values[i].ordinal());
    }

    public void addControlsWindow() {
        controlsWindow = new ControlsWindow(Settings.getSuperShortApplicationName(), skin, stage, catalogManager);
        controlsWindow.setScene(scene);
        controlsWindow.setVisibilityToggles(visibilityEntities, visible);
        controlsWindow.initialize();
        controlsWindow.left();
        controlsWindow.getTitleTable().align(Align.left);
        controlsWindow.setFillParent(false);
        controlsWindow.setMovable(true);
        controlsWindow.setResizable(false);
        controlsWindow.padRight(5);
        controlsWindow.padBottom(5);

        controlsWindow.collapseInstant();
    }

    public void initializeMinimap(Stage ui) {
        if (Settings.settings.program.minimap.active) {
            if (Settings.settings.program.minimap.inWindow) {
                showMinimapWindow(ui, true);
            } else {
                if (minimapInterface == null) {
                    minimapInterface = new MinimapInterface(skin, globalResources.getShapeShader(), globalResources.getSpriteShader());
                    minimapInterface.setFillParent(true);
                    minimapInterface.right().top();
                    minimapInterface.pad(pad, 0f, 0f, pad);
                    interfaces.add(minimapInterface);
                }
            }
        }
    }

    public void showMinimapInterface(Stage ui, boolean show) {
        if (show && minimapInterface == null) {
            minimapInterface = new MinimapInterface(skin, globalResources.getShapeShader(), globalResources.getSpriteShader());
            minimapInterface.setFillParent(true);
            minimapInterface.right().top();
            minimapInterface.pad(pad, 0f, 0f, pad);
            interfaces.add(minimapInterface);
        }
        if (show) {
            // Add to ui.
            if (!minimapInterface.hasParent() || minimapInterface.getParent() != ui.getRoot()) {
                ui.addActor(minimapInterface);
                minimapInterface.addAction(
                        Actions.sequence(
                                Actions.alpha(0f),
                                Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds())));

            }
        } else if (minimapInterface != null) {
            // Remove from ui.
            minimapInterface.addAction(
                    Actions.sequence(
                            Actions.alpha(1f),
                            Actions.fadeOut(Settings.settings.program.ui.getAnimationSeconds()),
                            Actions.run(() -> minimapInterface.remove())));
        }
    }

    public void addLoadProgressInterface() {
        loadProgressInterface = new LoadProgressInterface(400f, skin);
        loadProgressInterface.setFillParent(true);
        loadProgressInterface.center().bottom();
        loadProgressInterface.pad(0, 0, 0, 0);
        interfaces.add(loadProgressInterface);
    }

    public void toggleMinimapInterface(Stage stage) {
        showMinimapInterface(stage, minimapInterface == null || (!minimapInterface.isVisible() || !minimapInterface.hasParent()));
    }

    public void showMinimapWindow(Stage stage, boolean show) {
        if (show && minimapWindow == null)
            minimapWindow = new MinimapWindow(stage, skin, globalResources.getShapeShader(), globalResources.getSpriteShader());
        if (show)
            minimapWindow.show(stage, graphics.getWidth() - minimapWindow.getWidth(), graphics.getHeight() - minimapWindow.getHeight());
        else if (minimapWindow != null)
            minimapWindow.hide();
    }

    public void toggleMinimapWindow(Stage ui) {
        showMinimapWindow(ui, minimapWindow == null || (!minimapWindow.isVisible() || !minimapWindow.hasParent()));
    }

    @Override
    public boolean updateUnitsPerPixel(float upp) {
        boolean cool = super.updateUnitsPerPixel(upp);
        if (cool) {
            if (controlsWindow != null) {
                controlsWindow.setPosition(0, graphics.getHeight() * unitsPerPixel - controlsWindow.getHeight());
                controlsWindow.recalculateSize();
                if (stage.getHeight() < controlsWindow.getHeight()) {
                    // Collapse
                    controlsWindow.collapseInstant();
                }
            }
        }
        return cool;
    }

}
