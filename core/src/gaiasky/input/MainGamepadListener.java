/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.controllers.Controller;
import gaiasky.event.Event;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;

public class MainGamepadListener extends AbstractGamepadListener {
    private static final Log logger = Logger.getLogger(MainGamepadListener.class);

    private final NaturalCamera cam;

    public MainGamepadListener(NaturalCamera cam, String mappingsFile) {
        super(mappingsFile);
        this.cam = cam;
    }

    @Override
    public boolean buttonDown(Controller controller, final int buttonCode) {
        if (active.get()) {
            super.buttonDown(controller, buttonCode);
            logger.debug("button down [inputListener/code]: " + controller.getName() + " / " + buttonCode);

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
            } else if (buttonCode == mappings.getButtonSelect()) {
                em.post(Event.SHOW_QUIT_ACTION, this);
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

            return true;
        }
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        if (active.get()) {
            super.buttonUp(controller, buttonCode);
            logger.debug("button up [inputListener/code]: " + controller.getName() + " / " + buttonCode);
            cam.setGamepadInput(true);
        }
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (active.get()) {
            super.axisMoved(controller, axisCode, value);
            logger.debug("axis moved [inputListener/code/value]: " + controller.getName() + " / " + axisCode + " / " + value);

            boolean treated = false;

            // Zero point
            value = (float) applyZeroPoint(value);

            // Apply power function to axis reading
            double val = Math.signum(value) * Math.pow(Math.abs(value), mappings.getAxisValuePower());

            if (axisCode == mappings.getAxisLstickH()) {
                double valMapped = val * mappings.getAxisLstickHSensitivity();
                if (cam.getMode().isFocus()) {
                    cam.setRoll(valMapped * 1.0e-1);
                } else {
                    // Use this for lateral movement
                    cam.setHorizontal(valMapped);
                }
                treated = true;
            } else if (axisCode == mappings.getAxisLstickV()) {
                cam.setVelocity(-val * mappings.getAxisLstickVSensitivity());
                treated = true;
            } else if (axisCode == mappings.getAxisRstickH()) {
                double valMapped = (Settings.settings.controls.gamepad.invertX ? -1.0 : 1.0) * val * mappings.getAxisRstickVSensitivity();
                if (cam.getMode().isFocus()) {
                    cam.setHorizontal(valMapped * 2.0e-2);
                } else {
                    cam.setYaw(valMapped * 1.0e-1);
                }
                treated = true;
            } else if (axisCode == mappings.getAxisRstickV()) {
                double valMapped = (Settings.settings.controls.gamepad.invertY ? 1.0 : -1.0) * val * mappings.getAxisRstickHSensitivity();
                if (cam.getMode().isFocus()) {
                    cam.setVertical(valMapped * 2.0e-2);
                } else {
                    cam.setPitch(valMapped * 1.0e-1);
                }
                treated = true;
            } else if (axisCode == mappings.getAxisRT()) {
                double valMapped = val * 1.0e-1 * mappings.getAxisRTSensitivity();
                cam.setRoll(valMapped);
                treated = true;
            } else if (axisCode == mappings.getAxisLT()) {
                double valMapped = val * 1.0e-1 * mappings.getAxisLTSensitivity();
                cam.setRoll(-valMapped);
                treated = true;
            }

            if (treated) {
                cam.setGamepadInput(true);
            }

            return treated;
        }
        return false;
    }

    @Override
    public boolean pollAxes() {
        return false;
    }

    @Override
    public boolean pollButtons() {
        return false;
    }
}
