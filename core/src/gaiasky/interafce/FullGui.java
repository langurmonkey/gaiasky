/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.desktop.util.MemInfoWindow;
import gaiasky.desktop.util.SysUtils;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.*;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings.ProgramSettings.UpdateSettings;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.scene2d.FileChooser;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.update.VersionCheckEvent;
import gaiasky.util.update.VersionChecker;

import java.nio.file.Files;
import java.time.Instant;

/**
 * Full OpenGL GUI with all the controls and whistles.
 */
public class FullGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(FullGui.class);

    protected ControlsWindow controlsWindow;
    protected MinimapWindow minimapWindow;

    protected Container<FocusInfoInterface> fi;
    protected Container<TopInfoInterface> ti;
    protected Container<NotificationsInterface> ni;
    protected FocusInfoInterface focusInterface;
    protected NotificationsInterface notificationsInterface;
    protected MessagesInterface messagesInterface;
    protected CustomInterface customInterface;
    protected RunStateInterface runStateInterface;
    protected TopInfoInterface topInfoInterface;
    protected PopupNotificationsInterface popupNotificationsInterface;
    protected MinimapInterface minimapInterface;
    protected LoadProgressInterface loadProgressInterface;

    protected MemInfoWindow memInfoWindow;
    protected LogWindow logWindow;
    protected WikiInfoWindow wikiInfoWindow;
    protected ArchiveViewWindow archiveViewWindow;

    protected INumberFormat nf;
    protected Label pointerXCoord, pointerYCoord;

    protected float pad, pad5;

    protected ISceneGraph sg;
    private ComponentType[] visibilityEntities;
    private boolean[] visible;

    private final GlobalResources globalResources;
    private final CatalogManager catalogManager;

    public FullGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel, final GlobalResources globalResources, final CatalogManager catalogManager) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        this.globalResources = globalResources;
        this.catalogManager = catalogManager;
    }

    @Override
    public void initialize(final AssetManager assetManager, final SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        this.ui = new Stage(vp, sb);
        vp.update(graphics.getWidth(), graphics.getHeight(), true);
    }

    public void initialize(Stage ui) {
        this.ui = ui;
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        logger.info(I18n.txt("notif.gui.init"));

        interfaces = new Array<>();

        buildGui();

        // We must subscribe to the desired events
        EventManager.instance.subscribe(this, Events.FOV_CHANGED_CMD, Events.SHOW_WIKI_INFO_ACTION, Events.UPDATE_WIKI_INFO_ACTION, Events.SHOW_ARCHIVE_VIEW_ACTION, Events.UPDATE_ARCHIVE_VIEW_ACTION, Events.SHOW_PLAYCAMERA_ACTION, Events.DISPLAY_MEM_INFO_WINDOW, Events.REMOVE_KEYBOARD_FOCUS, Events.REMOVE_GUI_COMPONENT, Events.ADD_GUI_COMPONENT, Events.SHOW_LOG_ACTION, Events.RA_DEC_UPDATED, Events.LON_LAT_UPDATED, Events.POPUP_MENU_FOCUS, Events.SHOW_LAND_AT_LOCATION_ACTION, Events.DISPLAY_POINTER_COORDS_CMD, Events.TOGGLE_MINIMAP, Events.SHOW_MINIMAP_ACTION, Events.SHOW_PROCEDURAL_GEN_ACTION);
    }

    protected void buildGui() {
        pad = 16f;
        pad5 = 8f;
        // Component types name init
        for (ComponentType ct : ComponentType.values()) {
            ct.getName();
        }
        nf = NumberFormatFactory.getFormatter("##0.##");

        // NOTIFICATIONS INTERFACE - BOTTOM LEFT
        notificationsInterface = new NotificationsInterface(skin, lock, true, true, true, true);
        notificationsInterface.pad(pad5);
        ni = new Container<>(notificationsInterface);
        ni.setFillParent(true);
        ni.bottom().left();
        ni.pad(0, pad, pad, 0);
        interfaces.add(notificationsInterface);

        // CONTROLS WINDOW
        addControlsWindow();

        // FOCUS INFORMATION - BOTTOM RIGHT
        focusInterface = new FocusInfoInterface(skin);
        fi = new Container<>(focusInterface);
        fi.setFillParent(true);
        fi.bottom().right();
        fi.pad(0, 0, pad, pad);
        interfaces.add(focusInterface);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, Gdx.graphics.getWidth() * 0.2f, Gdx.graphics.getHeight() * 0.2f, 0);
        interfaces.add(messagesInterface);

        // TOP INFO - TOP CENTER
        topInfoInterface = new TopInfoInterface(skin);
        topInfoInterface.top();
        topInfoInterface.pad(pad5, pad, pad5, pad);
        ti = new Container<>(topInfoInterface);
        ti.setFillParent(true);
        ti.top();
        ti.pad(pad);
        interfaces.add(topInfoInterface);

        // MINIMAP
        initializeMinimap(ui);

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
        addLoadProgressInterface(ui);

        // CUSTOM OBJECTS INTERFACE
        customInterface = new CustomInterface(ui, skin, lock);
        interfaces.add(customInterface);

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
        if (Settings.settings.program.update.lastCheck == null || Instant.now().toEpochMilli() - Settings.settings.program.update.lastCheck.toEpochMilli() > UpdateSettings.VERSION_CHECK_INTERVAL_MS) {
            // Start version check
            VersionChecker vc = new VersionChecker(Settings.settings.program.url.versionCheck);
            vc.setListener(event -> {
                if (event instanceof VersionCheckEvent) {
                    VersionCheckEvent vce = (VersionCheckEvent) event;
                    if (!vce.isFailed()) {
                        // Check version
                        String tagVersion = vce.getTag();
                        Integer versionNumber = vce.getVersionNumber();

                        Settings.settings.program.update.lastCheck = Instant.now();

                        if (versionNumber > Settings.settings.version.versionNumber) {
                            logger.info(I18n.txt("gui.newversion.available", Settings.settings.version.version, tagVersion));
                            // There's a new version!
                            UpdatePopup newVersion = new UpdatePopup(tagVersion, ui, skin);
                            newVersion.pack();
                            float ww = newVersion.getWidth();
                            float margin = 8f;
                            newVersion.setPosition(graphics.getWidth() - ww - margin, margin);
                            ui.addActor(newVersion);
                        } else {
                            // No new version
                            logger.info(I18n.txt("gui.newversion.nonew", Settings.settings.program.update.getLastCheckedString()));
                        }

                    } else {
                        // Handle failed case
                        // Do nothing
                        logger.info(I18n.txt("gui.newversion.fail"));
                    }
                }
                return false;
            });

            // Start in 10 seconds
            Thread vct = new Thread(vc);
            Timer.Task t = new Timer.Task() {
                @Override
                public void run() {
                    logger.info(I18n.txt("gui.newversion.checking"));
                    vct.start();
                }
            };
            Timer.schedule(t, 10);
        }

    }

    public void recalculateOptionsSize() {
        controlsWindow.recalculateSize();
    }

    protected void rebuildGui() {
        if (ui != null) {
            ui.clear();
            boolean collapsed;
            if (controlsWindow != null) {
                collapsed = controlsWindow.isCollapsed();
                recalculateOptionsSize();
                if (collapsed)
                    controlsWindow.collapseInstant();
                controlsWindow.setPosition(0, graphics.getHeight() * unitsPerPixel - controlsWindow.getHeight());
                ui.addActor(controlsWindow);
            }
            if (ni != null) {
                ui.addActor(ni);
            }
            if (messagesInterface != null) {
                ui.addActor(messagesInterface);
            }
            if (fi != null) {
                ui.addActor(fi);
            }
            if (runStateInterface != null) {
                ui.addActor(runStateInterface);
            }
            if (ti != null) {
                ui.addActor(ti);
            }
            if (minimapInterface != null) {
                ui.addActor(minimapInterface);
            }
            if (loadProgressInterface != null) {
                ui.addActor(loadProgressInterface);
            }
            if (pointerXCoord != null && pointerYCoord != null) {
                ui.addActor(pointerXCoord);
                ui.addActor(pointerYCoord);
            }
            if (customInterface != null) {
                customInterface.reAddObjects();
            }
            if (popupNotificationsInterface != null) {
                ui.addActor(popupNotificationsInterface);
            }

            /* CAPTURE SCROLL FOCUS */
            ui.addListener(new EventListener() {

                @Override
                public boolean handle(Event event) {
                    if (event instanceof InputEvent) {
                        InputEvent ie = (InputEvent) event;

                        if (ie.getType() == Type.mouseMoved) {
                            Actor scrollPanelAncestor = getScrollPanelAncestor(ie.getTarget());
                            ui.setScrollFocus(scrollPanelAncestor);
                        } else if (ie.getType() == Type.touchDown) {
                            if (ie.getTarget() instanceof TextField)
                                ui.setKeyboardFocus(ie.getTarget());
                        }
                    }
                    return false;
                }

                private Actor getScrollPanelAncestor(Actor actor) {
                    if (actor == null) {
                        return null;
                    } else if (actor instanceof ScrollPane) {
                        return actor;
                    } else {
                        return getScrollPanelAncestor(actor.getParent());
                    }
                }

            });

            /* KEYBOARD FOCUS */
            ui.addListener((event) -> {
                if (event instanceof InputEvent) {
                    InputEvent ie = (InputEvent) event;
                    if (ie.getType() == Type.touchDown && !ie.isHandled()) {
                        ui.setKeyboardFocus(null);
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
        if (ui.getScrollFocus() != null) {
            ui.setScrollFocus(null);
            ui.setKeyboardFocus(null);
            return true;
        }
        return false;
    }

    @Override
    public void update(double dt) {
        ui.act((float) dt);
        for (IGuiInterface i : interfaces) {
            if (i.isOn())
                i.update();
        }
    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case SHOW_PROCEDURAL_GEN_ACTION:
            Planet planet = (Planet) data[0];
            Actor w = findActor("procedural-window");
            // Only one instance
            if (w != null && w.hasParent()) {
                if (!w.isVisible())
                    w.setVisible(true);
            } else {
                ProceduralGenerationWindow proceduralWindow = new ProceduralGenerationWindow(planet, ui, skin);
                proceduralWindow.setName("procedural-window");
                proceduralWindow.show(ui);
            }
            break;
        case SHOW_LAND_AT_LOCATION_ACTION:
            CelestialBody target = (CelestialBody) data[0];
            LandAtWindow landAtLocation = new LandAtWindow(target, ui, skin);
            landAtLocation.show(ui);
            break;
        case SHOW_PLAYCAMERA_ACTION:
            FileChooser fc = new FileChooser(I18n.txt("gui.camera.title"), skin, ui, SysUtils.getDefaultCameraDir(), FileChooser.FileChooserTarget.FILES);
            fc.setShowHidden(Settings.settings.program.fileChooser.showHidden);
            fc.setShowHiddenConsumer((showHidden) -> Settings.settings.program.fileChooser.showHidden = showHidden);
            fc.setAcceptText(I18n.txt("gui.camera.run"));
            fc.setFileFilter(pathname -> pathname.getFileName().toString().endsWith(".dat") || pathname.getFileName().toString().endsWith(".gsc"));
            fc.setAcceptedFiles("*.dat, *.gsc");
            fc.setResultListener((success, result) -> {
                if (success) {
                    if (Files.exists(result) && Files.exists(result)) {
                        EventManager.instance.post(Events.PLAY_CAMERA_CMD, result);
                        return true;
                    } else {
                        logger.error("Selection must be a file: " + result.toAbsolutePath());
                    }
                }
                return false;
            });
            fc.show(ui);
            break;
        case DISPLAY_MEM_INFO_WINDOW:
            if (memInfoWindow == null) {
                memInfoWindow = new MemInfoWindow(ui, skin);
            }
            if (!memInfoWindow.isVisible() || !memInfoWindow.hasParent())
                memInfoWindow.show(ui);
            break;
        case SHOW_LOG_ACTION:
            if (logWindow == null) {
                logWindow = new LogWindow(ui, skin);
            }
            logWindow.update();
            if (!logWindow.isVisible() || !logWindow.hasParent())
                logWindow.show(ui);
            break;
        case UPDATE_WIKI_INFO_ACTION:
            if (wikiInfoWindow != null && wikiInfoWindow.isVisible() && wikiInfoWindow.hasParent() && !wikiInfoWindow.isUpdating()) {
                // Update
                String searchName = (String) data[0];
                wikiInfoWindow.update(searchName);
            }
            break;
        case SHOW_WIKI_INFO_ACTION:
            String searchName = (String) data[0];
            if (wikiInfoWindow == null) {
                wikiInfoWindow = new WikiInfoWindow(ui, skin);
            }
            if (!wikiInfoWindow.isUpdating()) {
                wikiInfoWindow.update(searchName);
                if (!wikiInfoWindow.isVisible() || !wikiInfoWindow.hasParent())
                    wikiInfoWindow.show(ui);
            }
            break;
        case UPDATE_ARCHIVE_VIEW_ACTION:
            if (archiveViewWindow != null && archiveViewWindow.isVisible() && archiveViewWindow.hasParent()) {
                // Update
                IStarFocus starFocus = (IStarFocus) data[0];
                archiveViewWindow.update(starFocus);
            }
            break;
        case SHOW_ARCHIVE_VIEW_ACTION:
            IStarFocus starFocus = (IStarFocus) data[0];
            if (archiveViewWindow == null) {
                archiveViewWindow = new ArchiveViewWindow(ui, skin);
            }
            archiveViewWindow.update(starFocus);
            if (!archiveViewWindow.isVisible() || !archiveViewWindow.hasParent())
                archiveViewWindow.show(ui);
            break;
        case REMOVE_KEYBOARD_FOCUS:
            ui.setKeyboardFocus(null);
            break;
        case REMOVE_GUI_COMPONENT:
            String name = (String) data[0];
            String method = "remove" + TextUtils.capitalise(name);
            try {
                Method m = ClassReflection.getMethod(this.getClass(), method);
                m.invoke(this);
            } catch (ReflectionException e) {
                logger.error(e);
            }
            rebuildGui();
            break;
        case ADD_GUI_COMPONENT:
            name = (String) data[0];
            method = "add" + TextUtils.capitalise(name);
            try {
                Method m = ClassReflection.getMethod(this.getClass(), method);
                m.invoke(this);
            } catch (ReflectionException e) {
                logger.error(e);
            }
            rebuildGui();
            break;
        case RA_DEC_UPDATED:
            if (Settings.settings.program.pointer.coordinates) {
                Stage ui = pointerYCoord.getStage();
                float uiScale = Settings.settings.program.ui.scale;
                Double ra = (Double) data[0];
                Double dec = (Double) data[1];
                Integer x = (Integer) data[4];
                Integer y = (Integer) data[5];

                pointerXCoord.setText("RA/".concat(nf.format(ra)).concat("°"));
                pointerXCoord.setPosition(x / uiScale, 1.6f);
                pointerYCoord.setText("DEC/".concat(nf.format(dec)).concat("°"));
                pointerYCoord.setPosition(ui.getWidth() + 1.6f, ui.getHeight() - y / uiScale);
            }
            break;
        case LON_LAT_UPDATED:
            if (Settings.settings.program.pointer.coordinates) {
                Stage ui = pointerYCoord.getStage();
                float uiScale = Settings.settings.program.ui.scale;
                Double lon = (Double) data[0];
                Double lat = (Double) data[1];
                Integer x = (Integer) data[2];
                Integer y = (Integer) data[3];

                pointerXCoord.setText("Lon/".concat(nf.format(lon)).concat("°"));
                pointerXCoord.setPosition(x / uiScale, 1.6f);
                pointerYCoord.setText("Lat/".concat(nf.format(lat)).concat("°"));
                pointerYCoord.setPosition(ui.getWidth() + 1.6f, ui.getHeight() - y / uiScale);
            }
            break;
        case DISPLAY_POINTER_COORDS_CMD:
            Boolean display = (Boolean) data[0];
            pointerXCoord.setVisible(display);
            pointerYCoord.setVisible(display);
            break;
        case POPUP_MENU_FOCUS:
            final IFocus candidate = (IFocus) data[0];
            int screenX = Gdx.input.getX();
            int screenY = Gdx.input.getY();

            GaiaSkyContextMenu popup = new GaiaSkyContextMenu(skin, "default", screenX, screenY, candidate, catalogManager);

            int h = (int) getGuiStage().getHeight();

            float px = screenX / Settings.settings.program.ui.scale;
            float py = h - screenY / Settings.settings.program.ui.scale - 32f;

            popup.showMenu(ui, px, py);

            break;
        case TOGGLE_MINIMAP:
            if (Settings.settings.program.minimap.inWindow) {
                toggleMinimapWindow(ui);
            } else {
                toggleMinimapInterface(ui);
            }
            break;
        case SHOW_MINIMAP_ACTION:
            boolean show = (Boolean) data[0];
            if (Settings.settings.program.minimap.inWindow) {
                showMinimapWindow(ui, show);
            } else {
                showMinimapInterface(ui, show);
            }
            break;
        default:
            break;
        }
    }

    public void setSceneGraph(ISceneGraph sg) {
        this.sg = sg;
    }

    @Override
    public void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible) {
        this.visibilityEntities = entities;
        ComponentType[] vals = ComponentType.values();
        this.visible = new boolean[vals.length];
        for (int i = 0; i < vals.length; i++)
            this.visible[i] = visible.get(vals[i].ordinal());
    }

    public void removeControlsWindow() {
        if (controlsWindow != null) {
            controlsWindow.remove();
            controlsWindow = null;
        }
    }

    public void addControlsWindow() {
        controlsWindow = new ControlsWindow(Settings.settings.getSuperShortApplicationName(), skin, ui, catalogManager);
        controlsWindow.setSceneGraph(sg);
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
        if (minimapInterface == null) {
            minimapInterface = new MinimapInterface(skin, globalResources.getShapeShader(), globalResources.getSpriteShader());
            minimapInterface.setFillParent(true);
            minimapInterface.right().top();
            minimapInterface.pad(pad, 0f, 0f, pad);
            interfaces.add(minimapInterface);
        }
        if (show) {
            // Add to ui
            if (!minimapInterface.hasParent() || minimapInterface.getParent() != ui.getRoot()) {
                ui.addActor(minimapInterface);
            }
        } else {
            // Remove from ui
            minimapInterface.remove();
        }
    }

    public void addLoadProgressInterface(Stage ui) {
        loadProgressInterface = new LoadProgressInterface(400f, skin);
        loadProgressInterface.setFillParent(true);
        loadProgressInterface.center().bottom();
        loadProgressInterface.pad(0, 0, 0, 0);
        interfaces.add(loadProgressInterface);
    }

    public void toggleMinimapInterface(Stage ui) {
        showMinimapInterface(ui, minimapInterface == null || (!minimapInterface.isVisible() || !minimapInterface.hasParent()));
    }

    public void showMinimapWindow(Stage ui, boolean show) {
        if (minimapWindow == null)
            minimapWindow = new MinimapWindow(ui, skin, globalResources.getShapeShader(), globalResources.getSpriteShader());
        if (show)
            minimapWindow.show(ui, graphics.getWidth() - minimapWindow.getWidth(), graphics.getHeight() - minimapWindow.getHeight());
        else
            minimapWindow.hide();
    }

    public void toggleMinimapWindow(Stage ui) {
        showMinimapWindow(ui, minimapWindow == null || (!minimapWindow.isVisible() || !minimapWindow.hasParent()));
    }

    @Override
    public boolean updateUnitsPerPixel(float upp) {
        boolean cool = super.updateUnitsPerPixel(upp);
        if (cool) {
            controlsWindow.setPosition(0, graphics.getHeight() * unitsPerPixel - controlsWindow.getHeight());
            controlsWindow.recalculateSize();
            if (ui.getHeight() < controlsWindow.getHeight()) {
                // Collapse
                controlsWindow.collapseInstant();
            }
        }
        return cool;
    }
}
