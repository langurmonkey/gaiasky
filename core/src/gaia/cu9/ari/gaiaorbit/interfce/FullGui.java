package gaia.cu9.ari.gaiaorbit.interfce;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.data.stars.UncertaintiesHandler;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.interfce.components.VisualEffectsComponent;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.IFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.ISceneGraph;
import gaia.cu9.ari.gaiaorbit.scenegraph.IStarFocus;
import gaia.cu9.ari.gaiaorbit.scenegraph.Planet;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.camera.CameraUtils;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.scene2d.ContextMenu;
import gaia.cu9.ari.gaiaorbit.util.scene2d.MenuItem;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;

/**
 * Full OpenGL GUI with all the controls and whistles.
 * 
 * @author Toni Sagrista
 *
 */
public class FullGui extends AbstractGui {
    private static final Log logger = Logger.getLogger(FullGui.class);
    
    protected ControlsWindow controlsWindow;
    protected MinimapWindow minimapWindow;

    protected Container<FocusInfoInterface> fi;
    protected FocusInfoInterface focusInterface;
    protected NotificationsInterface notificationsInterface;
    protected MessagesInterface messagesInterface;
    protected CustomInterface customInterface;
    protected RunStateInterface runStateInterface;

    protected SearchDialog searchDialog;
    protected AboutWindow aboutWindow;
    protected LogWindow logWindow;
    protected PreferencesWindow preferencesWindow;
    protected VisualEffectsComponent visualEffectsComponent;

    protected INumberFormat nf;
    protected Label pointerXCoord, pointerYCoord;

    protected ISceneGraph sg;
    private ComponentType[] visibilityEntities;
    private boolean[] visible;

    private List<Actor> invisibleInStereoMode;

    // Uncertainties disabled by default
    private boolean uncertainties = false;
    // Rel effects off
    private boolean releffects = false;

    public FullGui() {
        super();
    }

    @Override
    public void initialize(AssetManager assetManager) {
        // User interface
        ui = new Stage(new ScreenViewport(), GlobalResources.spriteBatch);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        logger.info(txt("notif.gui.init"));

        skin = GlobalResources.skin;
        interfaces = new Array<IGuiInterface>();

        buildGui();

        // We must subscribe to the desired events
        EventManager.instance.subscribe(this, Events.FOV_CHANGED_CMD, Events.SHOW_TUTORIAL_ACTION, Events.SHOW_SEARCH_ACTION, Events.REMOVE_KEYBOARD_FOCUS, Events.REMOVE_GUI_COMPONENT, Events.ADD_GUI_COMPONENT, Events.SHOW_ABOUT_ACTION, Events.SHOW_LOG_ACTION, Events.RA_DEC_UPDATED, Events.LON_LAT_UPDATED, Events.POPUP_MENU_FOCUS, Events.SHOW_PREFERENCES_ACTION, Events.SHOW_LAND_AT_LOCATION_ACTION, Events.DISPLAY_POINTER_COORDS_CMD, Events.TOGGLE_MINIMAP);
    }

