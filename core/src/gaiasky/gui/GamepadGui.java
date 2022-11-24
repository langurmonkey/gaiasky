package gaiasky.gui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.input.AbstractGamepadListener;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.view.FilterView;
import gaiasky.scene.view.FocusView;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.util.*;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GUI that is operated with a game controller and optimized for that purpose.
 */
public class GamepadGui extends AbstractGui {
    private static final Logger.Log logger = Logger.getLogger(GamepadGui.class.getSimpleName());

    private final Table content, menu;
    private Table searchT, camT, timeT, graphicsT, typesT, controlsT, sysT;
    private Cell<?> contentCell, infoCell;
    private OwnTextButton searchButton, cameraButton, timeButton, graphicsButton, typesButton, controlsButton, systemButton;
    // Contains a matrix (column major) of actors for each tab
    private final List<Actor[][]> model;
    private OwnTextButton cameraFocus, cameraFree, cameraCinematic;
    private OwnTextButton timeStartStop, timeUp, timeDown, timeReset, quit, motionBlurButton, flareButton, starGlowButton, invertYButton, invertXButton;
    private OwnSliderPlus fovSlider, camSpeedSlider, camRotSlider, camTurnSlider, bloomSlider;
    private OwnTextField searchField;
    private OwnLabel infoMessage;

    private final List<OwnTextButton> tabButtons;
    private final List<ScrollPane> tabContents;

    private Actor[][] currentModel;
    private Scene scene;

    private final EventManager em;
    private final GUIControllerListener guiControllerListener;
    private final float pad5;
    private final float pad10;
    private final float pad20;
    private final float pad30;
    private String currentInputText = "";
    private final FocusView view;
    private final FilterView filterView;

    private int selectedTab = 0;
    private int fi = 0, fj = 0;

