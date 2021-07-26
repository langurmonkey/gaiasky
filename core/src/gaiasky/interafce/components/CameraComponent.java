/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interafce.ControlsWindow;
import gaiasky.interafce.KeyBindings;
import gaiasky.interafce.beans.CameraComboBoxBean;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.*;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections.CubemapProjection;
import gaiasky.util.scene2d.*;

import java.util.Objects;

public class CameraComponent extends GuiComponent implements IObserver {

    protected OwnLabel date;
    protected SelectBox<String> cameraSpeedLimit;
    protected SelectBox<CameraComboBoxBean> cameraMode;
    protected OwnSliderPlus fieldOfView, cameraSpeed, turnSpeed, rotateSpeed;
    protected CheckBox focusLock, orientationLock, cinematic;
    protected OwnTextIconButton button3d, buttonDome, buttonCubemap, buttonMaster;
    protected boolean fovFlag = true;
    private boolean fieldLock = false;

    public CameraComponent(Skin skin, Stage stage) {
        super(skin, stage);
    }

    @Override
    public void initialize() {
        float contentWidth = ControlsWindow.getContentWidth();

        cinematic = new OwnCheckBox(I18n.txt("gui.camera.cinematic"), skin, pad8);
        cinematic.setName("cinematic camera");
        cinematic.setChecked(Settings.settings.scene.camera.cinematic);
        cinematic.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.CAMERA_CINEMATIC_CMD, cinematic.isChecked(), true);
                return true;
            }
            return false;
        });

        Label modeLabel = new Label(I18n.txt("gui.camera.mode"), skin, "default");
        int cameraModes = CameraMode.values().length;
        CameraComboBoxBean[] cameraOptions = new CameraComboBoxBean[cameraModes];
        for (int i = 0; i < cameraModes; i++) {
            cameraOptions[i] = new CameraComboBoxBean(Objects.requireNonNull(CameraMode.getMode(i)).toStringI18n(), CameraMode.getMode(i));
        }
        cameraMode = new OwnSelectBox<>(skin);
        cameraMode.setName("camera mode");
        cameraMode.setWidth(contentWidth);
        cameraMode.setItems(cameraOptions);
        cameraMode.addListener(event -> {
            if (event instanceof ChangeEvent) {
                CameraComboBoxBean selection = cameraMode.getSelected();
                CameraMode mode = selection.mode;

                EventManager.instance.post(Events.CAMERA_MODE_CMD, mode);
                return true;
            }
            return false;
        });

        if (!Settings.settings.runtime.openVr) {
            Image icon3d = new Image(skin.getDrawable("3d-icon"));
            button3d = new OwnTextIconButton("", icon3d, skin, "toggle");
            String hk3d = KeyBindings.instance.getStringKeys("action.toggle/element.stereomode");
            button3d.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.txt("element.stereomode")), hk3d, skin));
            button3d.setName("3d");
            button3d.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (button3d.isChecked()) {
                        buttonCubemap.setProgrammaticChangeEvents(true);
                        buttonCubemap.setChecked(false);
                        buttonDome.setProgrammaticChangeEvents(true);
                        buttonDome.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.instance.post(Events.STEREOSCOPIC_CMD, button3d.isChecked(), true);
                    return true;
                }
                return false;
            });

            Image iconDome = new Image(skin.getDrawable("dome-icon"));
            buttonDome = new OwnTextIconButton("", iconDome, skin, "toggle");
            String hkdome = KeyBindings.instance.getStringKeys("action.toggle/element.planetarium");
            buttonDome.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.txt("element.planetarium")), hkdome, skin));
            buttonDome.setName("dome");
            buttonDome.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (buttonDome.isChecked()) {
                        buttonCubemap.setProgrammaticChangeEvents(true);
                        buttonCubemap.setChecked(false);
                        button3d.setProgrammaticChangeEvents(true);
                        button3d.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.instance.post(Events.CUBEMAP_CMD, buttonDome.isChecked(), CubemapProjection.FISHEYE, true);
                    fieldOfView.setDisabled(buttonDome.isChecked());
                    return true;
                }
                return false;
            });

            Image iconCubemap = new Image(skin.getDrawable("cubemap-icon"));
            buttonCubemap = new OwnTextIconButton("", iconCubemap, skin, "toggle");
            buttonCubemap.setProgrammaticChangeEvents(false);
            String hkcubemap = KeyBindings.instance.getStringKeys("action.toggle/element.360");
            buttonCubemap.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.txt("element.360")), hkcubemap, skin));
            buttonCubemap.setName("cubemap");
            buttonCubemap.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    if (buttonCubemap.isChecked()) {
                        buttonDome.setProgrammaticChangeEvents(true);
                        buttonDome.setChecked(false);
                        button3d.setProgrammaticChangeEvents(true);
                        button3d.setChecked(false);
                    }
                    // Enable/disable
                    EventManager.instance.post(Events.CUBEMAP_CMD, buttonCubemap.isChecked(), CubemapProjection.EQUIRECTANGULAR, true);
                    fieldOfView.setDisabled(buttonCubemap.isChecked());
                    return true;
                }
                return false;
            });

            if (Settings.settings.program.net.isMasterInstance()) {
                Image iconMaster = new Image(skin.getDrawable("iconic-link-intact"));
                buttonMaster = new OwnTextIconButton("", iconMaster, skin, "default");
                buttonMaster.setProgrammaticChangeEvents(false);
                buttonMaster.setSize(28f, 29.6f);
                String hkmaster = KeyBindings.instance.getStringKeys("action.slave.configure");
                buttonMaster.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.txt("element.slave.config")), hkmaster, skin));
                buttonMaster.setName("master");
                buttonMaster.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        // Enable/disable
                        EventManager.instance.post(Events.SHOW_SLAVE_CONFIG_ACTION);
                        return true;
                    }
                    return false;
                });
            }
        }

        fieldOfView = new OwnSliderPlus(I18n.txt("gui.camera.fov"), Constants.MIN_FOV, Constants.MAX_FOV, Constants.SLIDER_STEP_SMALL, false, skin);
        fieldOfView.setValueSuffix("°");
        fieldOfView.setName("field of view");
        fieldOfView.setWidth(contentWidth);
        fieldOfView.setValue(Settings.settings.scene.camera.fov);
        fieldOfView.setDisabled(Settings.settings.program.modeCubemap.isFixedFov());
        fieldOfView.addListener(event -> {
            if (fovFlag && event instanceof ChangeEvent && !SlaveManager.projectionActive() && !Settings.settings.program.modeCubemap.isFixedFov()) {
                float value = fieldOfView.getMappedValue();
                EventManager.instance.post(Events.FOV_CHANGED_CMD, value);
                return true;
            }
            return false;
        });

        // CAMERA SPEED LIMIT
        String[] speedLimits = new String[19];
        speedLimits[0] = I18n.txt("gui.camera.speedlimit.100kmh");
        speedLimits[1] = I18n.txt("gui.camera.speedlimit.cfactor", "0.5");
        speedLimits[2] = I18n.txt("gui.camera.speedlimit.cfactor", "0.8");
        speedLimits[3] = I18n.txt("gui.camera.speedlimit.cfactor", "0.9");
        speedLimits[4] = I18n.txt("gui.camera.speedlimit.cfactor", "0.99");
        speedLimits[5] = I18n.txt("gui.camera.speedlimit.cfactor", "0.99999");
        speedLimits[6] = I18n.txt("gui.camera.speedlimit.c");
        speedLimits[7] = I18n.txt("gui.camera.speedlimit.cfactor", 2);
        speedLimits[8] = I18n.txt("gui.camera.speedlimit.cfactor", 10);
        speedLimits[9] = I18n.txt("gui.camera.speedlimit.cfactor", 1000);
        speedLimits[10] = I18n.txt("gui.camera.speedlimit.aus", 1);
        speedLimits[11] = I18n.txt("gui.camera.speedlimit.aus", 10);
        speedLimits[12] = I18n.txt("gui.camera.speedlimit.aus", 1000);
        speedLimits[13] = I18n.txt("gui.camera.speedlimit.aus", 10000);
        speedLimits[14] = I18n.txt("gui.camera.speedlimit.pcs", 1);
        speedLimits[15] = I18n.txt("gui.camera.speedlimit.pcs", 2);
        speedLimits[16] = I18n.txt("gui.camera.speedlimit.pcs", 10);
        speedLimits[17] = I18n.txt("gui.camera.speedlimit.pcs", 1000);
        speedLimits[18] = I18n.txt("gui.camera.speedlimit.nolimit");

        cameraSpeedLimit = new OwnSelectBox<>(skin);
        cameraSpeedLimit.setName("camera speed limit");
        cameraSpeedLimit.setWidth(contentWidth);
        cameraSpeedLimit.setItems(speedLimits);
        cameraSpeedLimit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                int idx = cameraSpeedLimit.getSelectedIndex();
                EventManager.instance.post(Events.SPEED_LIMIT_CMD, idx, true);
                return true;
            }
            return false;
        });
        cameraSpeedLimit.setSelectedIndex(Settings.settings.scene.camera.speedLimit);

        // CAMERA SPEED
        cameraSpeed = new OwnSliderPlus(I18n.txt("gui.camera.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_CAM_SPEED, Constants.MAX_CAM_SPEED, skin);
        cameraSpeed.setName("camera speed");
        cameraSpeed.setWidth(contentWidth);
        cameraSpeed.setMappedValue(Settings.settings.scene.camera.speed);
        cameraSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.CAMERA_SPEED_CMD, cameraSpeed.getMappedValue(), true);
                return true;
            }
            return false;
        });

        // ROTATION SPEED
        rotateSpeed = new OwnSliderPlus(I18n.txt("gui.rotation.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED, skin);
        rotateSpeed.setName("rotate speed");
        rotateSpeed.setWidth(contentWidth);
        rotateSpeed.setMappedValue(Settings.settings.scene.camera.rotate);
        rotateSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.ROTATION_SPEED_CMD, rotateSpeed.getMappedValue(), true);
                return true;
            }
            return false;
        });

        // TURNING SPEED
        turnSpeed = new OwnSliderPlus(I18n.txt("gui.turn.speed"), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED, skin);
        turnSpeed.setName("turn speed");
        turnSpeed.setWidth(contentWidth);
        turnSpeed.setMappedValue(Settings.settings.scene.camera.turn);
        turnSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.TURNING_SPEED_CMD, turnSpeed.getMappedValue(), true);
                return true;
            }
            return false;
        });

        // FOCUS_MODE lock
        focusLock = new CheckBox(" " + I18n.txt("gui.camera.lock"), skin);
        focusLock.setName("focus lock");
        focusLock.setChecked(Settings.settings.scene.camera.focusLock.position);
        focusLock.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.FOCUS_LOCK_CMD, I18n.txt("gui.camera.lock"), focusLock.isChecked());
                orientationLock.setVisible(focusLock.isChecked());
                return true;
            }
            return false;
        });

        // FOCUS_MODE orientation lock
        orientationLock = new CheckBox(" " + I18n.txt("gui.camera.lock.orientation"), skin);
        orientationLock.setName("orientation lock");
        orientationLock.setChecked(Settings.settings.scene.camera.focusLock.orientation);
        orientationLock.setVisible(Settings.settings.scene.camera.focusLock.position);
        orientationLock.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.ORIENTATION_LOCK_CMD, I18n.txt("gui.camera.lock.orientation"), orientationLock.isChecked(), true);
                return true;
            }
            return false;
        });

        HorizontalGroup buttonGroup = null;
        if (!Settings.settings.runtime.openVr) {
            buttonGroup = new HorizontalGroup();
            buttonGroup.space(pad4);
            buttonGroup.addActor(button3d);
            buttonGroup.addActor(buttonDome);
            buttonGroup.addActor(buttonCubemap);
            if (Settings.settings.program.net.isMasterInstance())
                buttonGroup.addActor(buttonMaster);
        }

        Table cameraGroup = new Table(skin);
        cameraGroup.align(Align.left);

        cameraGroup.add(group(modeLabel, cameraMode, pad3)).top().left().padBottom(pad9).row();
        cameraGroup.add(group(new Label(I18n.txt("gui.camera.speedlimit"), skin, "default"), cameraSpeedLimit, pad3)).top().left().padBottom(pad9).row();
        cameraGroup.add(fieldOfView).top().left().padBottom(pad9).row();
        cameraGroup.add(cameraSpeed).top().left().padBottom(pad9).row();
        cameraGroup.add(rotateSpeed).top().left().padBottom(pad9).row();
        cameraGroup.add(turnSpeed).top().left().padBottom(pad9).row();
        cameraGroup.add(cinematic).top().left().padBottom(pad9).row();
        cameraGroup.add(focusLock).top().left().padBottom(pad9).row();
        cameraGroup.add(orientationLock).top().left().padBottom(pad9).row();
        if (!Settings.settings.runtime.openVr)
            cameraGroup.add(group(new Label("", skin), buttonGroup, pad3)).top().center();

        component = cameraGroup;

        cameraGroup.pack();
        EventManager.instance.subscribe(this, Events.CAMERA_MODE_CMD, Events.ROTATION_SPEED_CMD, Events.TURNING_SPEED_CMD, Events.CAMERA_SPEED_CMD, Events.SPEED_LIMIT_CMD, Events.STEREOSCOPIC_CMD, Events.FOV_CHANGE_NOTIFICATION, Events.CUBEMAP_CMD, Events.CAMERA_CINEMATIC_CMD, Events.ORIENTATION_LOCK_CMD, Events.PLANETARIUM_CMD);
    }

    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case CAMERA_CINEMATIC_CMD:

            boolean gui = (Boolean) data[1];
            if (!gui) {
                cinematic.setProgrammaticChangeEvents(false);
                cinematic.setChecked((Boolean) data[0]);
                cinematic.setProgrammaticChangeEvents(true);
            }

            break;
        case CAMERA_MODE_CMD:
            // Update camera mode selection
            CameraMode mode = (CameraMode) data[0];
            Array<CameraComboBoxBean> cModes = cameraMode.getItems();
            CameraComboBoxBean selected = null;
            for (CameraComboBoxBean ccbb : cModes) {
                if (ccbb.mode == mode) {
                    selected = ccbb;
                    break;
                }
            }
            if (selected != null) {
                cameraMode.getSelection().setProgrammaticChangeEvents(false);
                cameraMode.setSelected(selected);
                cameraMode.getSelection().setProgrammaticChangeEvents(true);
            }
            break;
        case ROTATION_SPEED_CMD:
            Boolean interf = (Boolean) data[1];
            if (!interf) {
                float value = (Float) data[0];
                fieldLock = true;
                rotateSpeed.setMappedValue(value);
                fieldLock = false;
            }
            break;
        case CAMERA_SPEED_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                float value = (Float) data[0];
                fieldLock = true;
                cameraSpeed.setMappedValue(value);
                fieldLock = false;
            }
            break;

        case TURNING_SPEED_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                float value = (Float) data[0];
                fieldLock = true;
                turnSpeed.setMappedValue(value);
                fieldLock = false;
            }
            break;
        case SPEED_LIMIT_CMD:
            interf = false;
            if (data.length > 1)
                interf = (Boolean) data[1];
            if (!interf) {
                int value = (Integer) data[0];
                cameraSpeedLimit.getSelection().setProgrammaticChangeEvents(false);
                cameraSpeedLimit.setSelectedIndex(value);
                cameraSpeedLimit.getSelection().setProgrammaticChangeEvents(true);
            }
            break;
        case ORIENTATION_LOCK_CMD:
            interf = false;
            if (data.length > 2)
                interf = (Boolean) data[2];
            if (!interf) {
                boolean lock = (Boolean) data[1];
                orientationLock.setProgrammaticChangeEvents(false);
                orientationLock.setChecked(lock);
                orientationLock.setProgrammaticChangeEvents(true);
            }
            break;
        case STEREOSCOPIC_CMD:
            if (!(boolean) data[1] && !Settings.settings.runtime.openVr) {
                button3d.setProgrammaticChangeEvents(false);
                button3d.setChecked((boolean) data[0]);
                button3d.setProgrammaticChangeEvents(true);
            }
            break;
        case FOV_CHANGE_NOTIFICATION:
            fovFlag = false;
            fieldOfView.setValue(Settings.settings.scene.camera.fov);
            fovFlag = true;
            break;
        case CUBEMAP_CMD:
            if (!(boolean) data[2] && !Settings.settings.runtime.openVr) {
                CubemapProjection proj = (CubemapProjection) data[1];
                boolean enable = (boolean) data[0];
                if (proj.isPanorama()) {
                    buttonCubemap.setProgrammaticChangeEvents(false);
                    buttonCubemap.setChecked(enable);
                    buttonCubemap.setProgrammaticChangeEvents(true);
                    fieldOfView.setDisabled(enable);
                } else if (proj.isPlanetarium()) {
                    buttonDome.setProgrammaticChangeEvents(false);
                    buttonDome.setChecked(enable);
                    buttonDome.setProgrammaticChangeEvents(true);
                    fieldOfView.setDisabled(enable);
                }

            }
            break;
        case PLANETARIUM_CMD:
            if (!(boolean) data[1] && !Settings.settings.runtime.openVr) {
                boolean enable = (boolean) data[0];
                buttonDome.setProgrammaticChangeEvents(false);
                buttonDome.setChecked(enable);
                buttonDome.setProgrammaticChangeEvents(true);
                fieldOfView.setDisabled(enable);
            }
            break;
        default:
            break;
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
