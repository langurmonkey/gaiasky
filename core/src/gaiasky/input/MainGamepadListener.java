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
import net.jafama.FastMath;

import java.util.HashMap;
import java.util.Map;

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
 * <p>The axes and the RB/LB vertical movement all go through the same smoothing filter, driven by
 * the {@code controls.gamepad.smoothing} setting, so the navigation ramps in and out instead of
 * responding to every controller sample as it arrives.
 */
public class MainGamepadListener extends AbstractGamepadListener {
    private static final Log logger = Logger.getLogger(MainGamepadListener.class);

    /**
     * Time constant of the input smoothing filter at the maximum smoothing setting, in seconds.
     */
    private static final double MAX_SMOOTHING_TAU = 0.5;
    /**
     * Difference between the smoothed value and its target below which the value snaps to the
     * target.
     */
    private static final double SMOOTHING_EPSILON = 1.0e-4;
    /**
     * Reserved channel key for the button-driven vertical movement (RB and LB). Negative, so it can
     * never collide with a controller axis code. This is what lets those two buttons go through the
     * very same smoothing filter as the axes.
     */
    private static final int BUTTON_VERTICAL = -1;

    private final NaturalCamera cam;

    /** Is the vertical movement button up (RB) currently held? **/
    private boolean verticalButtonUp;
    /** Is the vertical movement button down (LB) currently held? **/
    private boolean verticalButtonDown;

    /**
     * Target value of each axis, as it comes from the controller, after the dead zone and the power
     * curve have been applied. Filled in by the axis events.
     */
    private final Map<Integer, Double> axisTargets = new HashMap<>();
    /**
     * Smoothed value of each axis, that is, the value currently handed over to the camera.
     */
    private final Map<Integer, Double> axisValues = new HashMap<>();

