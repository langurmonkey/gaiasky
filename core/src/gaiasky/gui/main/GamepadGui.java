/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.beans.CameraComboBoxBean;
import gaiasky.gui.bookmarks.BookmarksManager;
import gaiasky.gui.bookmarks.BookmarksManager.BookmarkNode;
import gaiasky.gui.iface.CameraInfoInterface;
import gaiasky.gui.iface.TopInfoInterface;
import gaiasky.gui.vr.MainVRGui;
import gaiasky.gui.window.GamepadConfigWindow;
import gaiasky.input.GuiGamepadListener;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.view.FilterView;
import gaiasky.scene.view.FocusView;
import gaiasky.util.*;
import gaiasky.util.Settings.ControlsSettings.GamepadSettings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;
import net.jafama.FastMath;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.List;

/**
 * Provides a way to navigate and edit the most important settings in Gaia Sky using a gamepad or a VR controller.
 */
public class GamepadGui extends AbstractGui {
    private static final Logger.Log logger = Logger.getLogger(GamepadGui.class.getSimpleName());

    private boolean initialized = false;
    private final Table content, menu;
    // Contains a matrix (column major) of actors for each tab
    private final List<Actor[][]> model;
    private final List<OwnTextButton> tabButtons;
    private final List<ScrollPane> tabContents;
    private final EventManager em;
    private final float pad5;
    private final float pad10;
    private final float pad20;
    private final float pad30;
    private final float pad40;
    private float th;
    private final float bw;
    private final float bh;
    private final FocusView view;
    private final FilterView filterView;
    boolean hackProgrammaticChangeEvents = true;
    private Table searchT;
    private Cell<?> contentCell, infoCell;
    private OwnTextButton vrInfoButton, searchButton, bookmarksButton, cameraButton, timeButton, graphicsButton, typesButton, controlsButton, systemButton;
    private OwnTextIconButton button3d, buttonDome, buttonCubemap, buttonOrthoSphere, buttonGoHome;

    private TopInfoInterface topLine;
    private CameraInfoInterface focusInterface;
    private OwnCheckBox cinematic, crosshairFocus, crosshairClosest, crosshairHome, debugInfo;
    private OwnSelectBox<CameraComboBoxBean> cameraMode;
    private OwnTextButton timeStartStop;
    private OwnTextButton timeUp;
    private OwnTextButton timeDown;
    private OwnTextButton timeReset;
    private OwnTextButton starGlowButton;
    private OwnTextButton invertYButton;
    private OwnTextButton invertXButton;
    private OwnSliderPlus fovSlider, camSpeedSlider, camRotSlider, camTurnSlider, bloomSlider, unsharpMaskSlider, starBrightness,
            magnitudeMultiplier, starGlowFactor, pointSize, starBaseLevel, lensFlare, motionBlur;
    private OwnTextField searchField;
    private OwnLabel infoMessage, cameraModeLabel, cameraFocusLabel;
    private Actor[][] currentModel;
    private Scene scene;
    private GamepadGuiListener gamepadListener;
    private Set<ControllerListener> backupGamepadListeners;
    private String currentInputText = "";
    private final Map<String, Button> visibilityButtonMap;
    private Cell<Container>[] bookmarkColumns;
    private final int maxBookmarkDepth = 4;

    private static int selectedTab = 0;
    private int fi = 0, fj = 0;

