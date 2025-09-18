/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.BookmarkInfoDialog;
import gaiasky.gui.api.IGui;
import gaiasky.gui.bookmarks.BookmarkNameDialog;
import gaiasky.gui.bookmarks.BookmarksManager;
import gaiasky.gui.datasets.DatasetLoadDialog;
import gaiasky.gui.window.*;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Scene;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.view.FocusView;
import gaiasky.util.*;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.i18n.I18n;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.FileChooser;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextButton;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Keeps track of and manages the active user interfaces ({@link IGui} instances). It also serves and implements
 * most of the GUI-related action events.
 */
public class GuiRegistry implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(GuiRegistry.class);
    /**
     * Scene reference.
     **/
    protected final Scene scene;
    /**
     * Registered GUI array.
     **/
    private final Array<IGui> guis;
    /**
     * GUIs not affected by the clean mode.
     */
    private final Array<IGui> specialGuis;
    /**
     * Render lock object.
     */
    private final Object renderLock = new Object();
    /**
     * The catalog manager.
     */
    private final CatalogManager catalogManager;
    private final FocusView view;
    /**
     * Mode change info popup.
     */
    public Table modeChangeInfoPopup;
    /**
     * Current GUI object.
     **/
    public IGui current;
    /**
     * Previous GUI object, if any.
     **/
    public IGui previous;
    private Skin skin;
    private PreferencesWindow preferencesWindow;
    private AboutWindow aboutWindow;
    private SearchDialog searchDialog;
    private DateDialog dateDialog;
    private BookmarkNameDialog locationBookmarkDialog;
    private BookmarkInfoDialog bookmarkInfoDialog;
    private ObjectDebugWindow objectDebugWindow;
    /**
     * Keyframes window.
     **/
    private KeyframesWindow keyframesWindow;
    /**
     * Individual visibility.
     */
    private IndividualVisibilityWindow indVisWindow;
    /**
     * Task to remove the information pop-up.
     **/
    private Task removePopup;
    /**
     * Last open location.
     */
    private Path lastOpenLocation;
    /* Slave config window. */
    private SlaveConfigWindow slaveConfigWindow;
    /**
     * Global input multiplexer.
     **/
    private InputMultiplexer inputMultiplexer = null;

    /**
     * Create new GUI registry object.
     */
    public GuiRegistry(final Skin skin,
                       final Scene scene,
                       final CatalogManager catalogManager) {
        super();
        this.skin = skin;
        this.scene = scene;
        this.guis = new Array<>(true, 2);
        this.specialGuis = new Array<>(true, 1);
        this.catalogManager = catalogManager;
        this.view = new FocusView();
        // Windows which are visible from any GUI.
        EventManager.instance.subscribe(this,
                                        Event.SHOW_SEARCH_ACTION,
                                        Event.SHOW_QUIT_ACTION,
                                        Event.SHOW_ABOUT_ACTION,
                                        Event.SHOW_LOAD_CATALOG_ACTION,
                                        Event.SHOW_PREFERENCES_ACTION,
                                        Event.SHOW_KEYFRAMES_WINDOW_ACTION,
                                        Event.SHOW_SLAVE_CONFIG_ACTION,
                                        Event.SHOW_TEXTURE_WINDOW_ACTION,
                                        Event.UI_THEME_RELOAD_INFO,
                                        Event.MODE_POPUP_CMD,
                                        Event.DISPLAY_GUI_CMD,
                                        Event.CAMERA_MODE_CMD,
                                        Event.UI_RELOAD_CMD,
                                        Event.SHOW_PER_OBJECT_VISIBILITY_ACTION,
                                        Event.SHOW_RESTART_ACTION,
                                        Event.CLOSE_ALL_GUI_WINDOWS_CMD,
                                        Event.SHOW_DATE_TIME_EDIT_ACTION,
                                        Event.SHOW_ADD_POSITION_BOOKMARK_ACTION,
                                        Event.SHOW_BOOKMARK_INFO_ACTION,
                                        Event.SHOW_OBJECT_DEBUG_ACTION);
    }

    public void setInputMultiplexer(InputMultiplexer inputMultiplexer) {
        this.inputMultiplexer = inputMultiplexer;
    }

    /**
     * Switches the current GUI with the given one, updating the processors.
     * It also sets the previous GUI to the given value.
     *
     * @param gui      The new GUI.
     * @param previous The new previous GUI.
     */
    public void change(IGui gui,
                       IGui previous) {
        if (current != gui) {
            unset(previous);
            set(gui);
        }
    }

    /**
     * Switches the current GUI with the given one, updating the processors.
     *
     * @param gui The new gui.
     */
    public void change(IGui gui) {
        if (current != gui) {
            unset();
            set(gui);
        }
    }

    /**
     * Unsets the current GUI and sets it as previous.
     */
    public void unset() {
        unset(current);
    }

    /**
     * Unsets the given GUI and sets it as previous.
     *
     * @param gui The GUI.
     */
    public void unset(IGui gui) {
        if (gui != null) {
            unregisterGui(gui);
            inputMultiplexer.removeProcessor(gui.getGuiStage());
        }
        previous = gui;
    }

    /**
     * Sets the given GUI as current.
     *
     * @param gui The new GUI.
     */
    public void set(IGui gui) {
        if (gui != null) {
            registerGui(gui);
            inputMultiplexer.addProcessor(0, gui.getGuiStage());
        }
        current = gui;
    }

    /**
     * Sets the given GUI as previous.
     *
     * @param gui The new previous GUI.
     */
    public void setPrevious(IGui gui) {
        previous = gui;
    }

    /**
     * Registers a new GUI.
     *
     * @param gui The GUI to register.
     */
    public void registerGui(IGui gui) {
        if (!guis.contains(gui, true)) {
            guis.add(gui);
        }
    }

    public void registerSpecialGui(IGui gui) {
        if (!specialGuis.contains(gui, true)) {
            specialGuis.add(gui);
        }
    }

    /**
     * Unregisters a GUI.
     *
     * @param gui The GUI to unregister.
     */
    public void unregisterGui(IGui gui) {
        guis.removeValue(gui, true);
    }

    public void unregisterSpecialGui(IGui gui) {
        specialGuis.removeValue(gui, true);
    }

    /**
     * Unregisters all GUIs.
     */
    public void unregisterAll() {
        guis.clear();
    }

    /**
     * Renders the registered GUIs.
     *
     * @param rw The render width.
     * @param rh The render height.
     */
    public void render(int rw,
                       int rh) {
        if (Settings.settings.runtime.displayGui) {
            synchronized (renderLock) {
                for (int i = 0; i < guis.size; i++) {
                    guis.get(i).getGuiStage().getViewport().apply();
                    try {
                        guis.get(i).render(rw, rh);
                    } catch (Exception ignored) {
                    }
                }
            }
        } else if (Settings.settings.program.displayTimeNoUi) {
            synchronized (renderLock) {
                for (int i = 0; i < specialGuis.size; i++) {
                    specialGuis.get(i).getGuiStage().getViewport().apply();
                    try {
                        specialGuis.get(i).render(rw, rh);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * Adds the stage of the given GUI to the processors in
     * the input multiplexer.
     *
     * @param gui The gui.
     */
    public void addProcessor(IGui gui) {
        if (inputMultiplexer != null && gui != null)
            inputMultiplexer.addProcessor(gui.getGuiStage());
    }

    public void removeProcessor(IGui gui) {
        if (inputMultiplexer != null && gui != null)
            inputMultiplexer.removeProcessor(gui.getGuiStage());
    }

    /**
     * Updates the registered GUIs.
     *
     * @param dt The delta time in seconds.
     */
    public void update(double dt) {
        for (IGui gui : guis) {
            gui.update(dt);
        }
        // Only update special when needed.
        if (!Settings.settings.runtime.displayGui && Settings.settings.program.displayTimeNoUi) {
            for (IGui gui : specialGuis) {
                gui.update(dt);
            }
        }
    }

    public void publishReleaseNotes() {
        // Check release notes if needed.
        Path releaseNotesRev = SysUtils.getReleaseNotesRevisionFile();
        int releaseNotesVersion = 0;
        if (Files.exists(releaseNotesRev) && Files.isRegularFile(releaseNotesRev)) {
            try {
                String contents = Files.readString(releaseNotesRev).trim();
                releaseNotesVersion = Parser.parseInt(contents);
            } catch (Exception e) {
                logger.warn(I18n.msg("error.file.read", releaseNotesRev.toString()));
            }
        }

        if (releaseNotesVersion < Settings.settings.version.versionNumber) {
            Path releaseNotesFile = SysUtils.getReleaseNotesFile();
            if (Files.exists(releaseNotesFile)) {
                final Task releaseNotesTask = new Task() {
                    @Override
                    public void run() {
                        Stage ui = current.getGuiStage();
                        ReleaseNotesWindow releaseNotesWindow = new ReleaseNotesWindow(ui, skin, releaseNotesFile);
                        releaseNotesWindow.show(ui);
                    }
                };
                Timer.schedule(releaseNotesTask, 3);
            }
        }
    }

    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
        if (searchDialog != null)
            searchDialog.dispose();
    }

    @Override
    public void notify(final Event event,
                       Object source,
                       final Object... data) {
        if (current != null) {
            Stage stage = current.getGuiStage();
            // Treats windows that can appear in any GUI.
            switch (event) {
                case SHOW_SEARCH_ACTION -> {
                    if (searchDialog == null) {
                        searchDialog = new SearchDialog(skin, stage, scene, true);
                    } else {
                        searchDialog.clearText();
                    }
                    if (!searchDialog.isVisible() | !searchDialog.hasParent())
                        searchDialog.show(stage);
                }
                case SHOW_QUIT_ACTION -> {
                    if (!removeModeChangePopup() && !removeGamepadGui()) {
                        if (GLFW.glfwGetInputMode(((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle(),
                                                  GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_DISABLED) {
                            // Release mouse if captured.
                            GLFW.glfwSetInputMode(((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle(),
                                                  GLFW.GLFW_CURSOR,
                                                  GLFW.GLFW_CURSOR_NORMAL);
                        } else {
                            Runnable quitRunnable = data.length > 0 ? (Runnable) data[0] : null;
                            if (Settings.settings.program.exitConfirmation) {
                                QuitWindow quit = new QuitWindow(stage, skin);
                                if (data.length > 0) {
                                    quit.setAcceptListener(quitRunnable);
                                }
                                quit.show(stage);
                            } else {
                                if (quitRunnable != null)
                                    quitRunnable.run();
                                GaiaSky.postRunnable(() -> Gdx.app.exit());
                            }
                        }
                    }
                }
                case CAMERA_MODE_CMD -> removeModeChangePopup();
                case SHOW_ABOUT_ACTION -> {
                    if (aboutWindow == null) {
                        aboutWindow = new AboutWindow(stage, skin);
                    }
                    if (!aboutWindow.isVisible() || !aboutWindow.hasParent()) {
                        aboutWindow.show(stage);
                    }
                }
                case SHOW_PREFERENCES_ACTION -> {
                    Array<Actor> prefs = getPreferencesWindows();
                    if (prefs.isEmpty()) {
                        if (preferencesWindow != null) {
                            preferencesWindow.dispose();
                            preferencesWindow = null;
                        }
                        preferencesWindow = new PreferencesWindow(stage, skin, GaiaSky.instance.getGlobalResources());
                        if (!preferencesWindow.isVisible() || !preferencesWindow.hasParent()) {
                            preferencesWindow.show(stage);
                        }
                    } else {
                        // Close current windows.
                        for (Actor pref : prefs) {
                            if (pref instanceof PreferencesWindow) {
                                ((PreferencesWindow) pref).cancel();
                                ((PreferencesWindow) pref).hide();
                            }
                        }
                    }
                }
                case SHOW_PER_OBJECT_VISIBILITY_ACTION -> {
                    if (indVisWindow == null) {
                        indVisWindow = new IndividualVisibilityWindow(scene, stage, skin);
                    }
                    if (!indVisWindow.isVisible() || !indVisWindow.hasParent())
                        indVisWindow.show(stage);
                }
                case SHOW_SLAVE_CONFIG_ACTION -> {
                    if (MasterManager.hasSlaves()) {
                        if (slaveConfigWindow == null) {
                            slaveConfigWindow = new SlaveConfigWindow(stage, skin);
                        }
                        if (!slaveConfigWindow.isVisible() || !slaveConfigWindow.hasParent()) {
                            slaveConfigWindow.show(stage);
                        }
                    }
                }
                case SHOW_LOAD_CATALOG_ACTION -> {
                    if (lastOpenLocation == null && Settings.settings.program.fileChooser.lastLocation != null
                            && !Settings.settings.program.fileChooser.lastLocation.isEmpty()) {
                        try {
                            lastOpenLocation = Paths.get(Settings.settings.program.fileChooser.lastLocation);
                        } catch (Exception e) {
                            lastOpenLocation = null;
                        }
                    }
                    if (lastOpenLocation == null) {
                        lastOpenLocation = SysUtils.getUserHome();
                    } else if (!Files.exists(lastOpenLocation) || !Files.isDirectory(lastOpenLocation)) {
                        lastOpenLocation = SysUtils.getHomeDir();
                    }
                    FileChooser fc = new FileChooser(I18n.msg("gui.loadcatalog"), skin, stage, lastOpenLocation, FileChooser.FileChooserTarget.FILES);
                    fc.setShowHidden(Settings.settings.program.fileChooser.showHidden);
                    fc.setShowHiddenConsumer((showHidden) -> Settings.settings.program.fileChooser.showHidden = showHidden);
                    fc.setAcceptText(I18n.msg("gui.loadcatalog"));
                    fc.setFileFilter(pathname -> pathname.getFileName().toString().endsWith(".vot") || pathname.getFileName()
                            .toString()
                            .endsWith(".csv")
                            || pathname.getFileName().toString().endsWith(".fits") || pathname.getFileName().toString().endsWith(".json"));
                    fc.setAcceptedFiles("*.vot, *.csv, *.fits, *.json");
                    fc.setResultListener((success, result) -> {
                        if (success) {
                            if (Files.exists(result) && Files.exists(result)) {
                                // Load selected file.
                                try {
                                    String fileName = result.getFileName().toString();
                                    if (fileName.endsWith(".json")) {
                                        // Load internal JSON catalog file.
                                        GaiaSky.instance.getExecutorService().execute(() -> {
                                            var loaded = GaiaSky.instance.scripting().loadJsonCatalog(fileName, result.toAbsolutePath().toString());
                                            if (!loaded) {
                                                logger.warn("The dataset could not be loaded: " + result.toAbsolutePath());
                                            }
                                        });
                                    } else {
                                        final DatasetLoadDialog dld = new DatasetLoadDialog(I18n.msg("gui.dsload.title") + ": " + fileName,
                                                                                            fileName,
                                                                                            skin,
                                                                                            stage);
                                        Runnable doLoad = () -> GaiaSky.instance.getExecutorService().execute(() -> {
                                            DatasetOptions datasetOptions = dld.generateDatasetOptions();
                                            // Load dataset.
                                            GaiaSky.instance.scripting()
                                                    .loadDataset(datasetOptions.catalogName,
                                                                 result.toAbsolutePath().toString(),
                                                                 CatalogInfoSource.UI,
                                                                 datasetOptions,
                                                                 true);
                                            // Select first.
                                            CatalogInfo ci = this.catalogManager.get(datasetOptions.catalogName);
                                            if (datasetOptions.type.isSelectable() && ci != null && ci.entity != null) {
                                                view.setEntity(ci.entity);
                                                if (view.isSet()) {
                                                    var set = view.getSet();
                                                    if (set.data() != null && !set.data().isEmpty() && EntityUtils.isVisibilityOn(ci.entity)) {
                                                        EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
                                                        EventManager.publish(Event.FOCUS_CHANGE_CMD, this, set.getFirstParticleName());
                                                    }
                                                } else if (view.getGraph().children != null && !view.getGraph().children.isEmpty() && EntityUtils.isVisibilityOn(
                                                        view.getGraph().children.get(0))) {
                                                    EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
                                                    EventManager.publish(Event.FOCUS_CHANGE_CMD,
                                                                         this,
                                                                         EntityUtils.isVisibilityOn(view.getGraph().children.get(0)));
                                                }
                                                // Open UI datasets.
                                                GaiaSky.instance.scripting().expandUIPane("Datasets");
                                            } else {
                                                logger.info("No data loaded (did the load crash?)");
                                            }
                                        });
                                        dld.setAcceptListener(doLoad);
                                        dld.show(stage);
                                    }

                                    lastOpenLocation = result.getParent();
                                    Settings.settings.program.fileChooser.lastLocation = lastOpenLocation.toAbsolutePath().toString();
                                    return true;
                                } catch (Exception e) {
                                    logger.error(I18n.msg("notif.error", result.getFileName()), e);
                                    return false;
                                }

                            } else {
                                logger.error("Selection must be a file: " + result.toAbsolutePath());
                                return false;
                            }
                        } else {
                            // Still, update last location.
                            if (!Files.isDirectory(result)) {
                                lastOpenLocation = result.getParent();
                            } else {
                                lastOpenLocation = result;
                            }
                            Settings.settings.program.fileChooser.lastLocation = lastOpenLocation.toAbsolutePath().toString();
                        }
                        return false;
                    });
                    fc.show(stage);
                }
                case SHOW_KEYFRAMES_WINDOW_ACTION -> {
                    if (keyframesWindow == null) {
                        keyframesWindow = new KeyframesWindow(scene, stage, skin);
                    }
                    if (!keyframesWindow.isVisible() || !keyframesWindow.hasParent()) {
                        keyframesWindow.reset();
                        keyframesWindow.show(stage, 0, 0);
                    }
                    if (!GaiaSky.instance.isOn(ComponentType.Others)) {
                        // Notify that the user needs to enable 'others'.
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.keyframe.ct"), 10f);
                    }
                }
                case SHOW_TEXTURE_WINDOW_ACTION -> {
                    TextureWindow textureWindow = getTextureWindow(data, stage);
                    textureWindow.show(stage, 0, 50);
                }
                case SHOW_DATE_TIME_EDIT_ACTION -> {
                    if (dateDialog == null) {
                        dateDialog = new DateDialog(stage, skin);
                    }
                    dateDialog.updateTime(GaiaSky.instance.time.getTime(), Settings.settings.program.timeZone.getTimeZone());
                    dateDialog.show(stage);
                }
                case SHOW_OBJECT_DEBUG_ACTION -> {
                    if (objectDebugWindow == null) {
                        objectDebugWindow = new ObjectDebugWindow(stage, skin, scene);
                    }
                    objectDebugWindow.show(stage);
                }
                case SHOW_ADD_POSITION_BOOKMARK_ACTION -> {
                    if (locationBookmarkDialog == null) {
                        locationBookmarkDialog = new BookmarkNameDialog(stage, skin);
                    }
                    locationBookmarkDialog.resetName();
                    locationBookmarkDialog.show(stage);
                }
                case SHOW_BOOKMARK_INFO_ACTION -> {
                    if (bookmarkInfoDialog == null) {
                        bookmarkInfoDialog = new BookmarkInfoDialog(stage, skin);
                    }
                    var bookmark = (BookmarksManager.BookmarkNode) data[0];
                    bookmarkInfoDialog.updateView(bookmark);
                    bookmarkInfoDialog.show(stage);

                }
                case UI_THEME_RELOAD_INFO -> {
                    if (keyframesWindow != null) {
                        keyframesWindow.dispose();
                        keyframesWindow = null;
                    }
                    this.skin = (Skin) data[0];
                }
                case MODE_POPUP_CMD -> {
                    if (Settings.settings.runtime.displayGui && Settings.settings.program.ui.modeChangeInfo) {
                        ModePopupInfo mpi = (ModePopupInfo) data[0];
                        String name = (String) data[1];

                        if (mpi != null) {
                            // Add
                            Float seconds = (Float) data[2];
                            float pad10 = 16f;
                            float pad5 = 8f;
                            float pad3 = 4.8f;
                            if (modeChangeInfoPopup != null) {
                                modeChangeInfoPopup.remove();
                            }
                            modeChangeInfoPopup = new Table(skin);
                            modeChangeInfoPopup.setName("mct-" + name);
                            modeChangeInfoPopup.setBackground("table-bg");
                            modeChangeInfoPopup.pad(pad10);

                            // Fill up table
                            OwnLabel ttl = new OwnLabel(mpi.title, skin, "hud-header");
                            modeChangeInfoPopup.add(ttl).left().padBottom(pad10).row();

                            OwnLabel dsc = new OwnLabel(mpi.header, skin);
                            modeChangeInfoPopup.add(dsc).left().padBottom(pad5 * 3f).row();

                            Table keysTable = new Table(skin);
                            for (Pair<String[], String> m : mpi.mappings) {
                                HorizontalGroup keysGroup = new HorizontalGroup();
                                keysGroup.space(pad3);
                                String[] keys = m.getFirst();
                                String action = m.getSecond();
                                if (keys != null && keys.length > 0 && action != null && !action.isEmpty()) {
                                    for (int i = 0; i < keys.length; i++) {
                                        TextButton key = new TextButton(keys[i], skin, "key");
                                        key.pad(0, pad3, 0, pad3);
                                        key.pad(pad5);
                                        keysGroup.addActor(key);
                                        if (i < keys.length - 1) {
                                            keysGroup.addActor(new OwnLabel("+", skin));
                                        }
                                    }
                                    keysTable.add(keysGroup).right().padBottom(pad5).padRight(pad10 * 2f);
                                    keysTable.add(new OwnLabel(action, skin)).left().padBottom(pad5).row();
                                }
                            }
                            modeChangeInfoPopup.add(keysTable).center().row();
                            if (mpi.warn != null && !mpi.warn.isEmpty()) {
                                modeChangeInfoPopup.add(new OwnLabel(mpi.warn, skin, "mono-pink")).left().padTop(pad10).padBottom(pad5).row();
                            }
                            OwnTextButton closeButton = new OwnTextButton(I18n.msg("gui.ok") + " [esc]", skin);
                            closeButton.pad(pad3, pad10, pad3, pad10);
                            closeButton.addListener(event1 -> {
                                if (event1 instanceof ChangeEvent) {
                                    removeModeChangePopup();
                                    return true;
                                }
                                return false;
                            });
                            modeChangeInfoPopup.add(closeButton).right().padTop(pad10 * 2f);

                            modeChangeInfoPopup.pack();

                            // Add table to UI.
                            Container<Table> mct = new Container<>(modeChangeInfoPopup);
                            mct.setFillParent(true);
                            mct.top();
                            mct.pad(pad10 * 2, 0, 0, 0);
                            stage.addActor(mct);

                            // Cancel and schedule task.
                            cancelRemovePopupTask();
                            removePopup = new Task() {
                                @Override
                                public void run() {
                                    if (modeChangeInfoPopup != null && modeChangeInfoPopup.hasParent()) {
                                        modeChangeInfoPopup.remove();
                                    }
                                }
                            };
                            Timer.schedule(removePopup, seconds);

                        } else {
                            // Remove
                            if (modeChangeInfoPopup != null && modeChangeInfoPopup.hasParent() && modeChangeInfoPopup.getName()
                                    .equals("mct-" + name)) {
                                modeChangeInfoPopup.remove();
                                modeChangeInfoPopup = null;
                            }
                        }
                    }
                }
                case DISPLAY_GUI_CMD -> {
                    boolean displayGui = (Boolean) data[0];
                    inputMultiplexer.removeProcessor(current.getGuiStage());
                    if (displayGui) {
                        // Add processor if needed.
                        inputMultiplexer.addProcessor(0, current.getGuiStage());
                    }
                }
                case SHOW_RESTART_ACTION -> {
                    String text;
                    if (data.length > 0) {
                        text = (String) data[0];
                    } else {
                        text = I18n.msg("gui.restart.default");
                    }
                    GenericDialog restart = new GenericDialog(I18n.msg("gui.restart.title"), skin, stage) {

                        @Override
                        protected void build() {
                            content.clear();
                            content.add(new OwnLabel(text, skin)).left().padBottom(pad18 * 2f).row();
                        }

                        @Override
                        protected boolean accept() {
                            // Shut down.
                            GaiaSky.postRunnable(() -> {
                                Gdx.app.exit();
                                // Attempt restart.
                                Path workingDir = Path.of(System.getProperty("user.dir"));
                                Path[] scripts;
                                if (SysUtils.isWindows()) {
                                    scripts = new Path[]{workingDir.resolve("gaiasky.exe"), workingDir.resolve("gaiasky.bat"), workingDir.resolve(
                                            "gradlew.bat")};
                                } else {
                                    scripts = new Path[]{workingDir.resolve("gaiasky"), workingDir.resolve("gradlew")};
                                }
                                for (Path file : scripts) {
                                    if (Files.exists(file) && Files.isRegularFile(file) && Files.isExecutable(file)) {
                                        try {
                                            if (file.getFileName().toString().contains("gaiasky")) {
                                                // Just use the script.
                                                final ArrayList<String> command = new ArrayList<>();
                                                command.add(file.toString());
                                                final ProcessBuilder builder = new ProcessBuilder(command);
                                                builder.start();
                                            } else if (file.getFileName().toString().contains("gradlew")) {
                                                // Gradle script.
                                                final ArrayList<String> command = new ArrayList<>();
                                                command.add(file.toString());
                                                command.add("core:run");
                                                final ProcessBuilder builder = new ProcessBuilder(command);
                                                builder.start();
                                            }
                                            break;
                                        } catch (IOException e) {
                                            logger.error(e, "Error running Gaia Sky");
                                        }
                                    }
                                }
                            });
                            return true;
                        }

                        @Override
                        protected void cancel() {
                            // Nothing.
                        }

                        @Override
                        public void dispose() {
                            // Nothing.
                        }
                    };
                    restart.setAcceptText(I18n.msg("gui.yes"));
                    restart.setCancelText(I18n.msg("gui.no"));
                    restart.buildSuper();
                    restart.show(stage);
                }
                case CLOSE_ALL_GUI_WINDOWS_CMD -> {
                    var actors = stage.getActors();
                    for (var actor : actors) {
                        if (actor instanceof GenericDialog) {
                            closeWindow((GenericDialog) actor);
                        }
                    }
                }
                case UI_RELOAD_CMD -> reloadUI((GlobalResources) data[0]);
                default -> {
                }
            }
        }

    }

    private TextureWindow getTextureWindow(Object[] data,
                                           Stage stage) {
        var title = (String) data[0];
        var scale = 1f;
        if (data.length > 2) {
            scale = (Float) data[2];
        }
        var flipX = false;
        var flipY = false;
        if (data.length > 3) {
            flipX = (Boolean) data[3];
        }
        if (data.length > 4) {
            flipY = (Boolean) data[4];
        }
        TextureWindow textureWindow;
        if (data[1] instanceof FrameBuffer frameBuffer) {
            textureWindow = new TextureWindow(title, skin, stage, frameBuffer, scale);
        } else {
            var texture = (Texture) data[1];
            textureWindow = new TextureWindow(title, skin, stage, texture, scale);
        }
        textureWindow.setFlip(flipX, flipY);
        return textureWindow;
    }

    private void closeWindow(GenericDialog dialog) {
        if (dialog != null && dialog.isVisible() && dialog.hasParent()) {
            dialog.closeCancel();
        }
    }

    private Array<Actor> getPreferencesWindows() {
        Array<Actor> result = new Array<>();
        if (current != null) {
            Stage ui = current.getGuiStage();
            Array<Actor> actors = ui.getActors();
            for (Actor actor : actors) {
                if (PreferencesWindow.class.isAssignableFrom(actor.getClass())) {
                    result.add(actor);
                }
            }
        }
        return result;
    }

    public boolean removeGamepadGui() {
        for (int i = 0; i < guis.size; i++) {
            IGui gui = guis.get(i);
            if (gui instanceof GamepadGui gamepadGui) {
                return gamepadGui.removeGamepadGui();
            }
        }
        return false;
    }

    /**
     * Cancels the task that removes the information pop-up if it is
     * scheduled.
     */
    private void cancelRemovePopupTask() {
        if (removePopup != null && removePopup.isScheduled()) {
            removePopup.cancel();
        }
    }

    public boolean removeModeChangePopup() {
        boolean removed = false;
        if (modeChangeInfoPopup != null) {
            removed = modeChangeInfoPopup.remove();
            cancelRemovePopupTask();
        }

        return removed;
    }

    /**
     * This method updates the default skin and reloads the full UI.
     *
     * @param globalResources The global resources object to update.
     */
    private void reloadUI(GlobalResources globalResources) {
        // Reinitialise user interface.
        GaiaSky.postRunnable(() -> {
            // Reinitialise GUI system.
            globalResources.updateSkin();
            GenericDialog.updatePads();
            GaiaSky.instance.reinitialiseGUI1();
            EventManager.publish(Event.SPACECRAFT_LOADED, this, scene.getEntity("Spacecraft"));
            GaiaSky.instance.reinitialiseGUI2();
            // Time init.
            EventManager.publish(Event.TIME_CHANGE_INFO, this, GaiaSky.instance.time.getTime());
            if (GaiaSky.instance.cameraManager.mode == CameraManager.CameraMode.FOCUS_MODE) {
                // Refocus.
                FocusView focus = (FocusView) GaiaSky.instance.cameraManager.getFocus();
                EventManager.publish(Event.FOCUS_CHANGE_CMD, this, focus.getEntity());
            }
            // UI theme reload broadcast.
            EventManager.publish(Event.UI_THEME_RELOAD_INFO, this, globalResources.getSkin());
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("notif.ui.reload"));
        });
    }

}
