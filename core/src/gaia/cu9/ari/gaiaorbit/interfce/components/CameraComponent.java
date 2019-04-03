package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.TextUtils;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;

import java.util.ArrayList;
import java.util.List;

public class CameraComponent extends GuiComponent implements IObserver {

    protected OwnLabel fov, speed, turn, rotate, date;
    protected SelectBox<String> cameraMode, cameraSpeedLimit;
    protected Slider fieldOfView, cameraSpeed, turnSpeed, rotateSpeed;
    protected CheckBox focusLock, orientationLock, crosshair, cinematic;
    protected OwnTextIconButton button3d, buttonDome, buttonCubemap, buttonAnaglyph, button3dtv, buttonVR, buttonCrosseye;
    protected boolean fovFlag = true;
    private boolean fieldLock = false;

    public CameraComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.CAMERA_MODE_CMD, Events.ROTATION_SPEED_CMD, Events.TURNING_SPEED_CMD, Events.CAMERA_SPEED_CMD, Events.SPEED_LIMIT_CMD, Events.STEREOSCOPIC_CMD, Events.FOV_CHANGE_NOTIFICATION, Events.CUBEMAP360_CMD, Events.CAMERA_CINEMATIC_CMD, Events.ORIENTATION_LOCK_CMD, Events.PLANETARIUM_CMD);
    }

    @Override
    public void initialize() {
        float pad = 5 * GlobalConf.SCALE_FACTOR;
        float space3 = 3 * GlobalConf.SCALE_FACTOR;
        float space2 = 2 * GlobalConf.SCALE_FACTOR;
        float width = 140 * GlobalConf.SCALE_FACTOR;

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
        String[] cameraOptions = new String[cameraModes];
        for (int i = 0; i < cameraModes; i++) {
            cameraOptions[i] = CameraMode.getMode(i).toString();
        }
        cameraMode = new OwnSelectBox<>(skin);
        cameraMode.setName("camera mode");
        cameraMode.setWidth(width);
        cameraMode.setItems(cameraOptions);
        cameraMode.addListener(event -> {
            if (event instanceof ChangeEvent) {
                String selection = cameraMode.getSelected();
                CameraMode mode;
                try {
                    mode = CameraMode.fromString(selection);
                } catch (IllegalArgumentException e) {
                    // Foucs to one of our models
                    mode = CameraMode.Focus;
                    EventManager.instance.post(Events.FOCUS_CHANGE_CMD, selection, true);
                }

                EventManager.instance.post(Events.CAMERA_MODE_CMD, mode);
                return true;
            }
            return false;
        });

        List<Button> buttonList = new ArrayList<>();

        button3d = new OwnTextIconButton("", skin, "3d");
        button3d.addListener(new TextTooltip(TextUtils.capitalise(I18n.txt("element.stereomode")), skin));
        button3d.setName("3d");
        button3d.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.STEREOSCOPIC_CMD, button3d.isChecked(), true);
                return true;
            }
            return false;
        });

        buttonDome = new OwnTextIconButton("", skin, "dome");
        buttonDome.addListener(new TextTooltip(TextUtils.capitalise(I18n.txt("element.planetarium")), skin));
        buttonDome.setName("dome");
        buttonDome.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Enable
                EventManager.instance.post(Events.PLANETARIUM_CMD, buttonDome.isChecked(), true);
                return true;
            }
            return false;
        });

        buttonCubemap = new OwnTextIconButton("", skin, "cubemap");
        buttonCubemap.setProgrammaticChangeEvents(false);
        buttonCubemap.addListener(new TextTooltip(TextUtils.capitalise(I18n.txt("element.360")), skin));
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
        fieldOfView = new OwnSlider(Constants.MIN_FOV, Constants.MAX_FOV, 1, false, skin);
        fieldOfView.setName("field of view");
        fieldOfView.setWidth(width);
        fieldOfView.setValue(GlobalConf.scene.CAMERA_FOV);
        fieldOfView.addListener(event -> {
            if (fovFlag && event instanceof ChangeEvent) {
                float value = MathUtilsd.clamp(fieldOfView.getValue(), Constants.MIN_FOV, Constants.MAX_FOV);
                EventManager.instance.post(Events.FOV_CHANGED_CMD, value);
                fov.setText(Integer.toString((int) value) + "°");
                return true;
            }
            return false;
        });

        fov = new OwnLabel(Integer.toString((int) GlobalConf.scene.CAMERA_FOV) + "°", skin, "default");

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

        cameraSpeedLimit = new OwnSelectBox<String>(skin);
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
        cameraSpeed = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER / 2, 1, false, skin);
        cameraSpeed.setName("camera speed");
        cameraSpeed.setWidth(width);
        cameraSpeed.setValue((float) (GlobalConf.scene.CAMERA_SPEED * Constants.CAMERA_SPEED_FACTOR));
        cameraSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.CAMERA_SPEED_CMD, cameraSpeed.getValue() / Constants.CAMERA_SPEED_FACTOR, true);
                speed.setText(Integer.toString((int) cameraSpeed.getValue()));
                return true;
            }
            return false;
        });

        speed = new OwnLabel(Integer.toString((int) (GlobalConf.scene.CAMERA_SPEED * Constants.CAMERA_SPEED_FACTOR)), skin, "default");

        /** ROTATION SPEED **/
        rotateSpeed = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, false, skin);
        rotateSpeed.setName("rotate speed");
        rotateSpeed.setWidth(width);
        rotateSpeed.setValue((float) MathUtilsd.lint(GlobalConf.scene.ROTATION_SPEED, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        rotateSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.ROTATION_SPEED_CMD, (float) MathUtilsd.lint(rotateSpeed.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED), true);
                rotate.setText(Integer.toString((int) rotateSpeed.getValue()));
                return true;
            }
            return false;
        });

        rotate = new OwnLabel(Integer.toString((int) MathUtilsd.lint(GlobalConf.scene.ROTATION_SPEED, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED, Constants.MIN_SLIDER, Constants.MAX_SLIDER)), skin, "default");

        /** TURNING SPEED **/
        turnSpeed = new OwnSlider(Constants.MIN_SLIDER, Constants.MAX_SLIDER, 1, false, skin);
        turnSpeed.setName("turn speed");
        turnSpeed.setWidth(width);
        turnSpeed.setValue((float) MathUtilsd.lint(GlobalConf.scene.TURNING_SPEED, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED, Constants.MIN_SLIDER, Constants.MAX_SLIDER));
        turnSpeed.addListener(event -> {
            if (!fieldLock && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.TURNING_SPEED_CMD, MathUtilsd.lint(turnSpeed.getValue(), Constants.MIN_SLIDER, Constants.MAX_SLIDER, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED), true);
                turn.setText(Integer.toString((int) turnSpeed.getValue()));
                return true;
            }
            return false;
        });

        turn = new OwnLabel(Integer.toString((int) MathUtilsd.lint(GlobalConf.scene.TURNING_SPEED, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED, Constants.MIN_SLIDER, Constants.MAX_SLIDER)), skin, "default");

        /** Focus lock **/
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

        /** Focus orientation lock **/
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

        /** Crosshair **/
        crosshair = new OwnCheckBox("" + I18n.txt("gui.camera.crosshair"), skin, pad);
        crosshair.setName("orientation lock");
        crosshair.setChecked(GlobalConf.scene.CROSSHAIR);
        crosshair.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.CROSSHAIR_CMD, crosshair.isChecked());
                return true;
            }
            return false;
        });

        VerticalGroup cameraGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        cameraGroup.space(space2);

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

        cameraGroup.addActor(modeLabel);
        cameraGroup.addActor(cameraMode);
        cameraGroup.addActor(new Label(I18n.txt("gui.camera.speedlimit"), skin, "default"));
        cameraGroup.addActor(cameraSpeedLimit);
        cameraGroup.addActor(fovLabel);
        cameraGroup.addActor(fovGroup);
        cameraGroup.addActor(new Label(I18n.txt("gui.camera.speed"), skin, "default"));
        cameraGroup.addActor(speedGroup);
        cameraGroup.addActor(new Label(I18n.txt("gui.rotation.speed"), skin, "default"));
        cameraGroup.addActor(rotateGroup);
        cameraGroup.addActor(new Label(I18n.txt("gui.turn.speed"), skin, "default"));
        cameraGroup.addActor(turnGroup);
        cameraGroup.addActor(cinematic);
        cameraGroup.addActor(focusLock);
        cameraGroup.addActor(orientationLock);
        cameraGroup.addActor(crosshair);
        cameraGroup.addActor(new Label("", skin));
        cameraGroup.addActor(buttonGroup);

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
            cameraMode.getSelection().setProgrammaticChangeEvents(false);
            cameraMode.setSelected(mode.toString());
            cameraMode.getSelection().setProgrammaticChangeEvents(true);
            break;
        case ROTATION_SPEED_CMD:
            Boolean interf = (Boolean) data[1];
            if (!interf) {
                float value = (Float) data[0];
                value = MathUtilsd.lint(value, Constants.MIN_ROT_SPEED, Constants.MAX_ROT_SPEED, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
                fieldLock = true;
                rotateSpeed.setValue(value);
                fieldLock = false;
                rotate.setText(Integer.toString((int) value));
            }
            break;
        case CAMERA_SPEED_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                float value = (Float) data[0];
                value *= Constants.CAMERA_SPEED_FACTOR;
                fieldLock = true;
                cameraSpeed.setValue(value);
                fieldLock = false;
                speed.setText(Integer.toString((int) value));
            }
            break;

        case TURNING_SPEED_CMD:
            interf = (Boolean) data[1];
            if (!interf) {
                float value = (Float) data[0];
                value = MathUtilsd.lint(value, Constants.MIN_TURN_SPEED, Constants.MAX_TURN_SPEED, Constants.MIN_SLIDER, Constants.MAX_SLIDER);
                fieldLock = true;
                turnSpeed.setValue(value);
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
            fov.setText(Integer.toString((int) GlobalConf.scene.CAMERA_FOV) + "°");
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