    public GamepadGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel, final boolean vrMode) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        this.em = EventManager.instance;
        this.vr = vrMode;
        model = new ArrayList<>();
        content = new Table(skin);
        content.setVisible(false);
        menu = new Table(skin);
        tabButtons = new ArrayList<>();
        tabContents = new ArrayList<>();
        visibilityButtonMap = new HashMap<>();
        pad5 = 8f;
        pad10 = 16f;
        pad20 = 32f;
        pad30 = 48;
        pad40 = 98;

        // Bookmarks size.
        bw = 300;
        bh = 60;

        view = new FocusView();
        filterView = new FilterView();
    }

    public GamepadGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel) {
        this(skin, graphics, unitsPerPixel, false);
    }

    @Override
    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        stage = new Stage(vp, sb);

        gamepadListener = new GamepadGuiListener(this, Settings.settings.controls.gamepad.mappingsFile);

        // Comment to hide this whole dialog and functionality
        registerEvents();
        initialized = true;
    }

    public void initialize(Stage stage) {
        this.stage = stage;
        registerEvents();
    }

    private void registerEvents() {
        EventManager.instance.subscribe(this,
                                        Event.SHOW_CONTROLLER_GUI_ACTION,
                                        Event.TIME_STATE_CMD,
                                        Event.SCENE_LOADED,
                                        Event.CAMERA_MODE_CMD,
                                        Event.FOCUS_CHANGE_CMD);
        EventManager.instance.subscribe(this,
                                        Event.STAR_POINT_SIZE_CMD,
                                        Event.STAR_BRIGHTNESS_CMD,
                                        Event.STAR_BRIGHTNESS_POW_CMD,
                                        Event.STAR_GLOW_FACTOR_CMD,
                                        Event.STAR_BASE_LEVEL_CMD,
                                        Event.LABEL_SIZE_CMD,
                                        Event.LINE_WIDTH_CMD);
        EventManager.instance.subscribe(this, Event.CUBEMAP_CMD, Event.STEREOSCOPIC_CMD, Event.TOGGLE_VISIBILITY_CMD);
        EventManager.instance.subscribe(this, Event.TIME_CHANGE_INFO, Event.TIME_CHANGE_CMD, Event.INVERT_X_CMD, Event.INVERT_Y_CMD);
        EventManager.instance.subscribe(this, Event.TIME_WARP_CHANGED_INFO, Event.TIME_WARP_CMD);
        EventManager.instance.subscribe(this, Event.CROSSHAIR_CLOSEST_CMD, Event.CROSSHAIR_FOCUS_CMD, Event.CROSSHAIR_HOME_CMD);
    }

    public void build() {
        rebuildGui();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void rebuildGui() {

        // Clean up
        content.clear();
        menu.clear();
        tabButtons.clear();
        tabContents.clear();
        model.clear();

        float tw1 = vr ? MainVRGui.WIDTH : FastMath.min(Gdx.graphics.getWidth(), 1450f) - 60f;
        th = vr ? MainVRGui.HEIGHT : FastMath.min(Gdx.graphics.getHeight(), 860f) - 60f;
        // Widget width
        float ww = 400f;
        float wh = 64f;
        float sh = 96f;
        float tfw = 240f;
        // Tab width
        float tw = 224f;

        // Create contents

        if (vr) {
            // TOP LINE (time)
            topLine = new TopInfoInterface(skin, GaiaSky.instance.scene);

            // BOTTOM LINE (go home)
            buttonGoHome = new OwnTextIconButton("", skin, "home");
            buttonGoHome.addListener(new OwnTextTooltip(I18n.msg("context.goto", Settings.settings.scene.homeObject), skin, 10));
            buttonGoHome.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.GO_HOME_INSTANT_CMD, buttonGoHome);
                    return true;
                }
                return false;
            });

            // VR INFO
            model.add(null);

            Table infoT = new Table(skin);
            infoT.setSize(tw1, th);

            var vrInfoT = new Table(skin);
            vrInfoT.setSize(tw1, th);

            // Title
            OwnLabel welcomeTitle = new OwnLabel(Settings.getApplicationTitle(Settings.settings.runtime.openXr), skin, "header-large");
            OwnLabel version = new OwnLabel(Settings.settings.version.version, skin, "header-raw");
            vrInfoT.add(welcomeTitle).center().top().padBottom(pad20).colspan(2).row();
            vrInfoT.add(version).center().top().padBottom(pad40).colspan(2).row();

            var driver = GaiaSky.instance.xrDriver;

            if (driver != null) {
                // Devices
                addDeviceTypeInfo(vrInfoT, I18n.msg("gui.vr.system"), driver.systemString);
                addDeviceTypeInfo(vrInfoT, I18n.msg("gui.vr.runtime.name"), driver.runtimeName);
                addDeviceTypeInfo(vrInfoT, I18n.msg("gui.vr.runtime.version"), driver.runtimeVersionString);
                var left = driver.getControllerDevices().get(0);
                var right = driver.getControllerDevices().get(1);
                addDeviceTypeInfo(vrInfoT, I18n.msg("gui.vr.controller.left"), I18n.msg("gui.vr.controller.active", left.isActive()));
                addDeviceTypeInfo(vrInfoT, I18n.msg("gui.vr.controller.right"), I18n.msg("gui.vr.controller.active", right.isActive()));
            }

            infoT.add(vrInfoT).left().center().padRight(pad30 * 2f);

            // Focus info interface
            focusInterface = new CameraInfoInterface(skin, vr);
            infoT.add(focusInterface).left().center();

            tabContents.add(container(infoT, tw1, th));
            updatePads(infoT);
        }

        // SEARCH
        Actor[][] searchModel = new Actor[11][5];
        model.add(searchModel);

        searchT = new Table(skin);
        searchT.setSize(tw1, th);

        searchField = new OwnTextField("", skin, "big");
        searchField.setProgrammaticChangeEvents(true);
        searchField.setSize(ww, wh);
        searchField.setMessageText("Search...");
        searchField.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (!searchField.getText().equals(currentInputText) && !searchField.getText().isBlank()) {
                    // Process only if text changed
                    currentInputText = searchField.getText();
                    String name = currentInputText.toLowerCase(Locale.ROOT).trim();
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
        infoCell.colspan(11).padBottom(pad20).row();

        // Numbers row
        addTextKey("1", searchModel, 0, 0, false);
        addTextKey("2", searchModel, 1, 0, false);
        addTextKey("3", searchModel, 2, 0, false);
        addTextKey("4", searchModel, 3, 0, false);
        addTextKey("5", searchModel, 4, 0, false);
        addTextKey("6", searchModel, 5, 0, false);
        addTextKey("7", searchModel, 6, 0, false);
        addTextKey("8", searchModel, 7, 0, false);
        addTextKey("9", searchModel, 8, 0, false);
        addTextKey("0", searchModel, 9, 0, false);
        addTextKey("<--", (event) -> {
            if (event instanceof ChangeEvent) {
                if (!searchField.getText().isBlank()) {
                    searchField.setText(searchField.getText().substring(0, searchField.getText().length() - 1));
                }
            }
            return false;
        }, searchModel, 10, 0, true, tfw / 1.5f, 0);

        // First row
        addTextKey("Q", searchModel, 0, 1, false);
        addTextKey("W", searchModel, 1, 1, false);
        addTextKey("E", searchModel, 2, 1, false);
        addTextKey("R", searchModel, 3, 1, false);
        addTextKey("T", searchModel, 4, 1, false);
        addTextKey("Y", searchModel, 5, 1, false);
        addTextKey("U", searchModel, 6, 1, false);
        addTextKey("I", searchModel, 7, 1, false);
        addTextKey("O", searchModel, 8, 1, false);
        addTextKey("P", searchModel, 9, 1, true);

        // Second row
        addTextKey("A", searchModel, 0, 2, false);
        addTextKey("S", searchModel, 1, 2, false);
        addTextKey("D", searchModel, 2, 2, false);
        addTextKey("F", searchModel, 3, 2, false);
        addTextKey("G", searchModel, 4, 2, false);
        addTextKey("H", searchModel, 5, 2, false);
        addTextKey("J", searchModel, 6, 2, false);
        addTextKey("K", searchModel, 7, 2, false);
        addTextKey("L", searchModel, 8, 2, false);
        addTextKey("'", searchModel, 9, 2, true);

        // Third row
        addTextKey("Z", searchModel, 0, 3, false);
        addTextKey("X", searchModel, 1, 3, false);
        addTextKey("C", searchModel, 2, 3, false);
        addTextKey("V", searchModel, 3, 3, false);
        addTextKey("B", searchModel, 4, 3, false);
        addTextKey("N", searchModel, 5, 3, false);
        addTextKey("M", searchModel, 6, 3, false);
        addTextKey(".", searchModel, 7, 3, false);
        addTextKey(",", searchModel, 8, 3, false);
        addTextKey("-", searchModel, 9, 3, true);

        // Fourth row
        searchT.add().padRight(pad5).padBottom(pad10);
        searchT.add().padRight(pad5).padBottom(pad10);
        addTextKey("SPACE", (event) -> {
            if (event instanceof ChangeEvent) {
                searchField.setText(searchField.getText() + " ");
            }
            return false;
        }, searchModel, 7, 4, false, tfw * 2f, 6);
        addTextKey("Clear", (event) -> {
            if (event instanceof ChangeEvent) {
                if (!searchField.getText().isBlank()) {
                    searchField.setText("");
                }
            }
            return false;
        }, searchModel, 8, 4, false, tfw / 1.5f, 3);

        tabContents.add(container(searchT, tw1, th));
        updatePads(searchT);

        // BOOKMARKS
        BookmarksManager bm = GaiaSky.instance.getBookmarksManager();
        var bookmarks = bm.getBookmarks();

        Actor[][] bookmarksModel = new Actor[maxBookmarkDepth][bookmarks.size()];
        model.add(bookmarksModel);

        Table bookmarksT = new Table(skin);
        bookmarksT.setSize(tw1, th);
        bookmarksT.align(Align.topLeft);
        bookmarksT.pad(pad10);
        bookmarksT.top().left();

        bookmarkColumns = new Cell[maxBookmarkDepth];
        for (int l = 0; l < maxBookmarkDepth; l++) bookmarkColumns[l] = bookmarksT.add().left().width(bw);

        fillBookmarksColumn(bookmarkColumns, 0, bookmarks, bookmarksModel, bw, bh);

        tabContents.add(container(bookmarksT, tw1, th));
        updatePads(bookmarksT);

        // CAMERA
        var camera = GaiaSky.instance.getICamera();
        Actor[][] cameraModel = new Actor[4][10];
        model.add(cameraModel);

        Table camT = new Table(skin);
        camT.setSize(tw1, th);
        CameraManager cam = GaiaSky.instance.getCameraManager();

        final Label modeLabel = new Label(I18n.msg("gui.camera.mode"), skin, "header-raw");
        if (!vr) {
            // Camera mode
            final int cameraModes = CameraMode.values().length;
            final CameraComboBoxBean[] cameraOptions = new CameraComboBoxBean[cameraModes];
            for (int i = 0; i < cameraModes; i++) {
                cameraOptions[i] = new CameraComboBoxBean(Objects.requireNonNull(CameraMode.getMode(i)).toStringI18n(), CameraMode.getMode(i));
            }
            cameraMode = new OwnSelectBox<>(skin, "big");
            cameraModel[0][0] = cameraMode;
            cameraMode.setWidth(ww);
            cameraMode.setItems(cameraOptions);
            cameraMode.setSelectedIndex(cam.getMode().ordinal());
            cameraMode.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    CameraComboBoxBean selection = cameraMode.getSelected();
                    CameraMode mode = selection.mode;

                    EventManager.publish(Event.CAMERA_MODE_CMD, cameraMode, mode);
                    return true;
                }
                return false;
            });
            camT.add(modeLabel).right().padBottom(pad20).padRight(pad20);
            camT.add(cameraMode).left().padBottom(pad20).row();

            // Cinematic
            final Label cinematicLabel = new Label(I18n.msg("gui.camera.cinematic"), skin, "header-raw");
            cinematic = new OwnCheckBox("", skin, 0f);
            cameraModel[0][1] = cinematic;
            cinematic.setChecked(Settings.settings.scene.camera.cinematic);
            cinematic.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.CAMERA_CINEMATIC_CMD, cinematic, cinematic.isChecked());
                    return true;
                }
                return false;
            });
            camT.add(cinematicLabel).right().padBottom(pad20).padRight(pad20);
            camT.add(cinematic).left().padBottom(pad20).row();

            // FOV
            final Label fovLabel = new Label(I18n.msg("gui.camera.fov"), skin, "header-raw");
            fovSlider = new OwnSliderPlus("", Constants.MIN_FOV, Constants.MAX_FOV, Constants.SLIDER_STEP_SMALL, false, skin, "header-raw");
            cameraModel[0][2] = fovSlider;
            fovSlider.setValueSuffix("°");
            fovSlider.setName("field of view");
            fovSlider.setWidth(ww);
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
            camT.add(fovLabel).right().padBottom(pad20).padRight(pad20);
            camT.add(fovSlider).left().padBottom(pad20).row();
        } else {
            // In VR, we show the mode and the current focus, if any.
            var modeString = camera.getMode().toStringI18n();
            cameraModeLabel = new OwnLabel(modeString, skin, "header");
            camT.add(modeLabel).right().padBottom(pad20).padRight(pad20);
            camT.add(cameraModeLabel).left().padBottom(pad20).row();

            var focusString = camera.getMode().isFocus() ? camera.getFocus().getLocalizedName() : "-";
            final Label focusLabel = new Label(I18n.msg("camera.FOCUS_MODE"), skin, "header-raw");
            cameraFocusLabel = new OwnLabel(focusString, skin, "header");
            camT.add(focusLabel).right().padBottom(pad20).padRight(pad20);
            camT.add(cameraFocusLabel).left().padBottom(pad20).row();
        }

        // Speed
        final Label speedLabel = new Label(I18n.msg("gui.camera.speed"), skin, "header-raw");
        camSpeedSlider = new OwnSliderPlus("",
                                           Constants.MIN_SLIDER,
                                           Constants.MAX_SLIDER,
                                           Constants.SLIDER_STEP,
                                           Constants.MIN_CAM_SPEED,
                                           Constants.MAX_CAM_SPEED,
                                           skin,
                                           "header-raw");
        cameraModel[0][3] = camSpeedSlider;
        camSpeedSlider.setName("camera speed");
        camSpeedSlider.setWidth(ww);
        camSpeedSlider.setMappedValue(Settings.settings.scene.camera.speed);
        camSpeedSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.CAMERA_SPEED_CMD, camSpeedSlider, camSpeedSlider.getMappedValue(), false);
                return true;
            }
            return false;
        });
        camT.add(speedLabel).right().padBottom(pad20).padRight(pad20);
        camT.add(camSpeedSlider).left().padBottom(pad20).row();

        // Rot
        final Label rotationLabel = new Label(I18n.msg("gui.rotation.speed"), skin, "header-raw");
        camRotSlider = new OwnSliderPlus("",
                                         Constants.MIN_SLIDER,
                                         Constants.MAX_SLIDER,
                                         Constants.SLIDER_STEP,
                                         Constants.MIN_ROT_SPEED,
                                         Constants.MAX_ROT_SPEED,
                                         skin,
                                         "header-raw");
        cameraModel[0][4] = camRotSlider;
        camRotSlider.setName("rotate speed");
        camRotSlider.setWidth(ww);
        camRotSlider.setMappedValue(Settings.settings.scene.camera.rotate);
        camRotSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.ROTATION_SPEED_CMD, camRotSlider, camRotSlider.getMappedValue());
                return true;
            }
            return false;
        });
        camT.add(rotationLabel).right().padBottom(pad20).padRight(pad20);
        camT.add(camRotSlider).left().padBottom(pad20).row();

        // Turn
        final Label turnLabel = new Label(I18n.msg("gui.turn.speed"), skin, "header-raw");
        camTurnSlider = new OwnSliderPlus("",
                                          Constants.MIN_SLIDER,
                                          Constants.MAX_SLIDER,
                                          Constants.SLIDER_STEP,
                                          Constants.MIN_TURN_SPEED,
                                          Constants.MAX_TURN_SPEED,
                                          skin,
                                          "header-raw");
        cameraModel[0][5] = camTurnSlider;
        camTurnSlider.setName("turn speed");
        camTurnSlider.setWidth(ww);
        camTurnSlider.setMappedValue(Settings.settings.scene.camera.turn);
        camTurnSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.TURNING_SPEED_CMD, camTurnSlider, camTurnSlider.getMappedValue(), false);
                return true;
            }
            return false;
        });
        camT.add(turnLabel).right().padBottom(pad20).padRight(pad20);
        camT.add(camTurnSlider).left().padBottom(pad20).row();

        // Focus marker
        OwnLabel crosshairFocusLabel = new OwnLabel(I18n.msg("gui.ui.crosshair.focus"), skin);
        crosshairFocus = new OwnCheckBox("", skin);
        cameraModel[0][6] = crosshairFocus;
        crosshairFocus.setName("ch focus");
        crosshairFocus.setChecked(Settings.settings.scene.crosshair.focus);
        crosshairFocus.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.CROSSHAIR_FOCUS_CMD, this, crosshairFocus.isChecked());
            }
            return false;
        });
        camT.add(crosshairFocusLabel).right().padBottom(pad20).padRight(pad20);
        camT.add(crosshairFocus).left().padBottom(pad20).row();

        // Closest marker
        OwnLabel crosshairClosestLabel = new OwnLabel(I18n.msg("gui.ui.crosshair.closest"), skin);
        crosshairClosest = new OwnCheckBox("", skin);
        cameraModel[0][7] = crosshairClosest;
        crosshairClosest.setName("ch closest");
        crosshairClosest.setChecked(Settings.settings.scene.crosshair.closest);
        crosshairClosest.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.CROSSHAIR_CLOSEST_CMD, this, crosshairClosest.isChecked());
            }
            return false;
        });
        camT.add(crosshairClosestLabel).right().padBottom(pad20).padRight(pad20);
        camT.add(crosshairClosest).left().padBottom(pad20).row();

        // Home marker
        OwnLabel crosshairHomeLabel = new OwnLabel(I18n.msg("gui.ui.crosshair.home"), skin);
        crosshairHome = new OwnCheckBox("", skin);
        cameraModel[0][8] = crosshairHome;
        crosshairHome.setName("ch home");
        crosshairHome.setChecked(Settings.settings.scene.crosshair.home);
        crosshairHome.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.CROSSHAIR_HOME_CMD, this, crosshairHome.isChecked());
            }
            return false;
        });
        camT.add(crosshairHomeLabel).right().padBottom(pad20).padRight(pad20);
        camT.add(crosshairHome).left().padBottom(pad20).row();

        if (!vr) {
            // Mode buttons
            Table modeButtons = new Table(skin);

            final Image icon3d = new Image(skin.getDrawable("3d-icon"));
            button3d = new OwnTextIconButton("", icon3d, skin, "toggle");
            cameraModel[0][9] = button3d;
            button3d.setChecked(Settings.settings.program.modeStereo.active);
            final String[] hk3d = KeyBindings.instance.getStringKeys("action.toggle/element.stereomode", true);
            button3d.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("element.stereomode")), hk3d, skin));
            button3d.setName("3d");
            button3d.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (button3d.isChecked()) {
                        buttonCubemap.setProgrammaticChangeEvents(true);
                        buttonCubemap.setChecked(false);
                        buttonDome.setProgrammaticChangeEvents(true);
                        buttonDome.setChecked(false);
                        buttonOrthoSphere.setProgrammaticChangeEvents(true);
                        buttonOrthoSphere.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.publish(Event.STEREOSCOPIC_CMD, button3d, button3d.isChecked());
                    return true;
                }
                return false;
            });
            modeButtons.add(button3d).padRight(pad20);

            final Image iconDome = new Image(skin.getDrawable("dome-icon"));
            buttonDome = new OwnTextIconButton("", iconDome, skin, "toggle");
            cameraModel[1][9] = buttonDome;
            buttonDome.setChecked(Settings.settings.program.modeCubemap.active && Settings.settings.program.modeCubemap.isPlanetariumOn());
            final String[] hkDome = KeyBindings.instance.getStringKeys("action.toggle/element.planetarium", true);
            buttonDome.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("element.planetarium")), hkDome, skin));
            buttonDome.setName("dome");
            buttonDome.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (buttonDome.isChecked()) {
                        buttonCubemap.setProgrammaticChangeEvents(true);
                        buttonCubemap.setChecked(false);
                        button3d.setProgrammaticChangeEvents(true);
                        button3d.setChecked(false);
                        buttonOrthoSphere.setProgrammaticChangeEvents(true);
                        buttonOrthoSphere.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.publish(Event.CUBEMAP_CMD, buttonDome, buttonDome.isChecked(), CubemapProjection.AZIMUTHAL_EQUIDISTANT);
                    fovSlider.setDisabled(buttonDome.isChecked());
                    return true;
                }
                return false;
            });
            modeButtons.add(buttonDome).padRight(pad20);

            final Image iconCubemap = new Image(skin.getDrawable("cubemap-icon"));
            buttonCubemap = new OwnTextIconButton("", iconCubemap, skin, "toggle");
            cameraModel[2][9] = buttonCubemap;
            buttonCubemap.setProgrammaticChangeEvents(false);
            buttonCubemap.setChecked(Settings.settings.program.modeCubemap.active && Settings.settings.program.modeCubemap.isPanoramaOn());
            final String[] hkCubemap = KeyBindings.instance.getStringKeys("action.toggle/element.360", true);
            buttonCubemap.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("element.360")), hkCubemap, skin));
            buttonCubemap.setName("cubemap");
            buttonCubemap.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (buttonCubemap.isChecked()) {
                        buttonDome.setProgrammaticChangeEvents(true);
                        buttonDome.setChecked(false);
                        button3d.setProgrammaticChangeEvents(true);
                        button3d.setChecked(false);
                        buttonOrthoSphere.setProgrammaticChangeEvents(true);
                        buttonOrthoSphere.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.publish(Event.CUBEMAP_CMD, buttonCubemap, buttonCubemap.isChecked(), CubemapProjection.EQUIRECTANGULAR);
                    fovSlider.setDisabled(buttonCubemap.isChecked());
                    return true;
                }
                return false;
            });
            modeButtons.add(buttonCubemap).padRight(pad20);

            final Image iconOrthosphere = new Image(skin.getDrawable("orthosphere-icon"));
            buttonOrthoSphere = new OwnTextIconButton("", iconOrthosphere, skin, "toggle");
            cameraModel[3][9] = buttonOrthoSphere;
            buttonOrthoSphere.setProgrammaticChangeEvents(false);
            buttonOrthoSphere.setChecked(Settings.settings.program.modeCubemap.active && Settings.settings.program.modeCubemap.isOrthosphereOn());
            final String[] hkOrthosphere = KeyBindings.instance.getStringKeys("action.toggle/element.orthosphere", true);
            buttonOrthoSphere.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("element.orthosphere")), hkOrthosphere, skin));
            buttonOrthoSphere.setName("orthosphere");
            buttonOrthoSphere.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (buttonOrthoSphere.isChecked()) {
                        buttonCubemap.setProgrammaticChangeEvents(true);
                        buttonCubemap.setChecked(false);
                        buttonDome.setProgrammaticChangeEvents(true);
                        buttonDome.setChecked(false);
                        button3d.setProgrammaticChangeEvents(true);
                        button3d.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.publish(Event.CUBEMAP_CMD, buttonOrthoSphere, buttonOrthoSphere.isChecked(), CubemapProjection.ORTHOSPHERE);
                    fovSlider.setDisabled(buttonOrthoSphere.isChecked());
                    return true;
                }
                return false;
            });
            modeButtons.add(buttonOrthoSphere).padRight(pad20);

            camT.add(modeButtons).colspan(2).center().padTop(pad40);
        }

        tabContents.add(container(camT, tw1, th));
        updatePads(camT);

        // TIME
        Actor[][] timeModel = new Actor[3][2];
        model.add(timeModel);

        Table timeT = new Table(skin);

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
        tabContents.add(container(timeT, tw1, th));
        updatePads(timeT);

        // TYPE VISIBILITY
        int visTableCols = 6;
        Actor[][] typesModel = new Actor[visTableCols][7];
        model.add(typesModel);

        Table typesT = new Table(skin);

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
                    continue;
                }
                // Name is the key
                button.setName(ct.key);
                typesModel[di][dj] = button;

                button.setChecked(visible[i]);
                button.addListener(new OwnTextTooltip(ct.getName(), skin));
                button.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, button, ct.key, button.isChecked());
                        return true;
                    }
                    return false;
                });
                addButtonTooltip(button, ct, name);

                // In VR, protect 'Others' component type by disabling it. Otherwise, VR controllers, which are of type 'Others',
                // may disappear.
                button.setDisabled(ct.key.equals("element.others") && GaiaSky.instance.isVR());

                visibilityButtonMap.put(ct.key, button);
                Cell<?> c = typesT.add(button).padBottom(buttonPadVert).left();

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

        typesT.setSize(tw1, th);
        tabContents.add(container(typesT, tw1, th));
        updatePads(typesT);

        // CONTROLS
        Actor[][] controlsModel = new Actor[1][3];
        model.add(controlsModel);

        Table controlsT = new Table(skin);

        if (vr) {
            // In VR, show usage info.
            Texture vrctrl_tex = new Texture(Gdx.files.internal("img/controller/hud-info-ui.png"));
            vrctrl_tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Image vrctrl = new Image(vrctrl_tex);

            controlsT.add(vrctrl).top().pad(pad20);
        } else {
            // In desktop, show regular controller configuration widgets.
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
                        GamepadConfigWindow ccw = new GamepadConfigWindow(controllerName, mappings, stage, skin);
                        ccw.setAcceptListener(() -> {
                            if (ccw.savedFile != null) {
                                // File was saved, reload, select
                                EventManager.publish(Event.RELOAD_CONTROLLER_MAPPINGS, this, ccw.savedFile.toAbsolutePath().toString());
                            }
                        });
                        ccw.show(stage);
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
                    EventManager.publish(Event.INVERT_X_CMD, this, invertXButton.isChecked());
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
                    EventManager.publish(Event.INVERT_Y_CMD, this, invertYButton.isChecked());
                    return true;
                }
                return false;
            });
            controlsT.add(invertYButton);
        }

        controlsT.setSize(tw1, th);
        tabContents.add(container(controlsT, tw1, th));
        updatePads(controlsT);

        // GRAPHICS
        Actor[][] graphicsModel = new Actor[2][7];
        model.add(graphicsModel);

        Table graphicsT = new Table(skin);

        // Star brightness
        starBrightness = new OwnSliderPlus(I18n.msg("gui.star.brightness"),
                                           Constants.MIN_SLIDER,
                                           Constants.MAX_SLIDER,
                                           Constants.SLIDER_STEP_TINY,
                                           Constants.MIN_STAR_BRIGHTNESS,
                                           Constants.MAX_STAR_BRIGHTNESS,
                                           skin,
                                           "header-raw");
        starBrightness.setWidth(ww);
        starBrightness.setHeight(sh);
        starBrightness.setMappedValue(Settings.settings.scene.star.brightness);
        starBrightness.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.publish(Event.STAR_BRIGHTNESS_CMD, starBrightness, starBrightness.getMappedValue());
                return true;
            }
            return false;
        });

        // Magnitude multiplier
        magnitudeMultiplier = new OwnSliderPlus(I18n.msg("gui.star.brightness.pow"),
                                                Constants.MIN_STAR_BRIGHTNESS_POW,
                                                Constants.MAX_STAR_BRIGHTNESS_POW,
                                                Constants.SLIDER_STEP_TINY,
                                                false,
                                                skin,
                                                "header-raw");
        magnitudeMultiplier.addListener(new OwnTextTooltip(I18n.msg("gui.star.brightness.pow.info"), skin));
        magnitudeMultiplier.setWidth(ww);
        magnitudeMultiplier.setHeight(sh);
        magnitudeMultiplier.setValue(Settings.settings.scene.star.power);
        magnitudeMultiplier.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.STAR_BRIGHTNESS_POW_CMD, magnitudeMultiplier, magnitudeMultiplier.getValue());
                return true;
            }
            return false;
        });

        // Star glow factor
        starGlowFactor = new OwnSliderPlus(I18n.msg("gui.star.glowfactor"),
                                           Constants.MIN_STAR_GLOW_FACTOR,
                                           Constants.MAX_STAR_GLOW_FACTOR,
                                           Constants.SLIDER_STEP_TINY * 0.1f,
                                           false,
                                           skin,
                                           "header-raw");
        starGlowFactor.addListener(new OwnTextTooltip(I18n.msg("gui.star.glowfactor.info"), skin));
        starGlowFactor.setWidth(ww);
        starGlowFactor.setHeight(sh);
        starGlowFactor.setMappedValue(Settings.settings.scene.star.glowFactor);
        starGlowFactor.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.publish(Event.STAR_GLOW_FACTOR_CMD, starGlowFactor, starGlowFactor.getValue());
                return true;
            }
            return false;
        });

        // Point size
        pointSize = new OwnSliderPlus(I18n.msg("gui.star.size"),
                                      Constants.MIN_STAR_POINT_SIZE,
                                      Constants.MAX_STAR_POINT_SIZE,
                                      Constants.SLIDER_STEP_TINY,
                                      false,
                                      skin,
                                      "header-raw");
        pointSize.setWidth(ww);
        pointSize.setHeight(sh);
        pointSize.addListener(new OwnTextTooltip(I18n.msg("gui.star.size.info"), skin));
        pointSize.setMappedValue(Settings.settings.scene.star.pointSize);
        pointSize.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.publish(Event.STAR_POINT_SIZE_CMD, pointSize, pointSize.getMappedValue());
                return true;
            }
            return false;
        });

        // Base star level
        starBaseLevel = new OwnSliderPlus(I18n.msg("gui.star.opacity"),
                                          Constants.MIN_STAR_MIN_OPACITY,
                                          Constants.MAX_STAR_MIN_OPACITY,
                                          Constants.SLIDER_STEP_TINY,
                                          false,
                                          skin,
                                          "header-raw");
        starBaseLevel.addListener(new OwnTextTooltip(I18n.msg("gui.star.opacity"), skin));
        starBaseLevel.setWidth(ww);
        starBaseLevel.setHeight(sh);
        starBaseLevel.setMappedValue(Settings.settings.scene.star.opacity[0]);
        starBaseLevel.addListener(event -> {
            if (event instanceof ChangeEvent && hackProgrammaticChangeEvents) {
                EventManager.publish(Event.STAR_BASE_LEVEL_CMD, starBaseLevel, starBaseLevel.getMappedValue());
                return true;
            }
            return false;
        });

        // Bloom
        bloomSlider = new OwnSliderPlus(I18n.msg("gui.bloom"),
                                        Constants.MIN_BLOOM,
                                        Constants.MAX_BLOOM,
                                        Constants.SLIDER_STEP_TINY,
                                        false,
                                        skin,
                                        "header-raw");
        bloomSlider.setWidth(ww);
        bloomSlider.setHeight(sh);
        bloomSlider.setValue(Settings.settings.postprocess.bloom.intensity);
        bloomSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.BLOOM_CMD, bloomSlider, bloomSlider.getValue());
                return true;
            }
            return false;
        });

        // Unsharp mask
        unsharpMaskSlider = new OwnSliderPlus(I18n.msg("gui.unsharpmask"),
                                              Constants.MIN_UNSHARP_MASK_FACTOR,
                                              Constants.MAX_UNSHARP_MASK_FACTOR,
                                              Constants.SLIDER_STEP_TINY,
                                              false,
                                              skin,
                                              "header-raw");
        unsharpMaskSlider.setWidth(ww);
        unsharpMaskSlider.setHeight(sh);
        unsharpMaskSlider.setValue(Settings.settings.postprocess.unsharpMask.factor);
        unsharpMaskSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.UNSHARP_MASK_CMD, unsharpMaskSlider, unsharpMaskSlider.getValue());
                return true;
            }
            return false;
        });

        if (!vr) {
            // Lens flare
            lensFlare = new OwnSliderPlus(I18n.msg("gui.lensflare"),
                                          Constants.MIN_LENS_FLARE_STRENGTH,
                                          Constants.MAX_LENS_FLARE_STRENGTH,
                                          Constants.SLIDER_STEP_TINY,
                                          false,
                                          skin,
                                          "header-raw");
            lensFlare.setWidth(ww);
            lensFlare.setHeight(sh);
            lensFlare.setValue(Settings.settings.postprocess.lensFlare.strength);
            lensFlare.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.LENS_FLARE_CMD, lensFlare, lensFlare.getValue());
                    return true;
                }
                return false;
            });
            // Star glow
            starGlowButton = new OwnTextButton(I18n.msg("gui.lightscattering"), skin, "toggle-big");
            starGlowButton.setWidth(ww);
            starGlowButton.setChecked(Settings.settings.postprocess.lightGlow.active);
            starGlowButton.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.LIGHT_GLOW_CMD, starGlowButton, starGlowButton.isChecked());
                    return true;
                }
                return false;
            });

            // Motion blur
            motionBlur = new OwnSliderPlus(I18n.msg("gui.motionblur"),
                                           Constants.MOTIONBLUR_MIN,
                                           Constants.MOTIONBLUR_MAX,
                                           Constants.SLIDER_STEP_TINY,
                                           false,
                                           skin,
                                           "header-raw");
            motionBlur.setWidth(ww);
            motionBlur.setHeight(sh);
            motionBlur.setMappedValue(Settings.settings.postprocess.motionBlur.strength);
            motionBlur.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.MOTION_BLUR_CMD, motionBlur, motionBlur.getMappedValue());
                    return true;
                }
                return false;
            });
        }

        /* Reset defaults */
        OwnTextIconButton resetDefaults = new OwnTextIconButton(I18n.msg("gui.resetdefaults"), skin, "reset");
        resetDefaults.align(Align.center);
        resetDefaults.setWidth(ww);
        resetDefaults.addListener(new OwnTextTooltip(I18n.msg("gui.resetdefaults.tooltip"), skin));
        resetDefaults.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Read defaults from internal settings file
                EventManager.publish(Event.RESET_VISUAL_SETTINGS_DEFAULTS, resetDefaults);
                return true;
            }
            return false;
        });

        // Add to table.
        if (!vr) {
            graphicsModel[0][0] = starBrightness;
            graphicsModel[0][1] = magnitudeMultiplier;
            graphicsModel[0][2] = starGlowFactor;
            graphicsModel[0][3] = pointSize;
            graphicsModel[0][4] = starBaseLevel;
            graphicsModel[0][5] = bloomSlider;
            graphicsModel[0][6] = unsharpMaskSlider;
            graphicsModel[1][0] = lensFlare;
            graphicsModel[1][1] = starGlowButton;
            graphicsModel[1][2] = motionBlur;
            graphicsModel[1][4] = resetDefaults;
            // Regular mode (gamepad).
            graphicsT.add(starBrightness).padBottom(pad10).padRight(pad40);
            graphicsT.add(lensFlare).padBottom(pad10).row();
            graphicsT.add(magnitudeMultiplier).padBottom(pad10).padRight(pad40);
            graphicsT.add(starGlowButton).padBottom(pad10).row();
            graphicsT.add(starGlowFactor).padBottom(pad10).padRight(pad40);
            graphicsT.add(motionBlur).padBottom(pad10).row();
            graphicsT.add(pointSize).padBottom(pad10).padRight(pad40);
            graphicsT.add().padBottom(pad10).row();
            graphicsT.add(starBaseLevel).padBottom(pad10).padRight(pad40);
            graphicsT.add(resetDefaults).padBottom(pad10).row();
            graphicsT.add(bloomSlider).padBottom(pad10).padRight(pad40);
            graphicsT.add().row();
            graphicsT.add(unsharpMaskSlider).padBottom(pad10).padRight(pad40);
            graphicsT.add().row();
        } else {
            graphicsModel[0][0] = starBrightness;
            graphicsModel[0][1] = starGlowFactor;
            graphicsModel[0][2] = starBaseLevel;
            graphicsModel[0][3] = bloomSlider;
            graphicsModel[1][0] = magnitudeMultiplier;
            graphicsModel[1][1] = pointSize;
            graphicsModel[1][2] = unsharpMaskSlider;
            graphicsModel[1][4] = resetDefaults;
            // VR mode.
            graphicsT.add(starBrightness).padBottom(pad10).padRight(pad40);
            graphicsT.add(magnitudeMultiplier).padBottom(pad10).row();
            graphicsT.add(starGlowFactor).padBottom(pad10).padRight(pad40);
            graphicsT.add(pointSize).padBottom(pad10).row();
            graphicsT.add(starBaseLevel).padBottom(pad10).padRight(pad40);
            graphicsT.add(unsharpMaskSlider).padBottom(pad10).row();
            graphicsT.add(bloomSlider).padBottom(pad10).padRight(pad40);
            graphicsT.add(resetDefaults).padBottom(pad10).row();
        }

        tabContents.add(container(graphicsT, tw1, th));
        updatePads(graphicsT);

        // SYSTEM
        Actor[][] systemModel = new Actor[1][2];
        model.add(systemModel);

        Table sysT = new Table(skin);

        // Debug info panel
        final Label debugLabel = new Label(I18n.msg("gui.system.debuginfo"), skin, "header-raw");
        debugInfo = new OwnCheckBox("", skin, 0f);
        systemModel[0][0] = debugInfo;
        debugInfo.setChecked(Settings.settings.program.debugInfo);
        debugInfo.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_DEBUG_CMD, debugInfo, debugInfo.isChecked());
                return true;
            }
            return false;
        });
        sysT.add(debugLabel).right().padBottom(pad40).padRight(pad20);
        sysT.add(debugInfo).left().padBottom(pad40).row();

        // Quit button
        OwnTextButton quit = new OwnTextIconButton(I18n.msg("gui.quit.title"), Align.center, skin, "quit");
        systemModel[0][1] = quit;
        quit.setWidth(ww);
        quit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                GaiaSky.postRunnable(() -> Gdx.app.exit());
                return true;
            }
            return false;
        });
        sysT.add(quit).colspan(2);

        tabContents.add(container(sysT, tw1, th));
        updatePads(sysT);

        // Create tab buttons
        if (vr) {
            vrInfoButton = new OwnTextButton(I18n.msg("gui.info"), skin, "toggle-big");
            tabButtons.add(vrInfoButton);
            vrInfoButton.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    selectedTab = tabButtons.indexOf(vrInfoButton);
                    updateTabs();
                }
                return false;
            });
        }

        searchButton = new OwnTextButton(I18n.msg("gui.search"), skin, "toggle-big");
        tabButtons.add(searchButton);
        searchButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                selectedTab = tabButtons.indexOf(searchButton);
                updateTabs();
            }
            return false;
        });

        bookmarksButton = new OwnTextButton(I18n.msg("gui.bookmarks"), skin, "toggle-big");
        tabButtons.add(bookmarksButton);
        bookmarksButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                selectedTab = tabButtons.indexOf(bookmarksButton);
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
        if (vr) {
            menu.add(vrInfoButton).left().row();
        }
        menu.add(searchButton).left().row();
        menu.add(bookmarksButton).left().row();
        menu.add(cameraButton).left().row();
        menu.add(timeButton).left().row();
        menu.add(typesButton).left().row();
        menu.add(controlsButton).left().row();
        menu.add(graphicsButton).left().row();
        menu.add(systemButton).left().padBottom(pad30).row();
        menu.add(rb).center();

        Table padTable = new Table(skin);
        padTable.pad(pad30);
        padTable.setBackground("bg-pane-border");
        if (vr) {
            var topCell = padTable.add(topLine).center().colspan(2);
            topCell.row();
        }
        menu.pack();
        padTable.add(menu).left();
        contentCell = padTable.add().expandX().left();
        contentCell.row();

        if (vr) {
            padTable.add(buttonGoHome).right().colspan(2);
        }

        content.add(padTable);

        content.setFillParent(true);
        content.center();
        content.pack();

        updateTabs();
        updateFocused(true);

        if (scene == null)
            scene = GaiaSky.instance.scene;

        initialized = true;
    }

    @SuppressWarnings("all")
    private void fillBookmarksColumn(Cell<Container>[] columns, int columnIndex, List<BookmarkNode> bookmarks, Actor[][] model, float w, float h) {
        fillBookmarksColumn(columns, columnIndex, bookmarks, model, w, h, true);
    }

    private void fillBookmarksColumn(Cell<Container>[] columns,
                                     int columnIndex,
                                     List<BookmarkNode> bookmarks,
                                     Actor[][] model,
                                     float w,
                                     float h,
                                     boolean select) {
        assert columns != null : "Column list can't be null";
        assert columnIndex < columns.length && columnIndex >= 0 : "Column index out of bounds";

        Cell<Container> column = columns[columnIndex];
        column.clearActor();
        column.left().top().padRight(pad5);
        column.width(w + 20f);

        Table group = new Table(skin);
        group.top();

        int row = 0;
        model[columnIndex] = new Actor[bookmarks.size()];
        for (var node : bookmarks) {
            var button = new BookmarkButton(node, skin);
            button.setSize(w, h);

            // Add listener to folders.
            if (node.isTypeFolder()) {
                final int rowIndex = row;
                button.addListener((event) -> {
                    if (event instanceof ChangeEvent) {
                        selectInRow(columnIndex, rowIndex, true);
                        updateFocused();
                    }
                    return false;
                });
            }

            group.add(button).left().row();

            model[columnIndex][row++] = button;
        }
        OwnScrollPane scroll = new OwnScrollPane(group, skin, "minimalist-nobg");
        scroll.setFadeScrollBars(false);
        scroll.setScrollbarsVisible(true);
        scroll.setScrollingDisabled(true, false);
        scroll.setWidth(w + 20f);
        scroll.setHeight(th - 30f);

        column.setActor(scroll);
        column.top();

        if (select) {
            selectInCol(columnIndex, 0, true);
            updateFocused();
        }
    }

    private void updateFocusedBookmark() {
        if (((vr && selectedTab == 2) || (!vr && selectedTab == 1)) && fi < maxBookmarkDepth - 1) {
            if (currentModel[fi][fj] instanceof BookmarkButton selectedBookmark) {
                // Move scroll position.
                var scroll = GuiUtils.getScrollPaneAncestor(selectedBookmark);
                if (scroll != null) {
                    var coordinates = selectedBookmark.localToAscendantCoordinates(scroll.getActor(),
                                                                                   new Vector2(selectedBookmark.getX(), selectedBookmark.getY()));
                    scroll.scrollTo(coordinates.x, coordinates.y, selectedBookmark.getWidth(), selectedBookmark.getHeight() + 250f);
                }


                if (selectedBookmark.bookmark.isTypeFolder()) {
                    // If it is a folder, we need to populate the next.
                    fillBookmarksColumn(bookmarkColumns, fi + 1, selectedBookmark.bookmark.children, currentModel, bw, bh, false);
                } else {
                    // Clear all columns right of current.
                    for (int i = fi + 1; i < maxBookmarkDepth; i++) {
                        bookmarkColumns[i].clearActor();
                        currentModel[i] = null;
                    }
                }
            }
        }
    }

    private void addDeviceTypeInfo(Table table, String name, String text) {
        table.add(new OwnLabel(name, skin, "header")).top().left().padRight(pad40).padBottom(pad10);
        table.add(new OwnLabel(text, skin, "big")).top().left().padBottom(pad10).row();
    }

    private void addTextKey(String text, Actor[][] m, int i, int j, boolean nl) {
        addTextKey(text, (event) -> {
            if (event instanceof ChangeEvent) {
                searchField.setText(searchField.getText() + text.toLowerCase());
            }
            return false;
        }, m, i, j, nl, -1, 1);
    }

    private void addTextKey(String text, EventListener el, Actor[][] m, int i, int j, boolean nl, float width, int colspan) {
        OwnTextButton key = new OwnTextButton(text, skin, "big");
        if (width > 0) {
            key.setWidth(width);
        } else {
            key.setMinWidth(70f);
        }
        key.addListener(el);
        m[i][j] = key;
        var c = searchT.add(key).left().padRight(pad5).padBottom(pad10);
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
    public void doneLoading(AssetManager assetManager) {
        stage.setKeyboardFocus(null);
    }

    @Override
    public void update(double dt) {
        super.update(dt);
        if (gamepadListener.isActive()) {
            gamepadListener.update();
        }
    }

    private ScrollPane container(Table t, float w, float h) {
        var c = new OwnScrollPane(t, skin, "minimalist-nobg");
        t.center();
        c.setFadeScrollBars(true);
        c.setupFadeScrollBars(1f, 3f);
        c.setForceScroll(false, false);
        if (w > 0 && h > 0) {
            c.setSize(w, h);
        }
        return c;
    }

    private void updatePads(Table t) {
        var cells = t.getCells();
        for (Cell<?> c : cells) {
            if (c.getActor() instanceof Button && !(c.getActor() instanceof CheckBox)) {
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
     * @return True if the element was selected, false otherwise
     */
    public boolean selectInRow(int i, int j, boolean right) {
        int bi = fi;
        fi = i;
        fj = j;
        if (currentModel == null) {
            return false;
        }
        if (currentModel.length > 0 && currentModel[fi] != null) {
            // Not all columns need to be the same length!
            if (fj >= currentModel[fi].length) {
                fj = currentModel[fi].length - 1;
            }

            while (currentModel[fi][fj] == null) {
                // Move to next column.
                fi = (fi + (right ? 1 : -1)) % currentModel.length;
                if (fi < 0) {
                    fi = currentModel.length - 1;
                }
                if (fi == i || currentModel[fi] == null) {
                    return false;
                }

                // Not all columns need to be the same length!
                if (fj >= currentModel[fi].length) {
                    fj = currentModel[fi].length - 1;
                }
            }
            return true;
        } else if (currentModel[fi] == null) {
            // Go back.
            fi = bi;
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
            if (!vr && GuiUtils.isInputWidget(actor)) {
                stage.setKeyboardFocus(actor);
            }

            // In bookmarks, we populate next if we select a folder.
            updateFocusedBookmark();
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
        if (currentModel != null && currentModel[fi] != null && selectInCol(fi, update(fj, -1, currentModel[fi].length), false)) {
            updateFocused();
        }
    }

    public void down() {
        if (currentModel != null && currentModel[fi] != null && selectInCol(fi, update(fj, 1, currentModel[fi].length), true)) {
            updateFocused();
        }
    }

    public void left() {
        if (currentModel != null && selectInRow(update(fi, -1, currentModel.length), fj, false)) {
            updateFocused();
        }
    }

    public void right() {
        if (currentModel != null && selectInRow(update(fi, 1, currentModel.length), fj, true)) {
            updateFocused();
        }
    }

    private Actor getFocusedActor() {
        return currentModel != null ? currentModel[fi][fj] : null;
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

    public void back() {
        EventManager.publish(Event.SHOW_CONTROLLER_GUI_ACTION, this);
    }

    public Table getContent() {
        return content;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (initialized) {
            switch (event) {
                case SCENE_LOADED -> this.scene = (Scene) data[0];
                case SHOW_CONTROLLER_GUI_ACTION -> {
                    if (content.isVisible() && content.hasParent()) {
                        removeGamepadGui();
                    } else {
                        addGamepadGui();
                    }
                }
                case TOGGLE_VISIBILITY_CMD -> {
                    if (visibilityButtonMap != null) {
                        String key = (String) data[0];
                        Button b = visibilityButtonMap.get(key);
                        if (b != null && source != b) {
                            b.setProgrammaticChangeEvents(false);
                            if (data.length == 2) {
                                b.setChecked((Boolean) data[1]);
                            } else {
                                b.setChecked(!b.isChecked());
                            }
                            b.setProgrammaticChangeEvents(true);
                        }
                    }
                }
                case TIME_STATE_CMD -> {
                    if (timeStartStop != null) {
                        boolean on = (Boolean) data[0];
                        timeStartStop.setProgrammaticChangeEvents(false);
                        timeStartStop.setChecked(on);
                        timeStartStop.setText(on ? "Stop time" : "Start time");
                        timeStartStop.setProgrammaticChangeEvents(true);
                    }
                }
                case CAMERA_MODE_CMD -> {
                    if (cameraMode != null && source != cameraMode && !vr) {
                        // Update camera mode selection
                        final var mode = (CameraMode) data[0];
                        var cModes = cameraMode.getItems();
                        CameraComboBoxBean selected = null;
                        for (var cameraModeBean : cModes) {
                            if (cameraModeBean.mode == mode) {
                                selected = cameraModeBean;
                                break;
                            }
                        }
                        if (selected != null) {
                            cameraMode.getSelection().setProgrammaticChangeEvents(false);
                            cameraMode.setSelected(selected);
                            cameraMode.getSelection().setProgrammaticChangeEvents(true);
                        }
                    } else if (cameraModeLabel != null) {
                        final var mode = (CameraMode) data[0];
                        cameraModeLabel.setText(mode.toStringI18n());
                        if (mode != CameraMode.FOCUS_MODE && cameraFocusLabel != null) {
                            cameraFocusLabel.setText("-");
                        }
                    }
                }
                case FOCUS_CHANGE_CMD -> {
                    if (cameraFocusLabel != null) {
                        String focusName;
                        if (data[0] instanceof String) {
                            focusName = (String) data[0];
                        } else if (data[0] instanceof FocusView focusView) {
                            view.setEntity(focusView.getEntity());
                            focusName = view.getLocalizedName();
                        } else {
                            var entity = (Entity) data[0];
                            view.setEntity(entity);
                            focusName = view.getLocalizedName();
                        }
                        cameraFocusLabel.setText(focusName);
                    }
                }
                case STAR_POINT_SIZE_CMD -> {
                    if (source != pointSize && pointSize != null) {
                        hackProgrammaticChangeEvents = false;
                        float newSize = (float) data[0];
                        pointSize.setMappedValue(newSize);
                        hackProgrammaticChangeEvents = true;
                    }
                }
                case STAR_BRIGHTNESS_CMD -> {
                    if (source != starBrightness && starBrightness != null) {
                        Float brightness = (Float) data[0];
                        hackProgrammaticChangeEvents = false;
                        starBrightness.setMappedValue(brightness);
                        hackProgrammaticChangeEvents = true;
                    }
                }
                case STAR_BRIGHTNESS_POW_CMD -> {
                    if (source != magnitudeMultiplier && magnitudeMultiplier != null) {
                        Float pow = (Float) data[0];
                        hackProgrammaticChangeEvents = false;
                        magnitudeMultiplier.setMappedValue(pow);
                        hackProgrammaticChangeEvents = true;
                    }
                }
                case STAR_GLOW_FACTOR_CMD -> {
                    if (source != starGlowFactor && starGlowFactor != null) {
                        Float glowFactor = (Float) data[0];
                        hackProgrammaticChangeEvents = false;
                        starGlowFactor.setMappedValue(glowFactor);
                        hackProgrammaticChangeEvents = true;
                    }
                }
                case STAR_BASE_LEVEL_CMD -> {
                    if (source != starBaseLevel && starBaseLevel != null) {
                        Float baseLevel = (Float) data[0];
                        hackProgrammaticChangeEvents = false;
                        starBaseLevel.setMappedValue(baseLevel);
                        hackProgrammaticChangeEvents = true;
                    }
                }
                case STEREOSCOPIC_CMD -> {
                    if (button3d != null && source != button3d && !vr) {
                        button3d.setProgrammaticChangeEvents(false);
                        button3d.setChecked((boolean) data[0]);
                        button3d.setProgrammaticChangeEvents(true);
                    }
                }
                case CUBEMAP_CMD -> {
                    if (!vr) {
                        final CubemapProjection proj = (CubemapProjection) data[1];
                        final boolean enable = (boolean) data[0];
                        if (proj.isPanorama() && source != buttonCubemap && buttonCubemap != null) {
                            buttonCubemap.setProgrammaticChangeEvents(false);
                            buttonCubemap.setChecked(enable);
                            buttonCubemap.setProgrammaticChangeEvents(true);
                            fovSlider.setDisabled(enable);
                        } else if (proj.isPlanetarium() && source != buttonDome && buttonDome != null) {
                            buttonDome.setProgrammaticChangeEvents(false);
                            buttonDome.setChecked(enable);
                            buttonDome.setProgrammaticChangeEvents(true);
                            fovSlider.setDisabled(enable);
                        } else if (proj.isOrthosphere() && source != buttonOrthoSphere && buttonOrthoSphere != null) {
                            buttonOrthoSphere.setProgrammaticChangeEvents(false);
                            buttonOrthoSphere.setChecked(enable);
                            buttonOrthoSphere.setProgrammaticChangeEvents(true);
                            fovSlider.setDisabled(enable);
                        }
                    }
                }
                case CROSSHAIR_CLOSEST_CMD -> {
                    if (source != this && crosshairClosest != null) {
                        crosshairClosest.setProgrammaticChangeEvents(false);
                        crosshairClosest.setChecked((Boolean) data[0]);
                        crosshairClosest.setProgrammaticChangeEvents(true);
                    }
                }
                case CROSSHAIR_FOCUS_CMD -> {
                    if (source != this && crosshairFocus != null) {
                        crosshairFocus.setProgrammaticChangeEvents(false);
                        crosshairFocus.setChecked((Boolean) data[0]);
                        crosshairFocus.setProgrammaticChangeEvents(true);
                    }

                }
                case CROSSHAIR_HOME_CMD -> {
                    if (source != this && crosshairHome != null) {
                        crosshairHome.setProgrammaticChangeEvents(false);
                        crosshairHome.setChecked((Boolean) data[0]);
                        crosshairHome.setProgrammaticChangeEvents(true);
                    }

                }
                case INVERT_X_CMD -> {
                    if (source != this && invertXButton != null) {
                        invertXButton.setProgrammaticChangeEvents(false);
                        invertXButton.setChecked((Boolean) data[0]);
                        invertXButton.setProgrammaticChangeEvents(true);
                    }
                }
                case INVERT_Y_CMD -> {
                    if (source != this && invertYButton != null) {
                        invertYButton.setProgrammaticChangeEvents(false);
                        invertYButton.setChecked((Boolean) data[0]);
                        invertYButton.setProgrammaticChangeEvents(true);
                    }
                }
                default -> {
                }
            }
        }
    }

    private void addButtonTooltip(Button button, ComponentType ct, String name) {
        String[] hk = KeyBindings.instance.getStringKeys("action.toggle/" + ct.key, true);
        String text = TextUtils.capitalise(name);
        if (ct.equals(ComponentType.Constellations)) {
            text += " - " + I18n.msg("gui.tooltip.ct.constellations.hip");
        }

        if (hk != null && hk.length > 0) {
            button.addListener(new OwnTextHotkeyTooltip(text, hk, skin, 9));
        } else {
            button.addListener(new OwnTextTooltip(text, skin));
        }
    }

    public boolean removeGamepadGui() {
        // Hide and remove
        if (content.isVisible() && content.hasParent()) {
            searchField.setText("");
            content.addAction(Actions.sequence(Actions.alpha(1f),
                                               Actions.fadeOut(Settings.settings.program.ui.getAnimationSeconds()),
                                               Actions.visible(false),
                                               Actions.run(() -> {
                                                   content.remove();
                                                   content.clear();
                                                   stage.setKeyboardFocus(null);
                                                   removeGamepadListener();
                                               })));

            return true;
        }
        return false;
    }

    public void addGamepadGui() {
        // Add and show
        if (!content.isVisible() || !content.hasParent()) {
            rebuildGui();
            programmaticUpdate();
            stage.addActor(content);
            content.addAction(Actions.sequence(Actions.alpha(0f),
                                               Actions.visible(true),
                                               Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds()),
                                               Actions.run(() -> {
                                                   updateFocused();
                                                   addGamepadListener();
                                               })));

            // Close all open windows.
            EventManager.publish(Event.CLOSE_ALL_GUI_WINDOWS_CMD, this);
        }
    }

    public void programmaticUpdate() {
        if (topLine != null) {
            topLine.programmaticUpdate();
        }
        if (focusInterface != null) {
            focusInterface.programmaticUpdate();
        }
    }

    private void addGamepadListener() {
        if (gamepadListener != null) {
            GamepadSettings gamepadSettings = Settings.settings.controls.gamepad;
            // Backup and clean
            backupGamepadListeners = gamepadSettings.getControllerListeners();
            gamepadSettings.removeAllControllerListeners();

            // Add and activate.
            gamepadSettings.addControllerListener(gamepadListener);
        }
    }

    private void removeGamepadListener() {
        if (gamepadListener != null) {
            GamepadSettings gamepadSettings = Settings.settings.controls.gamepad;
            // Remove current listener
            gamepadSettings.removeControllerListener(gamepadListener);

            // Restore backup.
            gamepadSettings.setControllerListeners(backupGamepadListeners);
            backupGamepadListeners = null;
        }
    }

    private static class GamepadGuiListener extends GuiGamepadListener {
        private final GamepadGui gui;

        public GamepadGuiListener(GamepadGui gui, String mappingsFile) {
            super(mappingsFile, gui.stage);
            this.gui = gui;
        }

        @Override
        public boolean buttonUp(Controller controller, int buttonCode) {
            super.buttonUp(controller, buttonCode);
            if (buttonCode == mappings.getButtonA()) {
                actionUp();
                return true;
            }
            return false;
        }

        @Override
        public void actionDown() {
            Actor target = stage.getKeyboardFocus();

            if (target != null) {
                if (target instanceof CheckBox cb) {
                    // Check or uncheck.
                    if (!cb.isDisabled()) {
                        cb.setChecked(!cb.isChecked());
                    }
                } else if (target instanceof Button b) {
                    // Touch-down event for buttons.
                    InputEvent inputEvent = Pools.obtain(InputEvent::new);
                    inputEvent.setTarget(b);
                    inputEvent.setType(InputEvent.Type.touchDown);
                    b.fire(inputEvent);
                    Pools.free(inputEvent);
                } else {
                    // Fire change event.
                    ChangeEvent event = Pools.obtain(ChangeEvent::new);
                    event.setTarget(target);
                    target.fire(event);
                    Pools.free(event);
                }
            }
        }

        public void actionUp() {
            Actor target = stage.getKeyboardFocus();

            if (target != null) {
                if (target instanceof Button b) {
                    // Touch-up event.
                    InputEvent inputEvent = Pools.obtain(InputEvent::new);
                    inputEvent.setTarget(b);
                    inputEvent.setType(InputEvent.Type.touchUp);
                    b.fire(inputEvent);
                    Pools.free(inputEvent);
                }
            }
        }

        @Override
        public Array<Group> getContentContainers() {
            var a = new Array<Group>(1);
            a.add(gui.content);
            return a;
        }

        @Override
        public void back() {
            gui.back();
        }

        @Override
        public void start() {
            gui.back();
        }

        @Override
        public void select() {

        }

        @Override
        public void tabLeft() {
            gui.tabLeft();
        }

        @Override
        public void tabRight() {
            gui.tabRight();
        }

        @Override
        public void moveUp() {
            gui.up();
        }

        @Override
        public void moveDown() {
            gui.down();
        }

        @Override
        public void moveLeft() {
            gui.left();
        }

        @Override
        public void moveRight() {
            gui.right();
        }

        @Override
        public void rightStickVertical(float value) {
            super.rightStickVertical(gui.getFocusedActor(), value);
        }

        @Override
        public void rightStickHorizontal(float value) {
            super.rightStickHorizontal(gui.getFocusedActor(), value);
        }

        @Override
        public void rightTrigger(float value) {
            super.rightTrigger(gui.getFocusedActor(), value);
        }

        @Override
        public void leftTrigger(float value) {
            super.leftTrigger(gui.getFocusedActor(), value);
        }
    }

    @Override
    public void resizeImmediate(final int width, final int height) {
        stage.getViewport().update(width, height, true);
        if (content.isVisible() && content.hasParent()) {
            rebuildGui();
        }
    }

}