    protected void buildGui() {
        // Component types name init
        for (ComponentType ct : ComponentType.values()) {
            ct.getName();
        }

        // CONTROLS WINDOW
        addControlsWindow();

        // MINIMAP
        addMinimapWindow();

        nf = NumberFormatFactory.getFormatter("##0.##");

        // FOCUS INFORMATION - BOTTOM RIGHT
        focusInterface = new FocusInfoInterface(skin);
        // focusInterface.setFillParent(true);
        focusInterface.left().top();
        fi = new Container<FocusInfoInterface>(focusInterface);
        fi.setFillParent(true);
        fi.bottom().right();
        fi.pad(0, 0, 10, 10);

        // NOTIFICATIONS INTERFACE - BOTTOM LEFT
        notificationsInterface = new NotificationsInterface(skin, lock, true, true, true, true);
        notificationsInterface.setFillParent(true);
        notificationsInterface.left().bottom();
        notificationsInterface.pad(0, 5, 5, 0);
        interfaces.add(notificationsInterface);

        // MESSAGES INTERFACE - LOW CENTER
        messagesInterface = new MessagesInterface(skin, lock);
        messagesInterface.setFillParent(true);
        messagesInterface.left().bottom();
        messagesInterface.pad(0, 300, 150, 0);
        interfaces.add(messagesInterface);

        // INPUT STATE
        runStateInterface = new RunStateInterface(skin, true);
        runStateInterface.setFillParent(true);
        runStateInterface.center().bottom();
        //runStateInterface.pad(GlobalConf.SCALE_FACTOR == 1 ? 135 : 200, 0, 0, 5);
        runStateInterface.pad(0,0, 5, 0);
        interfaces.add(runStateInterface);

        // CUSTOM OBJECTS INTERFACE
        customInterface = new CustomInterface(ui, skin, lock);
        interfaces.add(customInterface);

        // MOUSE X/Y COORDINATES
        pointerXCoord = new OwnLabel("", skin, "default");
        pointerXCoord.setAlignment(Align.bottom);
        pointerXCoord.setVisible(GlobalConf.program.DISPLAY_POINTER_COORDS);
        pointerYCoord = new OwnLabel("", skin, "default");
        pointerYCoord.setAlignment(Align.right | Align.center);
        pointerYCoord.setVisible(GlobalConf.program.DISPLAY_POINTER_COORDS);

        /** ADD TO UI **/
        rebuildGui();

        // INVISIBLE IN STEREOSCOPIC MODE
        invisibleInStereoMode = new ArrayList<Actor>();
        invisibleInStereoMode.add(controlsWindow);
        invisibleInStereoMode.add(minimapWindow);
        invisibleInStereoMode.add(fi);
        invisibleInStereoMode.add(messagesInterface);
        invisibleInStereoMode.add(runStateInterface);
        // invisibleInStereoMode.add(customInterface);
        invisibleInStereoMode.add(pointerXCoord);
        invisibleInStereoMode.add(pointerYCoord);
    }

    public void recalculateOptionsSize() {
        controlsWindow.recalculateSize();
    }