    public MainGamepadListener(NaturalCamera cam, String mappingsFile) {
        super(mappingsFile);
        this.cam = cam;
        this.buttonPollDelay = 20;
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
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
                // Held state only. The vertical movement itself is produced by the smoothing filter
                // in update(), which is the single place that writes to the camera.
                verticalButtonUp = true;
            } else if (buttonCode == mappings.getButtonLB()) {
                verticalButtonDown = true;
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
                verticalButtonUp = false;
            } else if (buttonCode == mappings.getButtonLB()) {
                verticalButtonDown = false;
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

            if (isMappedAxis(axisCode)) {
                // Apply zero-point correction.
                value = (float) applyZeroPoint(value);

                // Apply power function to axis reading.
                double val = FastMath.signum(value) * FastMath.pow(Math.abs(value), mappings.getAxisValuePower());

                // Do not touch the camera here, only record the new target. The smoothing filter in
                // update() is the single place that writes to the camera, so that the response is
                // frame-locked and framerate-independent.
                axisTargets.put(axisCode, val);

                cam.setGamepadInput(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Is the given axis one of the axes this listener maps to a camera action?
     *
     * @param axisCode The axis code.
     * @return True if the axis is mapped, false otherwise.
     */
    private boolean isMappedAxis(int axisCode) {
        return axisCode == mappings.getAxisLstickH()
                || axisCode == mappings.getAxisLstickV()
                || axisCode == mappings.getAxisRstickH()
                || axisCode == mappings.getAxisRstickV()
                || axisCode == mappings.getAxisRT()
                || axisCode == mappings.getAxisLT();
    }

    @Override
    public void update() {
        // Polling first, as in the base class.
        super.update();

        if (!active.get()) {
            return;
        }

        double dt = GaiaSky.instance.time.getDt();
        if (dt <= 0) {
            return;
        }

        double smoothing = GaiaSky.settings().controls.gamepad.smoothing;
        // Map the [0,1] user setting onto a time constant. At 0 the filter is a pass-through and the
        // input is applied exactly as it arrives, which is the pre-smoothing behaviour.
        double tau = Math.max(0, Math.min(1, smoothing)) * MAX_SMOOTHING_TAU;

        // The vertical movement buttons (RB/LB) are not axes, but they go through the same filter
        // under a reserved key, so that they ramp in and out instead of starting and stopping the
        // vertical movement abruptly. Only meaningful in free mode, where they translate the camera
        // along its own up vector; in any other mode the target stays at zero and the movement is
        // damped out.
        double verticalTarget = cam.getMode().isFree()
                ? (verticalButtonUp ? 1.0 : 0.0) - (verticalButtonDown ? 1.0 : 0.0)
                : 0.0;
        if (verticalTarget != 0.0 || axisTargets.containsKey(BUTTON_VERTICAL)) {
            axisTargets.put(BUTTON_VERTICAL, verticalTarget);
        }

        for (var entry : axisTargets.entrySet()) {
            int axisCode = entry.getKey();
            double target = entry.getValue();

            Double previous = axisValues.get(axisCode);
            double current = previous == null ? 0.0 : previous;

            if (tau <= 0) {
                current = target;
            } else {
                // Framerate-independent exponential approach to the target.
                current += (target - current) * (1.0 - FastMath.exp(-dt / tau));
                if (FastMath.abs(target - current) < SMOOTHING_EPSILON) {
                    current = target;
                }
            }

            // Only write to the camera when the value actually changes. A resting axis would
            // otherwise reset the camera's rotation idle timer on every single frame.
            if (previous == null || current != previous) {
                axisValues.put(axisCode, current);
                applyAxis(axisCode, current);
            }
        }
    }

    /**
     * Hands the given axis value over to the camera, applying the per-axis sensitivity and the mode
     * dependent scaling.
     *
     * @param axisCode The axis code.
     * @param val      The axis value, after the dead zone and the power curve.
     */
    private void applyAxis(int axisCode, double val) {
        if (axisCode == BUTTON_VERTICAL) {
            // Vertical movement buttons (RB/LB): translate along the camera up vector in free mode.
            cam.setVertical(val);
        } else if (axisCode == mappings.getAxisLstickH()) {
            double valMapped = val * mappings.getAxisLstickHSensitivity();
            if (cam.getMode().isFocus()) {
                cam.setRoll(valMapped * 1.0e-1);
            } else {
                // Use this for lateral movement
                cam.setHorizontal(valMapped);
            }
        } else if (axisCode == mappings.getAxisLstickV()) {
            cam.setVelocity(-val * mappings.getAxisLstickVSensitivity());
        } else if (axisCode == mappings.getAxisRstickH()) {
            double valMapped = (GaiaSky.settings().controls.gamepad.invertX ? -1.0 : 1.0) * val * mappings.getAxisRstickVSensitivity();
            if (cam.getMode().isFocus()) {
                cam.setHorizontal(valMapped * 2.0e-2);
            } else {
                cam.setYaw(valMapped * 1.0e-1);
            }
        } else if (axisCode == mappings.getAxisRstickV()) {
            double valMapped = (GaiaSky.settings().controls.gamepad.invertY ? 1.0 : -1.0) * val * mappings.getAxisRstickHSensitivity();
            if (cam.getMode().isFocus()) {
                cam.setVertical(valMapped * 2.0e-2);
            } else {
                cam.setPitch(valMapped * 1.0e-1);
            }
        } else if (axisCode == mappings.getAxisRT()) {
            cam.setRoll(val * 1.0e-1 * mappings.getAxisRTSensitivity());
        } else if (axisCode == mappings.getAxisLT()) {
            cam.setRoll(-val * 1.0e-1 * mappings.getAxisLTSensitivity());
        }
    }

    @Override
    public void activate() {
        // Do not apply stale values from a previous activation.
        resetState();
        super.activate();
    }

    @Override
    public void deactivate() {
        resetState();
        super.deactivate();
    }

    private void resetState() {
        // If the vertical button channel was in use, the camera may be left climbing on its own
        // (a release event lost, or the listener going away with the button still held). Stop it.
        if (axisTargets.containsKey(BUTTON_VERTICAL) || axisValues.containsKey(BUTTON_VERTICAL)) {
            cam.setVertical(0.0);
        }
        verticalButtonUp = false;
        verticalButtonDown = false;
        axisTargets.clear();
        axisValues.clear();
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
                double inc = GaiaSky.settings().scene.camera.cinematic ? 0.01 : 0.05;
                EventManager.instance.post(Event.TIME_WARP_CMD, this, t < 0 ? t + FastMath.abs(t * inc) : t + t * inc);
            } else if (lastControllerUsed.getButton(mappings.getButtonDpadDown())) {
                var t = GaiaSky.instance.time.getWarpFactor();
                // Time slow down.
                if (t == 0) {
                    t = -0.1;
                } else if (t < 0.1 && t > 0) {
                    t = 0;
                }
                double inc = GaiaSky.settings().scene.camera.cinematic ? 0.01 : 0.05;
                EventManager.instance.post(Event.TIME_WARP_CMD, this, t < 0 ? t - FastMath.abs(t * inc) : t - t * inc);
            }
            return true;
        }
        return false;
    }
}