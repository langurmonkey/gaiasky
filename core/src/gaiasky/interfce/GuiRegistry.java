/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.data.group.DatasetOptions;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.scenegraph.ISceneGraph;
import gaiasky.scenegraph.ParticleGroup;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.script.EventScriptingInterface;
import gaiasky.util.*;
import gaiasky.util.scene2d.FileChooser;
import gaiasky.util.scene2d.OwnLabel;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the Graphical User Interfaces of Gaia Sky
 *
 * @author tsagrista
 */
public class GuiRegistry implements IObserver {
    private static final Logger.Log logger = Logger.getLogger(GuiRegistry.class);
    /**
     * Registered GUI array
     **/
    private static Array<IGui> guis;

    static {
        guis = new Array<>(true, 2);
    }

    /**
     * Render lock object
     */
    public static Object guirenderlock = new Object();

    /**
     * Current GUI object
     **/
    public static IGui current;
    /**
     * Previous GUI object, if any
     **/
    public static IGui previous;

    /**
     * Global input multiplexer
     **/
    private static InputMultiplexer im = null;

    public static void setInputMultiplexer(InputMultiplexer im) {
        GuiRegistry.im = im;
    }

    public static InputMultiplexer getInputMultiplexer() {
        return GuiRegistry.im;
    }

    /**
     * Switches the current GUI with the given one, updating the processors.
     * It also sets the previous GUI to the given value.
     *
     * @param gui      The new GUI
     * @param previous The new previous GUI
     */
    public static void change(IGui gui, IGui previous) {
        if (current != gui) {
            unset(previous);
            set(gui);
        }
    }

    /**
     * Switches the current GUI with the given one, updating the processors
     *
     * @param gui The new gui
     */
    public static void change(IGui gui) {
        if (current != gui) {
            unset();
            set(gui);
        }
    }

    /**
     * Unsets the current GUI and sets it as previous
     */
    public static void unset() {
        unset(current);
    }

    /**
     * Unsets the given GUI and sets it as previous
     *
     * @param gui The GUI
     */
    public static void unset(IGui gui) {
        if (gui != null) {
            unregisterGui(gui);
            im.removeProcessor(gui.getGuiStage());
        }
        previous = gui;
    }

    /**
     * Sets the given GUI as current
     *
     * @param gui The new GUI
     */
    public static void set(IGui gui) {
        if (gui != null) {
            registerGui(gui);
            im.addProcessor(0, gui.getGuiStage());
        }
        current = gui;
    }

    /**
     * Sets the given GUI as previous
     *
     * @param gui The new previous GUI
     */
    public static void setPrevious(IGui gui) {
        previous = gui;
    }

    /**
     * Registers a new GUI
     *
     * @param gui The GUI to register
     */
    public static void registerGui(IGui gui) {
        if (!guis.contains(gui, true)) {
            guis.add(gui);
        }
    }

    /**
     * Unregisters a GUI
     *
     * @param gui The GUI to unregister
     * @return True if the GUI was unregistered
     */
    public static boolean unregisterGui(IGui gui) {
        return guis.removeValue(gui, true);
    }

    /**
     * Unregisters all GUIs
     *
     * @return True if operation succeeded
     */
    public static boolean unregisterAll() {
        guis.clear();
        return true;
    }

    /**
     * Renders the registered GUIs
     *
     * @param rw The render width
     * @param rh The render height
     */
    public static void render(int rw, int rh) {
        if (GlobalConf.runtime.DISPLAY_GUI) {
            synchronized (guirenderlock) {
                for (int i = 0; i < guis.size; i++) {
                    guis.get(i).getGuiStage().getViewport().apply();
                    guis.get(i).render(rw, rh);
                }
            }
        }
    }

    /**
     * Adds the stage of the given GUI to the processors in
     * the input multiplexer
     *
     * @param gui The gui
     */
    public static void addProcessor(IGui gui) {
        if (im != null && gui != null)
            im.addProcessor(gui.getGuiStage());
    }

    public static void removeProcessor(IGui gui) {
        if (im != null && gui != null)
            im.removeProcessor(gui.getGuiStage());
    }

    /**
     * Updates the registered GUIs
     *
     * @param dt The delta time in seconds
     */
    public static void update(double dt) {
        for (IGui gui : guis)
            gui.update(dt);
    }

