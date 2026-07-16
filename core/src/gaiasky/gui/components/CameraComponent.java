/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.beans.CameraComboBoxBean;
import gaiasky.gui.main.KeyBindings;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.util.Constants;
import gaiasky.util.SlaveManager;
import gaiasky.util.TextUtils;
import gaiasky.util.camera.rec.Camcorder;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.util.Objects;

/**
 * GUI component that provides camera controls in the Gaia Sky control panel.
 * Includes camera mode selection, speed and rotation sliders, field of view
 * control, stereoscopic mode toggles (3D, dome, cubemap, orthosphere),
 * cinematic camera toggle, focus locks, and camera recording controls.
 * Subscribes to camera-related events to synchronize UI state.
 */
public class CameraComponent extends GuiComponent implements IObserver {

    protected OwnLabel date;
    protected SelectBox<String> cameraSpeedLimit;
    protected SelectBox<CameraComboBoxBean> cameraMode;
    protected OwnSliderReset fieldOfView, cameraSpeed, turnSpeed, rotateSpeed;
    protected CheckBox focusLock, orientationLock, cinematic;
    protected OwnTextIconButton button3d, buttonDome, buttonCubemap, buttonOrthosphere, buttonMaster;
    protected OwnImageButton recCamera, keyframesEditor, playCamera;
    protected boolean fovFlag = true;
    private boolean fieldLock;

    public CameraComponent(Skin skin,
                           Stage stage) {
        super(skin, stage);
    }

