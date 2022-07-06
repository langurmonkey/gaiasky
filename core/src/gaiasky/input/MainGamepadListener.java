/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.controllers.Controller;
import gaiasky.event.Event;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;

/**
 * Gamepad input listener for the natural camera.
 */
public class MainGamepadListener extends AbstractGamepadListener {
    private static final Log logger = Logger.getLogger(MainGamepadListener.class);

    private final NaturalCamera cam;


    public MainGamepadListener(NaturalCamera cam, String mappingsFile) {
        super(mappingsFile);
        this.cam = cam;
    }


    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        logger.debug("button down [inputListener/code]: " + controller.getName() + " / " + buttonCode);

        cam.setGamepadInput(true);

        addPressedKey(buttonCode);

        return true;
    }

    @Override
    public boolean buttonUp(Controller controller, final int buttonCode) {
        logger.debug("button up [inputListener/code]: " + controller.getName() + " / " + buttonCode);

        if (buttonCode == mappings.getButtonX()) {
            em.post(Event.TOGGLE_MINIMAP, this);
        } else if (buttonCode == mappings.getButtonY()) {
            em.post(Event.TOGGLE_VISIBILITY_CMD, this, "element.orbits");
        } else if (buttonCode == mappings.getButtonA()) {
            em.post(Event.TOGGLE_VISIBILITY_CMD, this, "element.labels");
        } else if (buttonCode == mappings.getButtonB()) {
            em.post(Event.TOGGLE_VISIBILITY_CMD, this, "element.asteroids");
        } else if (buttonCode == mappings.getButtonDpadUp()) {
            em.post(Event.STAR_POINT_SIZE_INCREASE_CMD, this);
        } else if (buttonCode == mappings.getButtonDpadDown()) {
            em.post(Event.STAR_POINT_SIZE_DECREASE_CMD, this);
        } else if (buttonCode == mappings.getButtonDpadLeft()) {
            em.post(Event.TIME_STATE_CMD, this, false);
        } else if (buttonCode == mappings.getButtonDpadRight()) {
            em.post(Event.TIME_STATE_CMD, this, true);
        } else if (buttonCode == mappings.getButtonStart()) {
            em.post(Event.SHOW_CONTROLLER_GUI_ACTION, this, cam);
        } else if (buttonCode == mappings.getButtonRstick()) {
            if (cam.getMode().isFocus()) {
                // Set free
                em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FREE_MODE);
            } else {
                // Set focus
                em.post(Event.CAMERA_MODE_CMD, this, CameraManager.CameraMode.FOCUS_MODE);
            }
        }
        cam.setGamepadInput(true);

        removePressedKey(buttonCode);

        return true;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (Math.abs(value) > 0.1)
            logger.debug("axis moved [inputListener/code/value]: " + controller.getName() + " / " + axisCode + " / " + value);

        boolean treated = false;

        // Apply power function to axis reading
        double val = Math.signum(value) * Math.pow(Math.abs(value), mappings.getAxisValuePower());

        if (axisCode == mappings.getAxisLstickH()) {
            if (cam.getMode().isFocus()) {
                cam.setRoll(val * 1e-2 * mappings.getAxisLstickHSensitivity());
            } else {
                // Use this for lateral movement
                cam.setHorizontal(val * mappings.getAxisLstickHSensitivity());
            }
            treated = true;
        } else if (axisCode == mappings.getAxisLstickV()) {
            if (Math.abs(val) < 0.005)
                val = 0;
            cam.setVelocity(-val * mappings.getAxisLstickVSensitivity());
            treated = true;
        } else if (axisCode == mappings.getAxisRstickH()) {
            double valr = (Settings.settings.controls.gamepad.invertX ? -1.0 : 1.0) * val * mappings.getAxisRstickVSensitivity();
            if (cam.getMode().isFocus()) {
                cam.setHorizontal(valr * 0.1);
            } else {
                cam.setYaw(valr * 3e-2);
            }
            treated = true;
        } else if (axisCode == mappings.getAxisRstickV()) {
            double valr = (Settings.settings.controls.gamepad.invertY ? 1.0 : -1.0) * val * mappings.getAxisRstickHSensitivity();
            if (cam.getMode().isFocus()) {
                cam.setVertical(valr * 0.1);
            } else {
                cam.setPitch(valr * 3e-2);
            }
            treated = true;
        } else if (axisCode == mappings.getAxisRT()) {
            double valr = val * 1e-1 * mappings.getAxisRTSensitivity();
            cam.setRoll(valr);
            treated = true;
        } else if (axisCode == mappings.getAxisLT()) {
            double valr = val * 1e-1 * mappings.getAxisLTSensitivity();
            cam.setRoll(-valr);
            treated = true;
        }

        if (treated)
            cam.setGamepadInput(true);

        return treated;
    }

}