    private Skin skin;

    /**
     * Keyframes window
     **/
    private KeyframesWindow keyframesWindow;

    /**
     * Individual visibility
     */
    private IndividualVisibilityWindow indVisWindow;

    /**
     * Mode change info popup
     */
    public Table modeChangeTable;

    private RemoveActorThread removeActorThread;

    /**
     * Last open location
     */
    private Path lastOpenLocation;

    /* Slave config window */
    private SlaveConfigWindow slaveConfigWindow;

    /**
     * One object to handle observer pattern
     */
    public GuiRegistry(Skin skin) {
        super();
        this.skin = skin;
        // Windows which are visible from any GUI
        EventManager.instance.subscribe(this, Events.SHOW_QUIT_ACTION, Events.SHOW_ABOUT_ACTION, Events.SHOW_LOAD_CATALOG_ACTION, Events.SHOW_PREFERENCES_ACTION, Events.SHOW_KEYFRAMES_WINDOW_ACTION, Events.SHOW_SLAVE_CONFIG_ACTION, Events.UI_THEME_RELOAD_INFO, Events.MODE_POPUP_CMD, Events.DISPLAY_GUI_CMD, Events.CAMERA_MODE_CMD, Events.UI_RELOAD_CMD, Events.SHOW_INDIVIDUAL_VISIBILITY_ACTION);
    }

    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    protected SearchDialog searchDialog;

