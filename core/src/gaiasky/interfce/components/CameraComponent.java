/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.interfce.KeyBindings;
import gaiasky.interfce.beans.CameraComboBoxBean;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.TextUtils;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.scene2d.*;

import java.util.ArrayList;
import java.util.List;

public class CameraComponent extends GuiComponent implements IObserver {

    protected OwnLabel fov, speed, turn, rotate, date;
    protected SelectBox<String> cameraSpeedLimit;
    protected SelectBox<CameraComboBoxBean> cameraMode;
    protected OwnSlider fieldOfView, cameraSpeed, turnSpeed, rotateSpeed;
    protected CheckBox focusLock, orientationLock, cinematic;
    protected OwnTextIconButton button3d, buttonDome, buttonCubemap, buttonAnaglyph, button3dtv, buttonVR, buttonCrosseye;
    protected boolean fovFlag = true;
    private boolean fieldLock = false;

    public CameraComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.CAMERA_MODE_CMD, Events.ROTATION_SPEED_CMD, Events.TURNING_SPEED_CMD, Events.CAMERA_SPEED_CMD, Events.SPEED_LIMIT_CMD, Events.STEREOSCOPIC_CMD, Events.FOV_CHANGE_NOTIFICATION, Events.CUBEMAP360_CMD, Events.CAMERA_CINEMATIC_CMD, Events.ORIENTATION_LOCK_CMD, Events.PLANETARIUM_CMD);
    }

    @Override
    public void initialize() {
        float width = 140 * GlobalConf.UI_SCALE_FACTOR;

        cinematic = new OwnCheckBox(I18n.txt("gui.camera.cinematic"), skin, pad);
        cinematic.setName("cinematic camera");
        cinematic.setChecked(GlobalConf.scene.CINEMATIC_CAMERA);
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
            cameraOptions[i] = new CameraComboBoxBean(CameraMode.getMode(i).toStringI18n(), CameraMode.getMode(i));
        }
        cameraMode = new OwnSelectBox<>(skin);
        cameraMode.setName("camera mode");
        cameraMode.setWidth(width);
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

        List<Button> buttonList = new ArrayList<>();

        Image icon3d = new Image(skin.getDrawable("3d-icon"));
        button3d = new OwnTextIconButton("", icon3d, skin, "toggle");
        String hk3d = KeyBindings.instance.getStringKeys("action.toggle/element.stereomode");
        button3d.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(I18n.txt("element.stereomode")), hk3d, skin));
        button3d.setName("3d");
        button3d.addListener(event -> {
            if (event instanceof ChangeEvent) {
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
                // Enable
                EventManager.instance.post(Events.PLANETARIUM_CMD, buttonDome.isChecked(), true);
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
                EventManager.instance.post(Events.CUBEMAP360_CMD, buttonCubemap.isChecked(), true);
                return true;
            }
            return false;
        });

        buttonList.add(button3d);
        buttonList.add(buttonDome);
        buttonList.add(buttonCubemap);

        Label fovLabel = new Label(I18n.txt("gui.camera.fov"), skin, "default");
        fieldOfView = new OwnSlider(Constants.MIN_FOV, Constants.MAX_FOV, Constants.SLIDER_STEP, false, skin);
        fieldOfView.setName("field of view");
        fieldOfView.setWidth(width);
        fieldOfView.setValue(GlobalConf.scene.CAMERA_FOV);
        fieldOfView.addListener(event -> {
            if (fovFlag && event instanceof ChangeEvent) {
                float value = MathUtilsd.clamp(fieldOfView.getValue(), Constants.MIN_FOV, Constants.MAX_FOV);
                EventManager.instance.post(Events.FOV_CHANGED_CMD, value);
                fov.setText((int) value + "°");
                return true;
            }
            return false;
        });

        fov = new OwnLabel((int) GlobalConf.scene.CAMERA_FOV + "°", skin, "default");

        /** CAMERA SPEED LIMIT **/
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
        cameraSpeedLimit.setWidth(width);
        cameraSpeedLimit.setItems(speedLimits);
        cameraSpeedLimit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                int idx = cameraSpeedLimit.getSelectedIndex();
                EventManager.instance.post(Events.SPEED_LIMIT_CMD, idx, true);
                return true;
            }
            return false;
        });
        cameraSpeedLimit.setSelectedIndex(GlobalConf.scene.CAMERA_SPEED_LIMIT_IDX);

        /** CAMERA SPEED **/
        cameraSpeed = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_CAM_SPEED, Constants.MAX_CAM_SPEED, skin);
        cameraSpeed.setName("camera speed");
        cameraSpeed.setWidth(width);
        cameraSpeed.setMappedValue(GlobalConf.scene.CAMERA_SPEED);
        cameraSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.CAMERA_SPEED_CMD, cameraSpeed.getMappedValue(), true);
                speed.setText(Integer.toString((int) cameraSpeed.getValue()));
                return true;
            }
            return false;
        });

        speed = new OwnLabel(Integer.toString((int) cameraSpeed.getValue()), skin, "default");

        /** ROTATION SPEED **/
        rotateSpeed = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED, skin);
        rotateSpeed.setName("rotate speed");
        rotateSpeed.setWidth(width);
        rotateSpeed.setMappedValue(GlobalConf.scene.ROTATION_SPEED);
        rotateSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.ROTATION_SPEED_CMD, rotateSpeed.getMappedValue(), true);
                rotate.setText(Integer.toString((int) rotateSpeed.getValue()));
                return true;
            }
            return false;
        });

        rotate = new OwnLabel(Integer.toString((int) MathUtilsd.lint(GlobalConf.scene.ROTATION_SPEED, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED, Constants.MIN_SLIDER, Constants.MAX_SLIDER)), skin, "default");

        /** TURNING SPEED **/
        turnSpeed = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.SLIDER_STEP, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED, skin);
        turnSpeed.setName("turn speed");
        turnSpeed.setWidth(width);
        turnSpeed.setMappedValue(GlobalConf.scene.TURNING_SPEED);
        turnSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.TURNING_SPEED_CMD, turnSpeed.getMappedValue(), true);
                turn.setText(Integer.toString((int) turnSpeed.getValue()));
                return true;
            }
            return false;
        });

        turn = new OwnLabel(Integer.toString((int) MathUtilsd.lint(GlobalConf.scene.TURNING_SPEED, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED, Constants.MIN_SLIDER, Constants.MAX_SLIDER)), skin, "default");

        /** FOCUS_MODE lock **/
        focusLock = new CheckBox(" " + I18n.txt("gui.camera.lock"), skin);
        focusLock.setName("focus lock");
        focusLock.setChecked(GlobalConf.scene.FOCUS_LOCK);
        focusLock.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.FOCUS_LOCK_CMD, I18n.txt("gui.camera.lock"), focusLock.isChecked());
                orientationLock.setVisible(focusLock.isChecked());
                return true;
            }
            return false;
        });

        /** FOCUS_MODE orientation lock **/
        orientationLock = new CheckBox(" " + I18n.txt("gui.camera.lock.orientation"), skin);
        orientationLock.setName("orientation lock");
        orientationLock.setChecked(GlobalConf.scene.FOCUS_LOCK_ORIENTATION);
        orientationLock.setVisible(GlobalConf.scene.FOCUS_LOCK);
        orientationLock.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.ORIENTATION_LOCK_CMD, I18n.txt("gui.camera.lock.orientation"), orientationLock.isChecked(), true);
                return true;
            }
            return false;
        });



        HorizontalGroup buttonGroup = new HorizontalGroup();
        buttonGroup.space(space3);
        buttonGroup.addActor(button3d);
        buttonGroup.addActor(buttonDome);
        buttonGroup.addActor(buttonCubemap);

        HorizontalGroup fovGroup = new HorizontalGroup();
        fovGroup.space(space3);
        fovGroup.addActor(fieldOfView);
        fovGroup.addActor(fov);

        HorizontalGroup speedGroup = new HorizontalGroup();
        speedGroup.space(space3);
        speedGroup.addActor(cameraSpeed);
        speedGroup.addActor(speed);

        HorizontalGroup rotateGroup = new HorizontalGroup();
        rotateGroup.space(space3);
        rotateGroup.addActor(rotateSpeed);
        rotateGroup.addActor(rotate);

        HorizontalGroup turnGroup = new HorizontalGroup();
        turnGroup.space(space3);
        turnGroup.addActor(turnSpeed);
        turnGroup.addActor(turn);

        VerticalGroup cameraGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        cameraGroup.space(space4);

        cameraGroup.addActor(vgroup(modeLabel, cameraMode, space2));
        cameraGroup.addActor(vgroup(new Label(I18n.txt("gui.camera.speedlimit"), skin, "default"), cameraSpeedLimit, space2));
        cameraGroup.addActor(vgroup(fovLabel, fovGroup, space2));
        cameraGroup.addActor(vgroup(new Label(I18n.txt("gui.camera.speed"), skin, "default"), speedGroup, space2));
        cameraGroup.addActor(vgroup(new Label(I18n.txt("gui.rotation.speed"), skin, "default"), rotateGroup, space2));
        cameraGroup.addActor(vgroup(new Label(I18n.txt("gui.turn.speed"), skin, "default"), turnGroup, space2));
        cameraGroup.addActor(cinematic);
        cameraGroup.addActor(focusLock);
        cameraGroup.addActor(orientationLock);
        cameraGroup.addActor(vgroup(new Label("", skin), buttonGroup, space2));

        component = cameraGroup;

        cameraGroup.pack();
    }

    @Override
    public void notify(Events event, Object... data) {
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
            for(CameraComboBoxBean ccbb : cModes){
                if(ccbb.mode == mode){
                    selected = ccbb;
                    break;
                }
            }
            if(selected != null) {
                cameraMode.getSelection().setProgrammaticChangeEvents(false);
                cameraMode.setSelected(selected);
                cameraMode.getSelection().setProgrammaticChangeEvents(true);
            } else {
                // Error?
            }
            break;
        case ROTATION_SPEED_CMD:
            Boolean interf = (Boolean) data[1];
            if (!interf) {
                float value = (Float) data[0];
                fieldLock = true;
                rotateSpeed.setMappedValue(value);
                fieldLock = false;
                rotate.setText(Integer.toString((int) value));
            }
            break;
        case CAMERA_SPEED_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                float value = (Float) data[0];
                fieldLock = true;
                cameraSpeed.setMappedValue(value);
                fieldLock = false;
                speed.setText(Integer.toString((int) value));
            }
            break;

        case TURNING_SPEED_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                float value = (Float) data[0];
                fieldLock = true;
                turnSpeed.setMappedValue(value);
                fieldLock = false;
                turn.setText(Integer.toString((int) value));
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
            if (!(boolean) data[1]) {
                button3d.setProgrammaticChangeEvents(false);
                button3d.setChecked((boolean) data[0]);
                button3d.setProgrammaticChangeEvents(true);
            }
            break;
        case FOV_CHANGE_NOTIFICATION:
            fovFlag = false;
            fieldOfView.setValue(GlobalConf.scene.CAMERA_FOV);
            fov.setText(GlobalConf.scene.CAMERA_FOV + "°");
            fovFlag = true;
            break;
        case CUBEMAP360_CMD:
            if (!(boolean) data[1]) {
                buttonCubemap.setProgrammaticChangeEvents(false);
                buttonCubemap.setChecked((boolean) data[0]);
                buttonCubemap.setProgrammaticChangeEvents(true);
            }
            break;
        case PLANETARIUM_CMD:
            if (!(boolean) data[1]) {
                buttonDome.setProgrammaticChangeEvents(false);
                buttonDome.setChecked((boolean) data[0]);
                buttonDome.setProgrammaticChangeEvents(true);
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