    @Override
    public void initialize(float componentWidth) {
        float iw = 32f;

        // Record camera button.
        recCamera = new OwnImageButton(skin, "rec");
        recCamera.setSize(iw, iw);
        recCamera.setName("recCam");
        recCamera.setChecked(GaiaSky.settings().runtime.recordCamera);
        recCamera.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (recCamera.isChecked() && Camcorder.instance.isPlaying()) {
                    // Nope.
                    recCamera.setCheckedNoFire(false);
                    return false;
                } else if (!recCamera.isChecked() && !Camcorder.instance.isRecording()) {
                    // Nope.
                    recCamera.setCheckedNoFire(false);
                    return false;
                }
                EventManager.publish(Event.RECORD_CAMERA_CMD, recCamera, recCamera.isChecked(), null);
                return true;
            }
            return false;
        });
        recCamera.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.reccamera"), skin));

        // Record camera (keyframes).
        keyframesEditor = new OwnImageButton(skin, "rec-key");
        keyframesEditor.setSize(iw, iw);
        keyframesEditor.setName("recKeyframeCamera");
        keyframesEditor.setChecked(GaiaSky.settings().runtime.recordKeyframeCamera);
        keyframesEditor.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_KEYFRAMES_WINDOW_ACTION, keyframesEditor);
                return true;
            }
            return false;
        });
        keyframesEditor.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.reccamerakeyframe"), skin));

        // Play camera button.
        playCamera = new OwnImageButton(skin, "play");
        playCamera.setSize(iw, iw);
        playCamera.setName("playCam");
        playCamera.setChecked(false);
        playCamera.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (Camcorder.instance.isRecording()) {
                    // Nope.
                    return false;
                }
                EventManager.publish(Event.SHOW_PLAYCAMERA_CMD, playCamera);
                return true;
            }
            return false;
        });

        playCamera.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.playcamera"), skin));

        // Camera paths group.
        Table cameraPathGroup = new Table(skin);
        cameraPathGroup.setWidth(componentWidth);
        cameraPathGroup.add(new Separator(skin, "gray")).center().growX().padRight(pad8);
        cameraPathGroup.add(recCamera).center().padRight(pad4);
        cameraPathGroup.add(keyframesEditor).center().padRight(pad4);
        cameraPathGroup.add(playCamera).center().padRight(pad8);
        cameraPathGroup.add(new Separator(skin, "gray")).center().growX();

        // Cinematic camera check box.
        cinematic = new OwnCheckBox(I18n.msg("gui.camera.cinematic"), skin, pad8);
        cinematic.setName("cinematic camera");
        cinematic.setChecked(GaiaSky.settings().scene.camera.cinematic);
        String[] hkc = KeyBindings.instance.getStringKeys("action.toggle/camera.cinematic", true);
        cinematic.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("gui.camera.cinematic")), hkc, skin));
        cinematic.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.CAMERA_CINEMATIC_CMD, cinematic, cinematic.isChecked());
                return true;
            }
            return false;
        });

        // Camera mode.
        Label modeLabel = new Label(I18n.msg("gui.camera.mode"), skin, "default");
        int cameraModes = CameraMode.values().length;
        CameraComboBoxBean[] cameraOptions = new CameraComboBoxBean[cameraModes];
        for (int i = 0; i < cameraModes; i++) {
            cameraOptions[i] = new CameraComboBoxBean(Objects.requireNonNull(CameraMode.getMode(i)).toStringI18n(), CameraMode.getMode(i));
        }
        cameraMode = new OwnSelectBox<>(skin);
        cameraMode.setName("camera mode");
        cameraMode.setWidth(componentWidth);
        cameraMode.setItems(cameraOptions);
        cameraMode.addListener(event -> {
            if (event instanceof ChangeEvent) {
                CameraComboBoxBean selection = cameraMode.getSelected();
                CameraMode mode = selection.mode;

                EventManager.publish(Event.CAMERA_MODE_CMD, cameraMode, mode);
                return true;
            }
            return false;
        });

        if (!GaiaSky.settings().runtime.openXr) {
            var buttonSize = 55f;
            Image icon3d = new Image(skin.getDrawable("3d-icon"));
            button3d = new OwnTextIconButton("", Align.center, icon3d, skin, "toggle");
            button3d.setChecked(GaiaSky.settings().program.modeStereo.active);
            String[] hk3d = KeyBindings.instance.getStringKeys("action.toggle/element.stereomode", true);
            button3d.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("element.stereomode")), hk3d, skin));
            button3d.setName("3d");
            button3d.setSize(buttonSize, buttonSize);
            button3d.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (button3d.isChecked()) {
                        buttonCubemap.setProgrammaticChangeEvents(true);
                        buttonCubemap.setChecked(false);
                        buttonDome.setProgrammaticChangeEvents(true);
                        buttonDome.setChecked(false);
                        buttonOrthosphere.setProgrammaticChangeEvents(true);
                        buttonOrthosphere.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.publish(Event.STEREOSCOPIC_CMD, button3d, button3d.isChecked());
                    return true;
                }
                return false;
            });

            Image iconDome = new Image(skin.getDrawable("dome-icon"));
            buttonDome = new OwnTextIconButton("", Align.center, iconDome, skin, "toggle");
            buttonDome.setChecked(GaiaSky.settings().program.modeCubemap.active && GaiaSky.settings().program.modeCubemap.isPlanetariumOn());
            String[] hkDome = KeyBindings.instance.getStringKeys("action.toggle/element.planetarium", true);
            buttonDome.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("element.planetarium")), hkDome, skin));
            buttonDome.setName("dome");
            buttonDome.setSize(buttonSize, buttonSize);
            buttonDome.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (buttonDome.isChecked()) {
                        buttonCubemap.setProgrammaticChangeEvents(true);
                        buttonCubemap.setChecked(false);
                        button3d.setProgrammaticChangeEvents(true);
                        button3d.setChecked(false);
                        buttonOrthosphere.setProgrammaticChangeEvents(true);
                        buttonOrthosphere.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.publish(Event.CUBEMAP_CMD, buttonDome, buttonDome.isChecked(), CubemapProjection.AZIMUTHAL_EQUIDISTANT);
                    fieldOfView.setDisabled(buttonDome.isChecked());
                    return true;
                }
                return false;
            });

            Image iconCubemap = new Image(skin.getDrawable("cubemap-icon"));
            buttonCubemap = new OwnTextIconButton("", Align.center, iconCubemap, skin, "toggle");
            buttonCubemap.setProgrammaticChangeEvents(false);
            buttonCubemap.setChecked(GaiaSky.settings().program.modeCubemap.active && GaiaSky.settings().program.modeCubemap.isPanoramaOn());
            String[] hkCubemap = KeyBindings.instance.getStringKeys("action.toggle/element.360", true);
            buttonCubemap.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("element.360")), hkCubemap, skin));
            buttonCubemap.setName("cubemap");
            buttonCubemap.setSize(buttonSize, buttonSize);
            buttonCubemap.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (buttonCubemap.isChecked()) {
                        buttonDome.setProgrammaticChangeEvents(true);
                        buttonDome.setChecked(false);
                        button3d.setProgrammaticChangeEvents(true);
                        button3d.setChecked(false);
                        buttonOrthosphere.setProgrammaticChangeEvents(true);
                        buttonOrthosphere.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.publish(Event.CUBEMAP_CMD, buttonCubemap, buttonCubemap.isChecked(), CubemapProjection.EQUIRECTANGULAR);
                    fieldOfView.setDisabled(buttonCubemap.isChecked());
                    return true;
                }
                return false;
            });

            Image iconOrthosphere = new Image(skin.getDrawable("orthosphere-icon"));
            buttonOrthosphere = new OwnTextIconButton("", Align.center, iconOrthosphere, skin, "toggle");
            buttonOrthosphere.setProgrammaticChangeEvents(false);
            buttonOrthosphere.setChecked(GaiaSky.settings().program.modeCubemap.active && GaiaSky.settings().program.modeCubemap.isOrthosphereOn());
            String[] hkOrthosphere = KeyBindings.instance.getStringKeys("action.toggle/element.orthosphere", true);
            buttonOrthosphere.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("element.orthosphere")), hkOrthosphere, skin));
            buttonOrthosphere.setName("orthosphere");
            buttonOrthosphere.setSize(buttonSize, buttonSize);
            buttonOrthosphere.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (buttonOrthosphere.isChecked()) {
                        buttonCubemap.setProgrammaticChangeEvents(true);
                        buttonCubemap.setChecked(false);
                        buttonDome.setProgrammaticChangeEvents(true);
                        buttonDome.setChecked(false);
                        button3d.setProgrammaticChangeEvents(true);
                        button3d.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.publish(Event.CUBEMAP_CMD, buttonOrthosphere, buttonOrthosphere.isChecked(), CubemapProjection.ORTHOSPHERE);
                    fieldOfView.setDisabled(buttonOrthosphere.isChecked());
                    return true;
                }
                return false;
            });

            if (GaiaSky.settings().program.net.isMasterInstance()) {
                Image iconMaster = new Image(skin.getDrawable("iconic-link-intact"));
                buttonMaster = new OwnTextIconButton("", Align.center, iconMaster, skin, "default");
                buttonMaster.setProgrammaticChangeEvents(false);
                buttonMaster.setSize(28f, 29.6f);
                String[] hkmaster = KeyBindings.instance.getStringKeys("action.slave.configure", true);
                buttonMaster.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.msg("element.slave.config")), hkmaster, skin));
                buttonMaster.setName("master");
                buttonMaster.setSize(buttonSize, buttonSize);
                buttonMaster.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        // Enable/disable
                        EventManager.publish(Event.SHOW_SLAVE_CONFIG_ACTION, buttonMaster);
                        return true;
                    }
                    return false;
                });
            }
        }

        fieldOfView = new OwnSliderReset(I18n.msg("gui.camera.fov"), Constants.MIN_FOV, Constants.MAX_FOV, Constants.SLIDER_STEP_TINY, 45f, skin);
        fieldOfView.setValueSuffix("°");
        fieldOfView.setName("field of view");
        fieldOfView.setWidth(componentWidth);
        fieldOfView.setValue(GaiaSky.settings().scene.camera.fov);
        fieldOfView.setDisabled(GaiaSky.settings().program.modeCubemap.isFixedFov());
        fieldOfView.addListener(event -> {
            if (fovFlag && event instanceof ChangeEvent && !SlaveManager.projectionActive() && !GaiaSky.settings().program.modeCubemap.isFixedFov()) {
                float value = fieldOfView.getMappedValue();
                EventManager.publish(Event.FOV_CMD, fieldOfView, value);
                return true;
            }
            return false;
        });

        // CAMERA SPEED LIMIT
        String[] speedLimits = new String[28];
        speedLimits[0] = I18n.msg("gui.camera.speedlimit.kmh", "1");
        speedLimits[1] = I18n.msg("gui.camera.speedlimit.kmh", "10");
        speedLimits[2] = I18n.msg("gui.camera.speedlimit.kmh", "100");
        speedLimits[3] = I18n.msg("gui.camera.speedlimit.kmh", "1000");
        speedLimits[4] = I18n.msg("gui.camera.speedlimit.kms", "1");
        speedLimits[5] = I18n.msg("gui.camera.speedlimit.kms", "10");
        speedLimits[6] = I18n.msg("gui.camera.speedlimit.kms", "100");
        speedLimits[7] = I18n.msg("gui.camera.speedlimit.kms", "1000");
        speedLimits[8] = I18n.msg("gui.camera.speedlimit.cfactor", "0.01");
        speedLimits[9] = I18n.msg("gui.camera.speedlimit.cfactor", "0.1");
        speedLimits[10] = I18n.msg("gui.camera.speedlimit.cfactor", "0.5");
        speedLimits[11] = I18n.msg("gui.camera.speedlimit.cfactor", "0.8");
        speedLimits[12] = I18n.msg("gui.camera.speedlimit.cfactor", "0.9");
        speedLimits[13] = I18n.msg("gui.camera.speedlimit.cfactor", "0.99");
        speedLimits[14] = I18n.msg("gui.camera.speedlimit.cfactor", "0.99999");
        speedLimits[15] = I18n.msg("gui.camera.speedlimit.c");
        speedLimits[16] = I18n.msg("gui.camera.speedlimit.cfactor", 2);
        speedLimits[17] = I18n.msg("gui.camera.speedlimit.cfactor", 10);
        speedLimits[18] = I18n.msg("gui.camera.speedlimit.cfactor", 1000);
        speedLimits[19] = I18n.msg("gui.camera.speedlimit.aus", 1);
        speedLimits[20] = I18n.msg("gui.camera.speedlimit.aus", 10);
        speedLimits[21] = I18n.msg("gui.camera.speedlimit.aus", 1000);
        speedLimits[22] = I18n.msg("gui.camera.speedlimit.aus", 10000);
        speedLimits[23] = I18n.msg("gui.camera.speedlimit.pcs", 1);
        speedLimits[24] = I18n.msg("gui.camera.speedlimit.pcs", 2);
        speedLimits[25] = I18n.msg("gui.camera.speedlimit.pcs", 10);
        speedLimits[26] = I18n.msg("gui.camera.speedlimit.pcs", 1000);
        speedLimits[27] = I18n.msg("gui.camera.speedlimit.nolimit");

        cameraSpeedLimit = new OwnSelectBox<>(skin);
        cameraSpeedLimit.setName("camera speed limit");
        cameraSpeedLimit.setWidth(componentWidth);
        cameraSpeedLimit.setItems(speedLimits);
        cameraSpeedLimit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                int idx = cameraSpeedLimit.getSelectedIndex();
                EventManager.publish(Event.SPEED_LIMIT_CMD, cameraSpeedLimit, idx);
                return true;
            }
            return false;
        });
        cameraSpeedLimit.setSelectedIndex(GaiaSky.settings().scene.camera.speedLimitIndex);

        // CAMERA SPEED
        cameraSpeed = new OwnSliderReset(I18n.msg("gui.camera.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_CAM_SPEED,
                                        Constants.MAX_CAM_SPEED, 10f, skin);
        cameraSpeed.setName("camera speed");
        cameraSpeed.setWidth(componentWidth);
        cameraSpeed.setDisplayValueMapped(false);
        cameraSpeed.setMappedValue(GaiaSky.settings().scene.camera.speed);
        cameraSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.publish(Event.CAMERA_SPEED_CMD, cameraSpeed, cameraSpeed.getMappedValue());
                return true;
            }
            return false;
        });

        // ROTATION SPEED
        rotateSpeed = new OwnSliderReset(I18n.msg("gui.rotation.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_ROT_SPEED,
                                        Constants.MAX_ROT_SPEED, 5000f, skin);
        rotateSpeed.setName("rotate speed");
        rotateSpeed.setWidth(componentWidth);
        rotateSpeed.setDisplayValueMapped(false);
        rotateSpeed.setMappedValue(GaiaSky.settings().scene.camera.rotate);
        rotateSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.publish(Event.ROTATION_SPEED_CMD, rotateSpeed, rotateSpeed.getMappedValue());
                return true;
            }
            return false;
        });

        // TURNING SPEED
        turnSpeed = new OwnSliderReset(I18n.msg("gui.turn.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_TURN_SPEED,
                                      Constants.MAX_TURN_SPEED, 1060f, skin);
        turnSpeed.setName("turn speed");
        turnSpeed.setWidth(componentWidth);
        turnSpeed.setDisplayValueMapped(false);
        turnSpeed.setMappedValue(GaiaSky.settings().scene.camera.turn);
        turnSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.publish(Event.TURNING_SPEED_CMD, turnSpeed, turnSpeed.getMappedValue(), true);
                return true;
            }
            return false;
        });

        // FOCUS_MODE lock
        focusLock = new CheckBox(" " + I18n.msg("gui.camera.lock"), skin);
        focusLock.setName("focus lock");
        focusLock.setChecked(GaiaSky.settings().scene.camera.focusLock.position);
        focusLock.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.FOCUS_LOCK_CMD, focusLock, focusLock.isChecked());
                orientationLock.setVisible(focusLock.isChecked());
                return true;
            }
            return false;
        });

        // FOCUS_MODE orientation lock
        orientationLock = new CheckBox(" " + I18n.msg("gui.camera.lock.orientation"), skin);
        orientationLock.setName("orientation lock");
        orientationLock.setChecked(GaiaSky.settings().scene.camera.focusLock.orientation);
        orientationLock.setVisible(GaiaSky.settings().scene.camera.focusLock.position);
        orientationLock.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.ORIENTATION_LOCK_CMD, orientationLock, orientationLock.isChecked());
                return true;
            }
            return false;
        });

        HorizontalGroup buttonGroup = null;
        if (!GaiaSky.settings().runtime.openXr) {
            buttonGroup = new HorizontalGroup();
            buttonGroup.space(pad4);
            buttonGroup.addActor(button3d);
            buttonGroup.addActor(buttonDome);
            buttonGroup.addActor(buttonCubemap);
            buttonGroup.addActor(buttonOrthosphere);
            if (GaiaSky.settings().program.net.isMasterInstance())
                buttonGroup.addActor(buttonMaster);
        }

        Table cameraGroup = new Table(skin);
        cameraGroup.align(Align.left);

        cameraGroup.add(cameraPathGroup).center().growX().padBottom(pad9).row();
        cameraGroup.add(group(modeLabel, cameraMode, pad3)).top().left().padBottom(pad9).row();
        cameraGroup.add(group(new Label(I18n.msg("gui.camera.speedlimit"), skin, "default"), cameraSpeedLimit, pad3)).top().left().padBottom(pad20).row();
        cameraGroup.add(fieldOfView).top().left().padBottom(pad9).row();
        cameraGroup.add(cameraSpeed).top().left().padBottom(pad9).row();
        cameraGroup.add(rotateSpeed).top().left().padBottom(pad9).row();
        cameraGroup.add(turnSpeed).top().left().padBottom(pad20).row();
        cameraGroup.add(cinematic).top().left().padBottom(pad9).row();
        cameraGroup.add(focusLock).top().left().padBottom(pad9).row();
        cameraGroup.add(orientationLock).top().left().row();
        if (!GaiaSky.settings().runtime.openXr)
            cameraGroup.add(group(new Label("", skin), buttonGroup, pad3)).top().center();

        component = cameraGroup;

        cameraGroup.pack();
        EventManager.instance.subscribe(this, Event.CAMERA_MODE_CMD, Event.ROTATION_SPEED_CMD,
                                        Event.TURNING_SPEED_CMD, Event.CAMERA_SPEED_CMD, Event.SPEED_LIMIT_CMD, Event.STEREOSCOPIC_CMD, Event.FOV_CMD,
                                        Event.CUBEMAP_CMD, Event.CAMERA_CINEMATIC_CMD, Event.ORIENTATION_LOCK_CMD,
                                        Event.RECORD_CAMERA_CMD);
    }

    @Override
    public void notify(Event event,
                       Object source,
                       Object... data) {
        switch (event) {
        case CAMERA_CINEMATIC_CMD -> {
            boolean gui = source == cinematic;
            if (!gui) {
                cinematic.setProgrammaticChangeEvents(false);
                cinematic.setChecked((Boolean) data[0]);
                cinematic.setProgrammaticChangeEvents(true);
            }
        }
        case CAMERA_MODE_CMD -> {
            if (source != cameraMode) {
                // Update camera mode selection
                var mode = (CameraMode) data[0];
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
            }
        }
        case ROTATION_SPEED_CMD -> {
            if (source != rotateSpeed) {
                float value = (Float) data[0];
                fieldLock = true;
                rotateSpeed.setMappedValue(value);
                fieldLock = false;
            }
        }
        case CAMERA_SPEED_CMD -> {
            if (source != cameraSpeed) {
                float value = (Float) data[0];
                fieldLock = true;
                cameraSpeed.setMappedValue(value);
                fieldLock = false;
            }
        }
        case TURNING_SPEED_CMD -> {
            if (source != turnSpeed) {
                float value = (Float) data[0];
                fieldLock = true;
                turnSpeed.setMappedValue(value);
                fieldLock = false;
            }
        }
        case SPEED_LIMIT_CMD -> {
            if (source != cameraSpeedLimit) {
                int value = (Integer) data[0];
                cameraSpeedLimit.getSelection().setProgrammaticChangeEvents(false);
                cameraSpeedLimit.setSelectedIndex(value);
                cameraSpeedLimit.getSelection().setProgrammaticChangeEvents(true);
            }
        }
        case ORIENTATION_LOCK_CMD -> {
            if (source != orientationLock) {
                boolean lock = (Boolean) data[0];
                orientationLock.setProgrammaticChangeEvents(false);
                orientationLock.setChecked(lock);
                orientationLock.setProgrammaticChangeEvents(true);
            }
        }
        case FOV_CMD -> {
            if (source != fieldOfView) {
                fovFlag = false;
                fieldOfView.setValue((Float) data[0]);
                fovFlag = true;
            }
        }
        case STEREOSCOPIC_CMD -> {
            if (source != button3d && !GaiaSky.settings().runtime.openXr) {
                button3d.setProgrammaticChangeEvents(false);
                button3d.setChecked((boolean) data[0]);
                button3d.setProgrammaticChangeEvents(true);
            }
        }
        case CUBEMAP_CMD -> {
            if (!GaiaSky.settings().runtime.openXr) {
                CubemapProjection proj = (CubemapProjection) data[1];
                boolean enable = (boolean) data[0];
                if (proj.isPanorama() && source != buttonCubemap) {
                    buttonCubemap.setProgrammaticChangeEvents(false);
                    buttonCubemap.setChecked(enable);
                    buttonCubemap.setProgrammaticChangeEvents(true);
                    fieldOfView.setDisabled(enable);
                } else if (proj.isPlanetarium() && source != buttonDome) {
                    buttonDome.setProgrammaticChangeEvents(false);
                    buttonDome.setChecked(enable);
                    buttonDome.setProgrammaticChangeEvents(true);
                    fieldOfView.setDisabled(enable);
                } else if (proj.isOrthosphere() && source != buttonOrthosphere) {
                    buttonOrthosphere.setProgrammaticChangeEvents(false);
                    buttonOrthosphere.setChecked(enable);
                    buttonOrthosphere.setProgrammaticChangeEvents(true);
                    fieldOfView.setDisabled(enable);
                }
            }
        }
        case RECORD_CAMERA_CMD -> {
            boolean state = (Boolean) data[0];
            if (source != recCamera) {
                recCamera.setCheckedNoFire(state);
            }
        }
        default -> {
        }
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