    @Override
    public void notify(Events event, Object... data) {
        if (current != null) {
            Stage ui = current.getGuiStage();
            // Treats windows that can appear in any GUI
            switch (event) {
                case SHOW_QUIT_ACTION:
                    if (!removeModeChangePopup()) {
                        if (GLFW.glfwGetInputMode(((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle(), GLFW.GLFW_CURSOR) == GLFW.GLFW_CURSOR_DISABLED) {
                            // Release mouse if captured
                            GLFW.glfwSetInputMode(((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
                        } else {
                            Runnable quitRunnable = data.length > 0 ? (Runnable) data[0] : null;
                            if (GlobalConf.program.EXIT_CONFIRMATION) {
                                QuitWindow quit = new QuitWindow(ui, skin);
                                if (data.length > 0) {
                                    quit.setAcceptRunnable(quitRunnable);
                                }
                                quit.show(ui);
                            } else {
                                if (quitRunnable != null)
                                    quitRunnable.run();
                                GaiaSky.postRunnable(() -> Gdx.app.exit());
                            }
                        }
                    }
                    break;
                case CAMERA_MODE_CMD:
                    removeModeChangePopup();
                    break;
                case SHOW_ABOUT_ACTION:
                    (new AboutWindow(ui, skin)).show(ui);
                    break;
                case SHOW_PREFERENCES_ACTION:
                    (new PreferencesWindow(ui, skin)).show(ui);
                    break;
                case SHOW_INDIVIDUAL_VISIBILITY_ACTION:
                    if (indVisWindow == null) {
                        final ISceneGraph sg = GaiaSky.instance.sg;
                        indVisWindow = new IndividualVisibilityWindow(sg, ui, skin);
                    }
                    if (!indVisWindow.isVisible() || !indVisWindow.hasParent())
                        indVisWindow.show(ui);
                    break;
                case SHOW_SLAVE_CONFIG_ACTION:
                    if (MasterManager.hasSlaves()) {
                        if (slaveConfigWindow == null)
                            slaveConfigWindow = new SlaveConfigWindow(ui, skin);
                        slaveConfigWindow.show(ui);
                    }
                    break;
                case SHOW_LOAD_CATALOG_ACTION:
                    if (lastOpenLocation == null && GlobalConf.program.LAST_OPEN_LOCATION != null && !GlobalConf.program.LAST_OPEN_LOCATION.isEmpty()) {
                        try {
                            lastOpenLocation = Paths.get(GlobalConf.program.LAST_OPEN_LOCATION);
                        } catch (Exception e) {
                            lastOpenLocation = null;
                        }
                    }
                    if (lastOpenLocation == null) {
                        lastOpenLocation = SysUtils.getUserHome();
                    } else if (!Files.exists(lastOpenLocation) || !Files.isDirectory(lastOpenLocation)) {
                        lastOpenLocation = SysUtils.getHomeDir();
                    }

                    FileChooser fc = new FileChooser(I18n.txt("gui.loadcatalog"), skin, ui, lastOpenLocation, FileChooser.FileChooserTarget.FILES);
                    fc.setAcceptText(I18n.txt("gui.loadcatalog"));
                    fc.setFileFilter(pathname -> pathname.getFileName().toString().endsWith(".vot") || pathname.getFileName().toString().endsWith(".csv"));
                    fc.setAcceptedFiles("*.vot, *.csv");
                    fc.setResultListener((success, result) -> {
                        if (success) {
                            if (Files.exists(result) && Files.exists(result)) {
                                // Load selected file
                                try {
                                    String fileName = result.getFileName().toString();
                                    final DatasetLoadDialog dld = new DatasetLoadDialog(I18n.txt("gui.dsload.title") + ": " + fileName, fileName, skin, ui);
                                    Runnable doLoad = () -> {
                                        try {
                                            Thread t = new Thread(() -> {
                                                DatasetOptions dops = dld.generateDatasetOptions();
                                                // Load dataset
                                                EventScriptingInterface.instance().loadDataset(dops.catalogName, result.toAbsolutePath().toString(), CatalogInfo.CatalogInfoType.UI, dops, true);
                                                // Select first
                                                CatalogInfo ci = CatalogManager.instance().get(dops.catalogName);
                                                if (ci.object != null) {
                                                    if (ci.object instanceof ParticleGroup) {
                                                        ParticleGroup pg = (ParticleGroup) ci.object;
                                                        if (pg.data() != null && !pg.data().isEmpty() && pg.isVisibilityOn()) {
                                                            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.FOCUS_MODE);
                                                            EventManager.instance.post(Events.FOCUS_CHANGE_CMD, pg.getRandomParticleName());
                                                        }
                                                    } else if (ci.object.children != null && !ci.object.children.isEmpty() && ci.object.children.get(0).isVisibilityOn()) {
                                                        EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraManager.CameraMode.FOCUS_MODE);
                                                        EventManager.instance.post(Events.FOCUS_CHANGE_CMD, ci.object.children.get(0));
                                                    }
                                                    // Open UI datasets
                                                    EventScriptingInterface.instance().maximizeInterfaceWindow();
                                                    EventScriptingInterface.instance().expandGuiComponent("DatasetsComponent");
                                                }
                                            });
                                            t.setName("Dataset loader thread (scripting)");
                                            t.start();
                                        } catch (Exception e) {
                                            logger.error(I18n.txt("notif.error", fileName), e);
                                        }
                                    };
                                    dld.setAcceptRunnable(doLoad);
                                    dld.show(ui);

                                    lastOpenLocation = result.getParent();
                                    GlobalConf.program.LAST_OPEN_LOCATION = lastOpenLocation.toAbsolutePath().toString();
                                    return true;
                                } catch (Exception e) {
                                    logger.error(I18n.txt("notif.error", result.getFileName()), e);
                                    return false;
                                }

                            } else {
                                logger.error("Selection must be a file: " + result.toAbsolutePath());
                                return false;
                            }
                        } else {
                            // Still, update last location
                            if (!Files.isDirectory(result)) {
                                lastOpenLocation = result.getParent();
                            } else {
                                lastOpenLocation = result;
                            }
                            GlobalConf.program.LAST_OPEN_LOCATION = lastOpenLocation.toAbsolutePath().toString();
                        }
                        return false;
                    });
                    fc.show(ui);
                    break;
                case SHOW_KEYFRAMES_WINDOW_ACTION:
                    if (keyframesWindow == null) {
                        keyframesWindow = new KeyframesWindow(ui, skin);
                    }
                    if (!keyframesWindow.isVisible() || !keyframesWindow.hasParent())
                        keyframesWindow.show(ui, 0, 0);
                    break;
                case UI_THEME_RELOAD_INFO:
                    if (keyframesWindow != null) {
                        keyframesWindow.dispose();
                        keyframesWindow = null;
                    }
                    this.skin = (Skin) data[0];
                    break;
                case MODE_POPUP_CMD:
                    if (GlobalConf.runtime.DISPLAY_GUI) {
                        ModePopupInfo mpi = (ModePopupInfo) data[0];
                        String name = (String) data[1];

                        if (mpi != null) {
                            // Add
                            Float seconds = (Float) data[2];
                            float pad10 = 10f * GlobalConf.UI_SCALE_FACTOR;
                            float pad5 = 5f * GlobalConf.UI_SCALE_FACTOR;
                            float pad3 = 3f * GlobalConf.UI_SCALE_FACTOR;
                            if (modeChangeTable != null) {
                                modeChangeTable.remove();
                            }
                            modeChangeTable = new Table(skin);
                            modeChangeTable.setName("mct-" + name);
                            modeChangeTable.setBackground("table-bg");
                            modeChangeTable.pad(pad10);

                            // Fill up table
                            OwnLabel ttl = new OwnLabel(mpi.title, skin, "hud-header");
                            modeChangeTable.add(ttl).left().padBottom(pad10).row();

                            OwnLabel dsc = new OwnLabel(mpi.header, skin);
                            modeChangeTable.add(dsc).left().padBottom(pad5 * 3f).row();

                            Table keysTable = new Table(skin);
                            for (Pair<String[], String> m : mpi.mappings) {
                                HorizontalGroup keysGroup = new HorizontalGroup();
                                keysGroup.space(pad3);
                                String[] keys = m.getFirst();
                                String action = m.getSecond();
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
                            modeChangeTable.add(keysTable).center().row();
                            modeChangeTable.add(new OwnLabel("ESC - close this", skin, "mono")).right().padTop(pad10 * 2f);

                            modeChangeTable.pack();

                            // Add table to UI
                            Container mct = new Container<>(modeChangeTable);
                            mct.setFillParent(true);
                            mct.top();
                            mct.pad(pad10 * 2, 0, 0, 0);
                            ui.addActor(mct);

                            startModePopupInfoThread(modeChangeTable, seconds);
                        } else {
                            // Remove
                            if (modeChangeTable != null && modeChangeTable.hasParent() &&  modeChangeTable.getName().equals("mct-" + name)){
                                modeChangeTable.remove();
                                modeChangeTable = null;
                            }
                        }
                    }
                    break;
                case DISPLAY_GUI_CMD:
                    boolean displayGui = (Boolean) data[0];
                    if (!displayGui) {
                        // Remove processor
                        im.removeProcessor(current.getGuiStage());
                    } else {
                        // Add processor
                        im.addProcessor(0, current.getGuiStage());
                    }
                    break;
                case UI_RELOAD_CMD:
                    reloadUI();
                    break;
                default:
                    break;
            }
        }

    }

    public boolean removeModeChangePopup() {
        boolean removed = false;
        if (modeChangeTable != null) {
            removed = modeChangeTable.remove();
            // Kill thread
            if (removeActorThread != null && removeActorThread.isAlive()) {
                removeActorThread.interrupt();
                removeActorThread = null;
            }
        }

        return removed;
    }

    private void startModePopupInfoThread(Actor actor, float seconds) {
        removeActorThread = new RemoveActorThread(actor, seconds);
        removeActorThread.start();
    }

    private void reloadUI() {
        // Reinitialise user interface
        GaiaSky.postRunnable(() -> {
            // Reinitialise GUI system
            GlobalResources.updateSkin();
            GenericDialog.updatePads();
            GaiaSky.instance.reinitialiseGUI1();
            EventManager.instance.post(Events.SPACECRAFT_LOADED, GaiaSky.instance.sg.getNode("Spacecraft"));
            GaiaSky.instance.reinitialiseGUI2();
            // Time init
            EventManager.instance.post(Events.TIME_CHANGE_INFO, GaiaSky.instance.time.getTime());
            if (GaiaSky.instance.cam.mode == CameraManager.CameraMode.FOCUS_MODE)
                // Refocus
                EventManager.instance.post(Events.FOCUS_CHANGE_CMD, GaiaSky.instance.cam.getFocus());
            // Update names with new language
            GaiaSky.instance.sg.getRoot().updateNamesRec();
            // UI theme reload broadcast
            EventManager.instance.post(Events.UI_THEME_RELOAD_INFO, GlobalResources.skin);
        });
    }

}
