/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.controllers.Controller;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.NaturalCamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import net.jafama.FastMath;

/**
 * Implements the gamepad/game controller listener in default mode.
 * <p>The default mappings go like this:
 * <ul>
 *     <li>X - visibility of labels</li>
 *     <li>Y - visibility of orbits</li>
 *     <li>A - hold to speed up camera</li>
 *     <li>B - visibility of asteroids</li>
 *     <li>D-pad right - start time</li>
 *     <li>D-pad left - stop time</li>
 *     <li>Start - show preferences (gamepad menu)</li>
 *     <li>Select - show quit window</li>
 *     <li>RB - move vertically up</li>
 *     <li>LB - move vertically down</li>
 *     <li>Axis R - movement</li>
 *     <li>Axis L - rotate view (free mode), rotate around object (focus mode)</li>
 *     <li>ZR - roll right</li>
 *     <li>ZL - roll left</li>
 * </ul>
 */
public class MainGamepadListener extends AbstractGamepadListener {
    private static final Log logger = Logger.getLogger(MainGamepadListener.class);

    private final NaturalCamera cam;

    public MainGamepadListener(NaturalCamera cam, String mappingsFile) {
        super(mappingsFile);
        this.cam = cam;
        this.buttonPollDelay = 20;
    }

    @Override
    public boolean buttonDown(Controller controller, final int buttonCode) {
        if (active.get()) {
            super.buttonDown(controller, buttonCode);
            logger.debug("button down [inputListener/code]: " + controller.getName() + " / " + buttonCode);

            if (buttonCode == mappings.getButtonX()) {
                em.post(Event.TOGGLE_VISIBILITY_CMD, this, "element.labels");
            } else if (buttonCode == mappings.getButtonY()) {
                em.post(Event.TOGGLE_VISIBILITY_CMD, this, "element.orbits");
            } else if (buttonCode == mappings.getButtonA()) {
                cam.setCameraMultipliers(6, 5);
            } else if (buttonCode == mappings.getButtonB()) {
                em.post(Event.TOGGLE_VISIBILITY_CMD, this, "element.asteroids");
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
            } else if (buttonCode == mappings.getButtonRB()) {
                if (cam.getMode().isFree()) {
                    cam.setVertical(1.0);
                }
            } else if (buttonCode == mappings.getButtonLB()) {
                if (cam.getMode().isFree()) {
                    cam.setVertical(-1.0);
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

            if (buttonCode == mappings.getButtonRB()) {
                if (cam.getMode().isFree()) {
                    cam.setVertical(0.0);
                }
            } else if (buttonCode == mappings.getButtonLB()) {
                if (cam.getMode().isFree()) {
                    cam.setVertical(0.0);
                }
            } else if (buttonCode == mappings.getButtonA()) {
                cam.setCameraMultipliers(1, 1);
            }

            cam.setGamepadInput(true);
            return true;
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
            double val = FastMath.signum(value) * FastMath.pow(Math.abs(value), mappings.getAxisValuePower());

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
        if (active.get() && lastControllerUsed != null) {
            if (lastControllerUsed.getButton(mappings.getButtonDpadUp())) {
                var t = GaiaSky.instance.time.getWarpFactor();
                // Time speed up.
                if (t == 0) {
                    t = 0.1;
                } else if (t > -0.1 && t < 0) {
                    t = 0;
                }
                double inc = Settings.settings.scene.camera.cinematic ? 0.01 : 0.05;
                EventManager.instance.post(Event.TIME_WARP_CMD, this, t < 0 ? t + FastMath.abs(t * inc) : t + t * inc);
            } else if (lastControllerUsed.getButton(mappings.getButtonDpadDown())) {
                var t = GaiaSky.instance.time.getWarpFactor();
                // Time slow down.
                if (t == 0) {
                    t = -0.1;
                } else if (t < 0.1 && t > 0) {
                    t = 0;
                }
                double inc = Settings.settings.scene.camera.cinematic ? 0.01 : 0.05;
                EventManager.instance.post(Event.TIME_WARP_CMD, this, t < 0 ? t - FastMath.abs(t * inc) : t - t * inc);
            }
            return true;
        }
        return false;
    }
}