    public GamepadGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        this.em = EventManager.instance;
        model = new ArrayList<>();
        content = new Table(skin);
        menu = new Table(skin);
        guiControllerListener = new GUIControllerListener(Settings.settings.controls.gamepad.mappingsFile);
        tabButtons = new ArrayList<>();
        tabContents = new ArrayList<>();
        pad5 = 8f;
        pad10 = 16f;
        pad20 = 32f;
        pad30 = 48;
        view = new FocusView();
        filterView = new FilterView();
    }

    @Override
    protected void rebuildGui() {

        // Clean up
        content.clear();
        menu.clear();
        tabButtons.clear();
        tabContents.clear();
        model.clear();

        float w = 1440f;
        float h = 640f;
        // Widget width
        float ww = 400f;
        float wh = 64f;
        float sh = 96f;
        float tfw = 240f;
        // Tab width
        float tw = 224f;

        // Create contents

        // SEARCH
        Actor[][] searchModel = new Actor[11][5];
        model.add(searchModel);

        searchT = new Table(skin);
        searchT.setSize(w, h);

        searchField = new OwnTextField("", skin, "big");
        searchField.setProgrammaticChangeEvents(true);
        searchField.setSize(ww, wh);
        searchField.setMessageText("Search...");
        searchField.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                ChangeEvent ie = (ChangeEvent) event;
                if (!searchField.getText().equals(currentInputText) && !searchField.getText().isBlank()) {
                    // Process only if text changed
                    currentInputText = searchField.getText();
                    String name = currentInputText.toLowerCase().trim();
                    if (!checkString(name, scene)) {
                        if (name.matches("[0-9]+")) {
                            // Check with 'HIP '
                            checkString("hip " + name, scene);
                        } else if (name.matches("hip [0-9]+") || name.matches("HIP [0-9]+")) {
                            // Check without 'HIP '
                            checkString(name.substring(4), scene);
                        }
                    }
                }

            }
            return false;
        });
        searchT.add(searchField).colspan(11).padBottom(pad5).row();

        infoMessage = new OwnLabel("", skin, "default-blue");
        infoCell = searchT.add();
        infoCell.colspan(10).padBottom(pad20).row();

        // First row
        addTextKey("Q", searchModel, 0, 0, false);
        addTextKey("W", searchModel, 1, 0, false);
        addTextKey("E", searchModel, 2, 0, false);
        addTextKey("R", searchModel, 3, 0, false);
        addTextKey("T", searchModel, 4, 0, false);
        addTextKey("Y", searchModel, 5, 0, false);
        addTextKey("U", searchModel, 6, 0, false);
        addTextKey("I", searchModel, 7, 0, false);
        addTextKey("O", searchModel, 8, 0, false);
        addTextKey("P", searchModel, 9, 0, false);
        addTextKey("<--", (event) -> {
            if (event instanceof ChangeEvent) {
                if (!searchField.getText().isBlank()) {
                    searchField.setText(searchField.getText().substring(0, searchField.getText().length() - 1));
                }
            }
            return false;
        }, searchModel, 10, 0, true, tfw / 1.5f, pad10, 0);
        // Second row
        searchT.add().padRight(pad5).padBottom(pad10);
        addTextKey("A", searchModel, 1, 1, false);
        addTextKey("S", searchModel, 2, 1, false);
        addTextKey("D", searchModel, 3, 1, false);
        addTextKey("F", searchModel, 4, 1, false);
        addTextKey("G", searchModel, 5, 1, false);
        addTextKey("H", searchModel, 6, 1, false);
        addTextKey("J", searchModel, 7, 1, false);
        addTextKey("K", searchModel, 8, 1, false);
        addTextKey("L", searchModel, 9, 1, false);
        addTextKey("Clear", (event) -> {
            if (event instanceof ChangeEvent) {
                if (!searchField.getText().isBlank()) {
                    searchField.setText("");
                }
            }
            return false;
        }, searchModel, 10, 1, true, tfw / 1.5f, pad10, 0);
        // Third row
        searchT.add().padRight(pad5).padBottom(pad10);
        searchT.add().padRight(pad5).padBottom(pad10);
        addTextKey("Z", searchModel, 2, 2, false);
        addTextKey("X", searchModel, 3, 2, false);
        addTextKey("C", searchModel, 4, 2, false);
        addTextKey("V", searchModel, 5, 2, false);
        addTextKey("B", searchModel, 6, 2, false);
        addTextKey("N", searchModel, 7, 2, false);
        addTextKey("M", searchModel, 8, 2, false);
        addTextKey("-", searchModel, 9, 2, true);

        // Fourth row
        searchT.add().padRight(pad5).padBottom(pad10);
        searchT.add().padRight(pad5).padBottom(pad10);
        searchT.add().padRight(pad5).padBottom(pad10);
        addTextKey("SPACE", (event) -> {
            if (event instanceof ChangeEvent) {
                searchField.setText(searchField.getText() + " ");
            }
            return false;
        }, searchModel, 5, 3, false, tfw * 2f, pad5, 6);

        tabContents.add(container(searchT, w, h));
        updatePads(searchT);

        // CAMERA
        Actor[][] cameraModel = new Actor[2][4];
        model.add(cameraModel);

        camT = new Table(skin);
        camT.setSize(w, h);
        CameraManager cm = GaiaSky.instance.getCameraManager();

        // Focus
        cameraFocus = new OwnTextButton(CameraMode.FOCUS_MODE.toStringI18n(), skin, "toggle-big");
        cameraModel[0][0] = cameraFocus;
        cameraFocus.setWidth(ww);
        cameraFocus.setChecked(cm.getMode().isFocus());
        cameraFocus.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (cameraFocus.isChecked()) {
                    em.post(Event.CAMERA_MODE_CMD, cameraFocus, CameraMode.FOCUS_MODE);
                    cameraFree.setProgrammaticChangeEvents(false);
                    cameraFree.setChecked(false);
                    cameraFree.setProgrammaticChangeEvents(true);
                }
                return true;
            }
            return false;
        });

        // Free
        cameraFree = new OwnTextButton(CameraMode.FREE_MODE.toStringI18n(), skin, "toggle-big");
        cameraModel[0][1] = cameraFree;
        cameraFree.setWidth(ww);
        cameraFree.setChecked(cm.getMode().isFree());
        cameraFree.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (cameraFree.isChecked()) {
                    em.post(Event.CAMERA_MODE_CMD, cameraFree, CameraMode.FREE_MODE);
                    cameraFocus.setProgrammaticChangeEvents(false);
                    cameraFocus.setChecked(false);
                    cameraFocus.setProgrammaticChangeEvents(true);
                }
                return true;
            }
            return false;
        });

        // Cinematic
        cameraCinematic = new OwnTextButton(I18n.msg("gui.camera.cinematic"), skin, "toggle-big");
        cameraModel[0][2] = cameraCinematic;
        cameraCinematic.setWidth(ww);
        cameraCinematic.setChecked(Settings.settings.scene.camera.cinematic);
        cameraCinematic.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Event.CAMERA_CINEMATIC_CMD, cameraCinematic, cameraCinematic.isChecked());
                return true;
            }
            return false;
        });

        // FOV
        fovSlider = new OwnSliderPlus(I18n.msg("gui.camera.fov"), Constants.MIN_FOV, Constants.MAX_FOV, Constants.SLIDER_STEP_SMALL, false, skin, "header-raw");
        cameraModel[1][0] = fovSlider;
        fovSlider.setValueSuffix("°");
        fovSlider.setName("field of view");
        fovSlider.setWidth(ww);
        fovSlider.setHeight(sh);
        fovSlider.setValue(Settings.settings.scene.camera.fov);
        fovSlider.setDisabled(Settings.settings.program.modeCubemap.isFixedFov());
        fovSlider.addListener(event -> {
            if (event instanceof ChangeEvent && !SlaveManager.projectionActive() && !Settings.settings.program.modeCubemap.isFixedFov()) {
                float value = fovSlider.getMappedValue();
                EventManager.publish(Event.FOV_CHANGED_CMD, fovSlider, value);
                return true;
            }
            return false;
        });

        // Speed
        camSpeedSlider = new OwnSliderPlus(I18n.msg("gui.camera.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_CAM_SPEED, Constants.MAX_CAM_SPEED, skin, "header-raw");
        cameraModel[1][1] = camSpeedSlider;
        camSpeedSlider.setName("camera speed");
        camSpeedSlider.setWidth(ww);
        camSpeedSlider.setHeight(sh);
        camSpeedSlider.setMappedValue(Settings.settings.scene.camera.speed);
        camSpeedSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.CAMERA_SPEED_CMD, camSpeedSlider, camSpeedSlider.getMappedValue(), false);
                return true;
            }
            return false;
        });

        // Rot
        camRotSlider = new OwnSliderPlus(I18n.msg("gui.rotation.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED, skin, "header-raw");
        cameraModel[1][2] = camRotSlider;
        camRotSlider.setName("rotate speed");
        camRotSlider.setWidth(ww);
        camRotSlider.setHeight(sh);
        camRotSlider.setMappedValue(Settings.settings.scene.camera.rotate);
        camRotSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.ROTATION_SPEED_CMD, camRotSlider, camRotSlider.getMappedValue());
                return true;
            }
            return false;
        });

        // Turn
        camTurnSlider = new OwnSliderPlus(I18n.msg("gui.turn.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED, skin, "header-raw");
        cameraModel[1][3] = camTurnSlider;
        camTurnSlider.setName("turn speed");
        camTurnSlider.setWidth(ww);
        camTurnSlider.setHeight(sh);
        camTurnSlider.setMappedValue(Settings.settings.scene.camera.turn);
        camTurnSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.TURNING_SPEED_CMD, camTurnSlider, camTurnSlider.getMappedValue(), false);
                return true;
            }
            return false;
        });

        camT.add(cameraFocus).padBottom(pad10).padRight(pad30);
        camT.add(fovSlider).padBottom(pad10).row();

        camT.add(cameraFree).padBottom(pad10).padRight(pad30);
        camT.add(camSpeedSlider).padBottom(pad10).row();

        camT.add(cameraCinematic).padBottom(pad10).padRight(pad30);
        camT.add(camRotSlider).padBottom(pad10).row();

        camT.add().padBottom(pad10).padRight(pad30);
        camT.add(camTurnSlider).padBottom(pad10).row();

        tabContents.add(container(camT, w, h));
        updatePads(camT);

        // TIME
        Actor[][] timeModel = new Actor[3][2];
        model.add(timeModel);

        timeT = new Table(skin);

        boolean timeOn = Settings.settings.runtime.timeOn;
        timeStartStop = new OwnTextButton(I18n.msg(timeOn ? "gui.time.pause" : "gui.time.start"), skin, "toggle-big");
        timeModel[1][0] = timeStartStop;
        timeStartStop.setWidth(ww);
        timeStartStop.setChecked(timeOn);
        timeStartStop.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Event.TIME_STATE_CMD, timeStartStop, timeStartStop.isChecked());
                timeStartStop.setText(I18n.msg(timeStartStop.isChecked() ? "gui.time.pause" : "gui.time.start"));
                return true;
            }
            return false;
        });
        timeUp = new OwnTextIconButton(I18n.msg("gui.time.speedup"), Align.right, skin, "fwd");
        timeModel[2][0] = timeUp;
        timeUp.setWidth(ww);
        timeUp.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Event.TIME_WARP_INCREASE_CMD, timeUp);
                return true;
            }
            return false;
        });
        timeDown = new OwnTextIconButton(I18n.msg("gui.time.slowdown"), skin, "bwd");
        timeModel[0][0] = timeDown;
        timeDown.setWidth(ww);
        timeDown.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Event.TIME_WARP_DECREASE_CMD, timeDown);
                return true;
            }
            return false;
        });
        timeReset = new OwnTextIconButton(I18n.msg("action.resettime"), Align.center, skin, "reload");
        timeModel[1][1] = timeReset;
        timeReset.setWidth(ww);
        timeReset.addListener(event -> {
            if (event instanceof ChangeEvent) {
                em.post(Event.TIME_CHANGE_CMD, timeReset, Instant.now());
                return true;
            }
            return false;
        });

        timeT.add(timeDown).padBottom(pad30).padRight(pad10);
        timeT.add(timeStartStop).padBottom(pad30).padRight(pad10);
        timeT.add(timeUp).padBottom(pad30).row();
        timeT.add(timeReset).padRight(pad10).padLeft(pad10).colspan(3).padBottom(pad10).row();
        tabContents.add(container(timeT, w, h));
        updatePads(timeT);

        // TYPES
        int visTableCols = 6;
        Actor[][] typesModel = new Actor[visTableCols][7];
        model.add(typesModel);

        typesT = new Table(skin);

        float buttonPadHor = 9.6f;
        float buttonPadVert = 4f;
        ComponentType[] visibilityEntities = ComponentType.values();
        boolean[] visible = new boolean[visibilityEntities.length];
        for (int i = 0; i < visibilityEntities.length; i++)
            visible[i] = GaiaSky.instance.sceneRenderer.visible.get(visibilityEntities[i].ordinal());

        int di = 0, dj = 0;
        for (int i = 0; i < visibilityEntities.length; i++) {
            final ComponentType ct = visibilityEntities[i];
            final String name = ct.getName();
            if (name != null) {
                Button button;
                if (ct.style != null) {
                    Image icon = new Image(skin.getDrawable(ct.style));
                    button = new OwnTextIconButton("", icon, skin, "toggle");
                } else {
                    button = new OwnTextButton(name, skin, "toggle");
                }
                // Name is the key
                button.setName(ct.key);
                typesModel[di][dj] = button;

                button.setChecked(visible[i]);
                button.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, button, ct.key, button.isChecked());
                        return true;
                    }
                    return false;
                });
                Cell c = typesT.add(button).padBottom(buttonPadVert).left();

                if ((i + 1) % visTableCols == 0) {
                    typesT.row();
                    di = 0;
                    dj++;
                } else {
                    c.padRight(buttonPadHor);
                    di++;

                }
            }
        }

        typesT.setSize(w, h);
        tabContents.add(container(typesT, w, h));
        updatePads(typesT);

        // CONTROLS
        Actor[][] controlsModel = new Actor[1][3];
        model.add(controlsModel);

        controlsT = new Table(skin);

        // Controllers list
        var controllers = Controllers.getControllers();
        Controller controller = null;
        for (var c : controllers) {
            if (!Settings.settings.controls.gamepad.isControllerBlacklisted(c.getName())) {
                // Found it!
                controller = c;
                break;
            }
        }
        if (controller == null) {
            // Error!
            OwnLabel noControllers = new OwnLabel(I18n.msg("gui.controller.nocontrollers"), skin, "header-blue");
            controlsT.add(noControllers).padBottom(pad10).row();
            controlsModel[0][0] = new OwnTextButton("", skin, "toggle-big");
        } else {
            final var controllerName = controller.getName();
            Table controllerTable = new Table(skin);

            OwnLabel detectedController = new OwnLabel(I18n.msg("gui.controller.detected"), skin, "header-raw");
            OwnLabel controllerNameLabel = new OwnLabel(controllerName, skin, "header-blue");

            OwnTextButton configureControllerButton = new OwnTextButton(I18n.msg("gui.controller.configure"), skin, "big");
            configureControllerButton.setWidth(ww);
            configureControllerButton.setHeight(wh);
            configureControllerButton.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    // Get currently selected mappings
                    GamepadMappings mappings = new GamepadMappings(controllerName, Path.of(Settings.settings.controls.gamepad.mappingsFile));
                    GamepadConfigWindow ccw = new GamepadConfigWindow(controllerName, mappings, ui, skin);
                    ccw.setAcceptRunnable(() -> {
                        if (ccw.savedFile != null) {
                            // File was saved, reload, select
                            EventManager.publish(Event.RELOAD_CONTROLLER_MAPPINGS, this, ccw.savedFile.toAbsolutePath().toString());
                        }
                    });
                    ccw.show(ui);
                    return true;
                }
                return false;
            });

            controllerTable.add(detectedController).padBottom(pad10).padRight(pad20);
            controllerTable.add(controllerNameLabel).padBottom(pad10).row();
            controllerTable.add(configureControllerButton).center().colspan(2);

            controlsT.add(controllerTable).padBottom(pad20 * 2).row();

            controlsModel[0][0] = configureControllerButton;
        }

        // Invert X
        invertXButton = new OwnTextButton(I18n.msg("gui.controller.axis.invert", "X"), skin, "toggle-big");
        controlsModel[0][1] = invertXButton;
        invertXButton.setWidth(ww);
        invertXButton.setChecked(Settings.settings.controls.gamepad.invertX);
        invertXButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.INVERT_X_CMD, invertXButton, invertXButton.isChecked());
                return true;
            }
            return false;
        });
        controlsT.add(invertXButton).padBottom(pad10).row();

        // Invert Y
        invertYButton = new OwnTextButton(I18n.msg("gui.controller.axis.invert", "Y"), skin, "toggle-big");
        controlsModel[0][2] = invertYButton;
        invertYButton.setWidth(ww);
        invertYButton.setChecked(Settings.settings.controls.gamepad.invertY);
        invertYButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.INVERT_Y_CMD, invertYButton, invertYButton.isChecked());
                return true;
            }
            return false;
        });
        controlsT.add(invertYButton);

        controlsT.setSize(w, h);
        tabContents.add(container(controlsT, w, h));
        updatePads(controlsT);

        // GRAPHICS
        Actor[][] graphicsModel = new Actor[1][4];
        model.add(graphicsModel);

        graphicsT = new Table(skin);

        // Slider
        bloomSlider = new OwnSliderPlus(I18n.msg("gui.bloom"), Constants.MIN_SLIDER, Constants.MAX_SLIDER * 0.2f, 1f, false, skin, "header-raw");
        bloomSlider.setWidth(ww);
        bloomSlider.setHeight(sh);
        bloomSlider.setValue(Settings.settings.postprocess.bloom.intensity * 10f);
        bloomSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.BLOOM_CMD, bloomSlider, bloomSlider.getValue() / 10f);
                return true;
            }
            return false;
        });
        graphicsModel[0][0] = bloomSlider;
        graphicsT.add(bloomSlider).padBottom(pad10).row();

        // Lens flare
        flareButton = new OwnTextButton(I18n.msg("gui.lensflare"), skin, "toggle-big");
        graphicsModel[0][1] = flareButton;
        flareButton.setWidth(ww);
        flareButton.setChecked(Settings.settings.postprocess.lensFlare.active);
        flareButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.LENS_FLARE_CMD, flareButton, flareButton.isChecked());
                return true;
            }
            return false;
        });
        graphicsT.add(flareButton).padBottom(pad10).row();

        // Star glow
        starGlowButton = new OwnTextButton(I18n.msg("gui.lightscattering"), skin, "toggle-big");
        graphicsModel[0][2] = starGlowButton;
        starGlowButton.setWidth(ww);
        starGlowButton.setChecked(Settings.settings.postprocess.lightGlow.active);
        starGlowButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.LIGHT_GLOW_CMD, starGlowButton, starGlowButton.isChecked());
                return true;
            }
            return false;
        });
        graphicsT.add(starGlowButton).padBottom(pad10).row();

        // Motion blur
        motionBlurButton = new OwnTextButton(I18n.msg("gui.motionblur"), skin, "toggle-big");
        graphicsModel[0][3] = motionBlurButton;
        motionBlurButton.setWidth(ww);
        motionBlurButton.setChecked(Settings.settings.postprocess.motionBlur.active);
        motionBlurButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.MOTION_BLUR_CMD, motionBlurButton, motionBlurButton.isChecked());
                return true;
            }
            return false;
        });
        graphicsT.add(motionBlurButton);

        tabContents.add(container(graphicsT, w, h));
        updatePads(graphicsT);

        // SYSTEM
        Actor[][] systemModel = new Actor[1][1];
        model.add(systemModel);

        sysT = new Table(skin);

        quit = new OwnTextIconButton(I18n.msg("gui.quit.title"), Align.center, skin, "quit");
        systemModel[0][0] = quit;
        quit.setWidth(ww);
        quit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                GaiaSky.postRunnable(() -> Gdx.app.exit());
                return true;
            }
            return false;
        });
        sysT.add(quit);

        tabContents.add(container(sysT, w, h));
        updatePads(sysT);

        // Create tab buttons
        searchButton = new OwnTextButton(I18n.msg("gui.search"), skin, "toggle-big");
        tabButtons.add(searchButton);
        searchButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                selectedTab = tabButtons.indexOf(searchButton);
                updateTabs();
            }
            return false;
        });

        cameraButton = new OwnTextButton(I18n.msg("gui.camera"), skin, "toggle-big");
        tabButtons.add(cameraButton);
        cameraButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                selectedTab = tabButtons.indexOf(cameraButton);
                updateTabs();
            }
            return false;
        });

        timeButton = new OwnTextButton(I18n.msg("gui.time"), skin, "toggle-big");
        tabButtons.add(timeButton);
        timeButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                selectedTab = tabButtons.indexOf(timeButton);
                updateTabs();
            }
            return false;
        });

        typesButton = new OwnTextButton(I18n.msg("gui.types"), skin, "toggle-big");
        tabButtons.add(typesButton);
        typesButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                selectedTab = tabButtons.indexOf(typesButton);
                updateTabs();
            }
            return false;
        });

        controlsButton = new OwnTextButton(I18n.msg("gui.controls"), skin, "toggle-big");
        tabButtons.add(controlsButton);
        controlsButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                selectedTab = tabButtons.indexOf(controlsButton);
                updateTabs();
            }
            return false;
        });

        graphicsButton = new OwnTextButton(I18n.msg("gui.graphics"), skin, "toggle-big");
        tabButtons.add(graphicsButton);
        graphicsButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                selectedTab = tabButtons.indexOf(graphicsButton);
                updateTabs();
            }
            return false;
        });

        systemButton = new OwnTextButton(I18n.msg("gui.system"), skin, "toggle-big");
        tabButtons.add(systemButton);
        systemButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                selectedTab = tabButtons.indexOf(systemButton);
                updateTabs();
            }
            return false;
        });

        // Tab buttons styling.
        for (OwnTextButton b : tabButtons) {
            b.pad(pad10);
            b.setMinWidth(tw);
        }

        // Left and Right indicators.
        OwnTextButton lb, rb;
        rb = new OwnTextIconButton("RB", Align.left, skin, "caret-down");
        rb.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                tabRight();
            }
            return false;
        });
        rb.pad(pad10);
        lb = new OwnTextIconButton("LB", Align.left, skin, "caret-up");
        lb.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                tabLeft();
            }
            return false;
        });
        lb.pad(pad10);
        menu.add(lb).center().padBottom(pad30).row();
        menu.add(searchButton).left().row();
        menu.add(cameraButton).left().row();
        menu.add(timeButton).left().row();
        menu.add(typesButton).left().row();
        menu.add(controlsButton).left().row();
        menu.add(graphicsButton).left().row();
        menu.add(systemButton).left().padBottom(pad30).row();
        menu.add(rb).center();

        Table padTable = new Table(skin);
        padTable.pad(pad30);
        padTable.setBackground("table-border");
        menu.pack();
        padTable.add(menu).left();
        contentCell = padTable.add();

        content.add(padTable);

        content.setFillParent(true);
        content.center();
        content.pack();

        updateTabs();
        updateFocused(true);

        if (scene == null)
            scene = GaiaSky.instance.scene;
    }

    private void addTextKey(String text, Actor[][] m, int i, int j, boolean nl) {
        addTextKey(text, (event) -> {
            if (event instanceof ChangeEvent) {
                searchField.setText(searchField.getText() + text.toLowerCase());
            }
            return false;
        }, m, i, j, nl, -1, pad5, 1);
    }

    private void addTextIconKey(String text, String style, int align, EventListener el, Actor[][] m, int i, int j, boolean nl, float width, float padRight, int colspan) {
        OwnTextButton key = new OwnTextIconButton(text, align, skin, style);
        if (width > 0)
            key.setWidth(width);
        key.addListener(el);
        m[i][j] = key;
        Cell<?> c = searchT.add(key).padRight(pad5).padBottom(pad10);
        if (nl)
            c.row();
        if (colspan > 1)
            c.colspan(colspan);
    }

    private void addTextKey(String text, EventListener el, Actor[][] m, int i, int j, boolean nl, float width, float padRight, int colspan) {
        OwnTextButton key = new OwnTextButton(text, skin, "big");
        if (width > 0)
            key.setWidth(width);
        key.addListener(el);
        m[i][j] = key;
        Cell<?> c = searchT.add(key).padRight(pad5).padBottom(pad10);
        if (nl)
            c.row();
        if (colspan > 1)
            c.colspan(colspan);
    }

    public boolean checkString(String text, Scene scene) {
        try {
            if (scene.index().containsEntity(text)) {
                Entity node = scene.index().getEntity(text);
                if (Mapper.focus.has(node)) {
                    view.setEntity(node);
                    view.getFocus(text);
                    filterView.setEntity(node);
                    boolean timeOverflow = view.isCoordinatesTimeOverflow();
                    boolean canSelect = !view.isSet() || view.getSet().canSelect(filterView);
                    boolean ctOn = GaiaSky.instance.isOn(view.getCt());
                    if (!timeOverflow && canSelect && ctOn) {
                        GaiaSky.postRunnable(() -> {
                            EventManager.publish(Event.CAMERA_MODE_CMD, this, CameraMode.FOCUS_MODE, true);
                            EventManager.publish(Event.FOCUS_CHANGE_CMD, this, node, true);
                        });
                        info(null);
                    } else if (timeOverflow) {
                        info(I18n.msg("gui.objects.search.timerange", text));
                    } else if (!canSelect) {
                        info(I18n.msg("gui.objects.search.filter", text));
                    } else {
                        info(I18n.msg("gui.objects.search.invisible", text, view.getCt().toString()));
                    }
                    return true;
                }
            } else {
                info(null);
            }
        } catch (Exception e) {
            logger.error(e);
        }
        return false;
    }

    private void info(String info) {
        if (info == null) {
            infoMessage.setText("");
            info(false);
        } else {
            infoMessage.setText(info);
            info(true);
        }
    }

    private void info(boolean visible) {
        if (visible) {
            infoCell.setActor(infoMessage);
        } else {
            infoCell.setActor(null);
        }
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        ui = new Stage(vp, sb);

        // Comment to hide this whole dialog and functionality
        EventManager.instance.subscribe(this, Event.SHOW_CONTROLLER_GUI_ACTION, Event.TIME_STATE_CMD, Event.SCENE_LOADED);
    }

    @Override
    public void doneLoading(AssetManager assetManager) {
        rebuildGui();
        ui.setKeyboardFocus(null);
    }

    @Override
    public void update(double dt) {
        super.update(dt);
        this.guiControllerListener.update();
    }

    private ScrollPane container(Table t, float w, float h) {
        OwnScrollPane c = new OwnScrollPane(t, skin, "minimalist-nobg");
        t.top();
        c.setFadeScrollBars(true);
        c.setForceScroll(false, false);
        c.setSize(w, h);
        return c;
    }

    private void updatePads(Table t) {
        Array<Cell> cells = t.getCells();
        for (Cell<?> c : cells) {
            if (c.getActor() instanceof Button) {
                ((Button) c.getActor()).pad(pad20);
            }
        }
    }

    public void updateTabs() {
        for (OwnTextButton tb : tabButtons) {
            tb.setProgrammaticChangeEvents(false);
            tb.setChecked(false);
            tb.setProgrammaticChangeEvents(true);
        }
        tabButtons.get(selectedTab).setProgrammaticChangeEvents(false);
        tabButtons.get(selectedTab).setChecked(true);
        tabButtons.get(selectedTab).setProgrammaticChangeEvents(true);
        contentCell.setActor(null);
        currentModel = model.get(selectedTab);
        contentCell.setActor(tabContents.get(selectedTab));
        selectFirst();
        updateFocused();
    }

    /**
     * Selects the given object. If it is null, it scans the row in the given direction until
     * all elements have been scanned.
     *
     * @param i     The column
     * @param j     The row
     * @param right Whether scan right or left
     *
     * @return True if the element was selected, false otherwise
     */
    public boolean selectInRow(int i, int j, boolean right) {
        fi = i;
        fj = j;
        if (currentModel != null && currentModel.length > 0) {
            while (currentModel[fi][fj] == null) {
                // Move to next column
                fi = (fi + (right ? 1 : -1)) % currentModel.length;
                if (fi < 0) {
                    fi = currentModel.length - 1;
                }
                if (fi == i) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Selects the given object. If it is null, it scans the column in the given direction until
     * all elements have been scanned.
     *
     * @param i    The column
     * @param j    The row
     * @param down Whether scan up or down
     *
     * @return True if the element was selected, false otherwise
     */
    public boolean selectInCol(int i, int j, boolean down) {
        fi = i;
        fj = j;
        if (currentModel != null && currentModel.length > 0) {
            while (currentModel[fi][fj] == null) {
                // Try out other columns
                if (selectInRow(fi, fj, true))
                    return true;
                // Move to the next row
                fj = (fj + (down ? 1 : -1)) % currentModel[fi].length;
                if (fj == j) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void selectFirst() {
        selectInRow(0, 0, true);
    }

    public void updateFocused() {
        updateFocused(false);
    }

    public void updateFocused(boolean force) {
        if ((force || content.getParent() != null) && currentModel != null && currentModel.length != 0) {
            Actor actor = currentModel[fi][fj];
            if (actor instanceof Button || actor instanceof Slider) {
                ui.setKeyboardFocus(actor);
            }
        }
    }

    public void tabLeft() {
        if (selectedTab - 1 < 0) {
            selectedTab = tabButtons.size() - 1;
        } else {
            selectedTab--;
        }
        updateTabs();
    }

    public void tabRight() {
        selectedTab = (selectedTab + 1) % tabButtons.size();
        updateTabs();
    }

    public void up() {
        selectInCol(fi, update(fj, -1, currentModel[fi].length), false);
        updateFocused();
    }

    public void down() {
        selectInCol(fi, update(fj, 1, currentModel[fi].length), true);
        updateFocused();
    }

    public void left() {
        selectInRow(update(fi, -1, currentModel.length), fj, false);
        updateFocused();
    }

    public void right() {
        selectInRow(update(fi, 1, currentModel.length), fj, true);
        updateFocused();
    }

    public void sliderUp(float percent) {
        sliderMove(true, percent);
    }

    public void sliderDown(float percent) {
        sliderMove(false, percent);
    }

    public void sliderMove(boolean up, float percent) {
        if (currentModel != null && currentModel[fi][fj] != null && currentModel[fi][fj] instanceof OwnSliderPlus) {
            OwnSliderPlus s = (OwnSliderPlus) currentModel[fi][fj];
            float max = s.getMaxValue();
            float min = s.getMinValue();
            float val = s.getValue();
            float inc = (max - min) * percent;
            s.setValue(MathUtils.clamp(val + (up ? inc : -inc), min, max));
        }
    }

    private int update(int val, int inc, int len) {
        if (len <= 0)
            return val;
        if (inc >= 0) {
            return (val + inc) % len;
        } else {
            if (val + inc < 0) {
                return inc + len;
            } else {
                return val + inc;
            }
        }
    }

    public void touchDown() {
        if (currentModel != null) {
            Actor actor = currentModel[fi][fj];
            if (actor instanceof Button) {
                final Button b = (Button) actor;

                InputEvent inputEvent = Pools.obtain(InputEvent.class);
                inputEvent.setType(InputEvent.Type.touchDown);
                b.fire(inputEvent);
                Pools.free(inputEvent);
            }
        }

    }

    public void touchUp() {
        if (currentModel != null) {
            Actor actor = currentModel[fi][fj];
            if (actor instanceof Button) {
                final Button b = (Button) actor;

                InputEvent inputEvent = Pools.obtain(InputEvent.class);
                inputEvent.setType(InputEvent.Type.touchUp);
                b.fire(inputEvent);
                Pools.free(inputEvent);
            }
        }
    }

    public void back() {
        EventManager.publish(Event.SHOW_CONTROLLER_GUI_ACTION, this, GaiaSky.instance.cameraManager.naturalCamera);
        updateFocused();
        ui.setKeyboardFocus(null);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        // Empty by default
        switch (event) {
        case SHOW_CONTROLLER_GUI_ACTION -> {
            NaturalCamera cam = (NaturalCamera) data[0];
            if (content.isVisible() && content.getParent() != null) {
                // Hide and remove
                searchField.setText("");
                content.setVisible(false);
                content.remove();
                ui.setKeyboardFocus(null);

                // Remove GUI listener, add natural listener
                cam.addGamepadListener();
                removeControllerListener();
            } else {
                // Show
                // Add and show
                ui.addActor(content);
                content.setVisible(true);
                updateFocused();

                // Remove natural listener, add GUI listener
                cam.removeGamepadListener();
                addControllerListener(cam);

                // Close all open windows.
                EventManager.publish(Event.CLOSE_ALL_GUI_WINDOWS_CMD, this);
            }
        }
        case TIME_STATE_CMD -> {
            boolean on = (Boolean) data[0];
            timeStartStop.setProgrammaticChangeEvents(false);
            timeStartStop.setChecked(on);
            timeStartStop.setText(on ? "Stop time" : "Start time");
            timeStartStop.setProgrammaticChangeEvents(true);
        }
        case SCENE_LOADED -> this.scene = (Scene) data[0];
        default -> {
        }
        }
    }

    public boolean removeControllerGui(NaturalCamera cam) {
        if (content.isVisible() && content.getParent() != null) {
            // Hide and remove
            content.setVisible(false);
            content.remove();

            // Remove GUI listener, add natural listener
            cam.addGamepadListener();
            removeControllerListener();
            return true;
        }
        return false;
    }

    private void addControllerListener(NaturalCamera cam) {
        guiControllerListener.setCamera(cam);
        guiControllerListener.setMappings(AbstractGamepadMappings.readGamepadMappings(Settings.settings.controls.gamepad.mappingsFile));
        Settings.settings.controls.gamepad.addControllerListener(guiControllerListener);
        guiControllerListener.activate();
    }

    private void removeControllerListener() {
        Settings.settings.controls.gamepad.removeControllerListener(guiControllerListener);
        guiControllerListener.deactivate();
    }

    private class GUIControllerListener extends AbstractGamepadListener {
        private float rStickX = 0;
        private final EventManager em;
        private NaturalCamera cam;

        public GUIControllerListener(String mappingsFile) {
            super(mappingsFile);
            this.em = EventManager.instance;
        }

        public void setCamera(NaturalCamera cam) {
            this.cam = cam;
        }

        @Override
        public void connected(Controller controller) {

        }

        @Override
        public void disconnected(Controller controller) {

        }

        @Override
        public void pollAxis() {

        }

        @Override
        public void pollButtons() {

        }

        @Override
        public boolean buttonDown(Controller controller, int buttonCode) {
            if (buttonCode == mappings.getButtonA()) {
                touchDown();
            }
            return true;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            if (buttonCode == mappings.getButtonStart()) {
                em.post(Event.SHOW_CONTROLLER_GUI_ACTION, this, cam);
            } else if (buttonCode == mappings.getButtonB()) {
                back();
            } else if (buttonCode == mappings.getButtonA()) {
                touchUp();
            } else if (buttonCode == mappings.getButtonDpadUp()) {
                up();
            } else if (buttonCode == mappings.getButtonDpadDown()) {
                down();
            } else if (buttonCode == mappings.getButtonDpadLeft()) {
                left();
            } else if (buttonCode == mappings.getButtonDpadRight()) {
                right();
            } else if (buttonCode == mappings.getButtonRB()) {
                tabRight();
            } else if (buttonCode == mappings.getButtonLB()) {
                tabLeft();
            }

            return true;
        }

        @Override
        public boolean axisMoved(Controller controller, int axisCode, float value) {
            value = (float) applyZeroPoint(value);
            if (System.currentTimeMillis() - lastAxisEvtTime > AXIS_EVT_DELAY) {
                // Event-based
                if (axisCode == mappings.getAxisLstickH()) {
                    // LEFT STICK horizontal - move horizontally
                    if (value > 0) {
                        right();
                    } else {
                        left();
                    }
                } else if (axisCode == mappings.getAxisLstickV()) {
                    // LEFT STICK vertical - move vertically
                    if (value > 0) {
                        down();
                    } else {
                        up();
                    }
                }
                lastAxisEvtTime = System.currentTimeMillis();
            }
            // Poll
            if (axisCode == mappings.getAxisRstickH()) {
                // RIGHT STICK horizontal - slider up/down
                rStickX = value;
            }

            return true;
        }

        @Override
        public void update() {
            // Right stick moves slider
            long now = TimeUtils.millis();
            boolean update = now - lastAxisEvtTime > AXIS_POLL_DELAY;
            if (rStickX != 0 && update) {
                if (rStickX > 0) {
                    sliderUp(0.05f);
                } else if(rStickX < 0) {
                    sliderDown(0.05f);
                }
                lastAxisEvtTime = now;
            }
        }

        @Override
        public void activate() {

        }

        @Override
        public void deactivate() {

        }

    }

}