    protected void rebuildGui() {

        if (ui != null) {
            ui.clear();
            boolean collapsed = false;
            if (controlsWindow != null) {
                collapsed = controlsWindow.isCollapsed();
                recalculateOptionsSize();
                if (collapsed)
                    controlsWindow.collapseInstant();
                controlsWindow.setPosition(0, Gdx.graphics.getHeight() - controlsWindow.getHeight());
                ui.addActor(controlsWindow);
            }
            if (minimapWindow != null) {
                minimapWindow.setPosition(Gdx.graphics.getWidth() - minimapWindow.getWidth() * 2, 0);
                ui.addActor(minimapWindow);
            }
            if (notificationsInterface != null)
                ui.addActor(notificationsInterface);
            if (messagesInterface != null)
                ui.addActor(messagesInterface);
            if (focusInterface != null && !GlobalConf.runtime.STRIPPED_FOV_MODE)
                ui.addActor(fi);
            if (runStateInterface != null) {
                ui.addActor(runStateInterface);
            }
            if (pointerXCoord != null && pointerYCoord != null) {
                ui.addActor(pointerXCoord);
                ui.addActor(pointerYCoord);
            }

            if (customInterface != null) {
                customInterface.reAddObjects();
            }

            /** CAPTURE SCROLL FOCUS **/
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
        notificationsInterface.update();
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case SHOW_TUTORIAL_ACTION:
            EventManager.instance.post(Events.RUN_SCRIPT_PATH, GlobalConf.program.TUTORIAL_SCRIPT_LOCATION);
            break;
        case SHOW_SEARCH_ACTION:
            if (searchDialog == null) {
                searchDialog = new SearchDialog(this, skin, sg);
            } else {
                searchDialog.clearText();
            }
            searchDialog.display();
            break;
        case SHOW_LAND_AT_LOCATION_ACTION:
            CelestialBody target = (CelestialBody) data[0];
            LandAtWindow landAtLocation = new LandAtWindow(target, ui, skin);
            landAtLocation.show(ui);
            break;
        case SHOW_ABOUT_ACTION:
            if (aboutWindow == null) {
                aboutWindow = new AboutWindow(ui, skin);
            }
            aboutWindow.show(ui);
            break;
        case SHOW_LOG_ACTION:
            if (logWindow == null) {
                logWindow = new LogWindow(ui, skin);
            }
            logWindow.update();
            logWindow.show(ui);
            break;
        case SHOW_PREFERENCES_ACTION:
            preferencesWindow = new PreferencesWindow(ui, skin);
            preferencesWindow.show(ui);
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
            if (GlobalConf.program.DISPLAY_POINTER_COORDS) {
                Double ra = (Double) data[0];
                Double dec = (Double) data[1];
                Integer x = (Integer) data[4];
                Integer y = (Integer) data[5];

                pointerXCoord.setText("RA/".concat(nf.format(ra)).concat("°"));
                pointerXCoord.setPosition(x, GlobalConf.SCALE_FACTOR);
                pointerYCoord.setText("DEC/".concat(nf.format(dec)).concat("°"));
                pointerYCoord.setPosition(Gdx.graphics.getWidth() + GlobalConf.SCALE_FACTOR, Gdx.graphics.getHeight() - y);
            }
            break;
        case LON_LAT_UPDATED:
            if (GlobalConf.program.DISPLAY_POINTER_COORDS) {
                Double lon = (Double) data[0];
                Double lat = (Double) data[1];
                Integer x = (Integer) data[2];
                Integer y = (Integer) data[3];

                pointerXCoord.setText("Lon/".concat(nf.format(lon)).concat("°"));
                pointerXCoord.setPosition(x, GlobalConf.SCALE_FACTOR);
                pointerYCoord.setText("Lat/".concat(nf.format(lat)).concat("°"));
                pointerYCoord.setPosition(Gdx.graphics.getWidth() + GlobalConf.SCALE_FACTOR, Gdx.graphics.getHeight() - y);
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

            ContextMenu popup = new ContextMenu(skin, "default");

            if (candidate != null) {
                MenuItem select = new MenuItem(txt("context.select", candidate.getCandidateName()), skin, "default");
                select.addListener(new EventListener() {

                    @Override
                    public boolean handle(Event event) {
                        if (event instanceof ChangeEvent) {
                            EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.Focus);
                            EventManager.instance.post(Events.FOCUS_CHANGE_CMD, candidate);
                        }
                        return false;
                    }

                });
                popup.addItem(select);

                MenuItem go = new MenuItem(txt("context.goto", candidate.getCandidateName()), skin, "default");
                go.addListener(new EventListener() {

                    @Override
                    public boolean handle(Event event) {
                        if (event instanceof ChangeEvent) {
                            candidate.makeFocus();
                            EventManager.instance.post(Events.NAVIGATE_TO_OBJECT, candidate);
                        }
                        return false;
                    }

                });
                popup.addItem(go);

                if (candidate instanceof Planet) {
                    popup.addSeparator();

                    MenuItem landOn = new MenuItem(txt("context.landon", candidate.getCandidateName()), skin, "default");
                    landOn.addListener(new EventListener() {

                        @Override
                        public boolean handle(Event event) {
                            if (event instanceof ChangeEvent) {
                                EventManager.instance.post(Events.LAND_ON_OBJECT, candidate);
                                return true;
                            }
                            return false;
                        }

                    });
                    popup.addItem(landOn);

                    double[] lonlat = new double[2];
                    boolean ok = CameraUtils.getLonLat((Planet) candidate, GaiaSky.instance.getICamera(), screenX, screenY, new Vector3(), new Vector3(), new Vector3(), new Vector3(), new Vector3d(), new Vector3d(), new Matrix4(), lonlat);
                    if (ok) {
                        final Double pointerLon = lonlat[0];
                        final Double pointerLat = lonlat[1];
                        // Add mouse pointer
                        MenuItem landOnPointer = new MenuItem(txt("context.landatpointer", candidate.getCandidateName()), skin, "default");
                        landOnPointer.addListener(new EventListener() {

                            @Override
                            public boolean handle(Event event) {
                                if (event instanceof ChangeEvent) {
                                    EventManager.instance.post(Events.LAND_AT_LOCATION_OF_OBJECT, candidate, pointerLon, pointerLat);
                                    return true;
                                }
                                return false;
                            }

                        });
                        popup.addItem(landOnPointer);
                    }

                    MenuItem landOnCoord = new MenuItem(txt("context.landatcoord", candidate.getCandidateName()), skin, "default");
                    landOnCoord.addListener(new EventListener() {

                        @Override
                        public boolean handle(Event event) {
                            if (event instanceof ChangeEvent) {
                                EventManager.instance.post(Events.SHOW_LAND_AT_LOCATION_ACTION, candidate);
                                return true;
                            }
                            return false;
                        }

                    });
                    popup.addItem(landOnCoord);
                }

                if (candidate instanceof IStarFocus && uncertainties) {
                    boolean sep = false;
                    if (UncertaintiesHandler.getInstance().containsStar(candidate.getCandidateId())) {
                        popup.addSeparator();
                        sep = true;

                        MenuItem showUncertainties = new MenuItem(txt("context.showuncertainties"), skin, "default");
                        showUncertainties.addListener(new EventListener() {

                            @Override
                            public boolean handle(Event event) {
                                if (event instanceof ChangeEvent) {
                                    EventManager.instance.post(Events.SHOW_UNCERTAINTIES, candidate);
                                    return true;
                                }
                                return false;
                            }

                        });
                        popup.addItem(showUncertainties);
                    }

                    if (UncertaintiesHandler.getInstance().containsUncertainties()) {
                        if (!sep)
                            popup.addSeparator();

                        MenuItem hideUncertainties = new MenuItem(txt("context.hideuncertainties"), skin, "default");
                        hideUncertainties.addListener(new EventListener() {

                            @Override
                            public boolean handle(Event event) {
                                if (event instanceof ChangeEvent) {
                                    EventManager.instance.post(Events.HIDE_UNCERTAINTIES, candidate);
                                    return true;
                                }
                                return false;
                            }

                        });
                        popup.addItem(hideUncertainties);

                    }
                }

                popup.addSeparator();
            }

            if (releffects) {
                // Spawn gravitational waves
                MenuItem gravWaveStart = new MenuItem(txt("context.startgravwave"), skin, "default");
                gravWaveStart.addListener(new EventListener() {

                    @Override
                    public boolean handle(Event event) {
                        if (event instanceof ChangeEvent) {
                            EventManager.instance.post(Events.GRAV_WAVE_START, screenX, screenY);
                            return true;
                        }
                        return false;
                    }

                });
                popup.addItem(gravWaveStart);

                if (RelativisticEffectsManager.getInstance().gravWavesOn()) {
                    // Cancel gravitational waves
                    MenuItem gravWaveStop = new MenuItem(txt("context.stopgravwave"), skin, "default");
                    gravWaveStop.addListener(new EventListener() {

                        @Override
                        public boolean handle(Event event) {
                            if (event instanceof ChangeEvent) {
                                EventManager.instance.post(Events.GRAV_WAVE_STOP);
                                return true;
                            }
                            return false;
                        }

                    });
                    popup.addItem(gravWaveStop);
                }
            }

            int mx = Gdx.input.getX();
            int my = Gdx.input.getY();
            int h = Gdx.graphics.getHeight();

            float px = mx;
            float py = h - my - 20 * GlobalConf.SCALE_FACTOR;

            popup.showMenu(ui, px, py);

            break;
        case TOGGLE_MINIMAP:
            if (minimapWindow != null) {
                minimapWindow.setVisible(!minimapWindow.isVisible());
            }
            break;
        default:
            break;
        }

    }

    @Override
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
        controlsWindow = new ControlsWindow(txt("gui.controlpanel"), skin, ui);
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

    public void addMinimapWindow() {
        minimapWindow = new MinimapWindow(ui, skin);
        minimapWindow.setVisible(false);
    }

}
