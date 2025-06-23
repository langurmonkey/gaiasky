/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.BaseModule;
import gaiasky.script.v2.impl.InteractiveCameraModule;

/**
 * API definition for the interactive camera module, {@link InteractiveCameraModule}.
 * <p>
 * The interactive camera module contains calls and methods that modify the camera using the <i>interactive mode</i>. In this mode,
 * the camera is modified through events that mimic inputs from different devices such as the mouse or the keyboard. This mode
 * produces good results when running scripts interactively (i.e. using the REST server, or the in-app console), but those results
 * are not reproducible, as they typically are affected by external factors such as the current frame rate, the computer clock, or
 * the pre-existing camera momentum and location.
 * <p>
 * If you need reproducible results, we recommend the following:
 * <ul>
 *     <li>
 *         Use the transitions API&mdash;see {@link CameraAPI#transition(double[], double[], double[], double)}
 *         and others in the same family of calls, prefixed by "transition", with the form <code>transition[...]()</code>.
 *     </li>
 *     <li>
 *         Manipulate the internal camera state directly with {@link CameraAPI#set_position(double[])}, {@link CameraAPI#set_direction(double[])},
 *         and {@link CameraAPI#set_up(double[])}. In this case, you need to implement your camera logic in your script.
 *     </li>
 *     <li>
 *         Use camera paths (see {@link CamcorderAPI}).
 *     </li>
 *     <li>
 *         Use key-framed camera paths TODO.
 *     </li>
 * </ul>
 */
public interface InteractiveCameraAPI {

    /**
     * Enable/disable the cinematic camera mode. The cinematic camera mode makes the camera use acceleration and momentum, leading to very
     * smooth transitions and movements. This is the ideal camera to use when recording camera paths or presenting to an audience.
     *
     * @param cinematic Whether to enable or disable the cinematic mode.
     */
    void set_cinematic(boolean cinematic);

    /**
     * Run a seamless trip to the object with the name <code>focusName</code>
     * until the object view angle is <code>20 degrees</code>.
     * <p>
     * <strong>Warning:</strong> This method is not deterministic. It is implemented as a loop that sends 'camera forward' events
     * to the main thread, running in a separate thread. If you need total synchronization and reproducibility,
     * look into the {@link BaseModule#park_runnable(String, Runnable)} family of calls.</p>
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     */
    void go_to_object(String name);

    /**
     * Run a seamless trip to the object with the name <code>focusName</code>
     * until the object view angle <code>viewAngle</code> is met. If angle is
     * negative, the default angle is <code>20 degrees</code>.
     *
     * <p>Warning: This method is not deterministic. It is implemented as a loop that sends 'camera forward' events
     * to the main thread, running in a separate thread. If you need total synchronization and reproducibility,
     * look into the {@link BaseModule#park_runnable(String, Runnable)} family of calls.</p>
     *
     * @param name       The name or id (HIP, TYC, sourceId) of the object.
     * @param solidAngle The target solid angle of the object, in degrees. The angle
     *                   gets larger and larger as we approach the object.
     */
    void go_to_object(String name,
                      double solidAngle);

    /**
     * Run a seamless trip to the object with the name <code>focusName</code>
     * until the object view angle <code>viewAngle</code> is met. If angle is
     * negative, the default angle is <code>20 degrees</code>. If
     * <code>waitTimeSeconds</code> is positive, it indicates the number of
     * seconds to wait (block the function) for the camera to face the focus
     * before starting the forward movement. This very much depends on the
     * <code>turn velocity</code> of the camera. See
     * {@link #turning_speed_setting(float)}.
     *
     * <p>Warning: This method is not deterministic. It is implemented as a loop that sends 'camera forward' events
     * to the main thread, running in a separate thread. If you need total synchronization and reproducibility,
     * look into the {@link BaseModule#park_runnable(String, Runnable)} family of calls.</p>
     *
     * @param name            The name or id (HIP, TYC, sourceId) of the object.
     * @param solidAngle      The target solid angle of the object, in degrees. The angle
     *                        gets larger and larger as we approach the object.
     * @param waitTimeSeconds The seconds to wait for the camera direction vector and the
     *                        vector from the camera position to the target object to be
     *                        aligned.
     */
    void go_to_object(String name,
                      double solidAngle,
                      float waitTimeSeconds);

    /**
     * Get the current physical speed of the camera in km/h.
     *
     * @return The current speed of the camera in km/h.
     */
    double get_speed();

    /**
     * Change the speed multiplier of the camera and its acceleration. This setting affects the camera speed
     * in interactive mode, and all the functions that move the camera forward or backward in this module.
     *
     * @param speed The new speed, from 0 to 100.
     */
    void speed_setting(float speed);

    /**
     * Change the speed of the camera when it rotates around a focus. This setting affects the rotation speed
     * of the camera in interactive mode, and the function {@link #add_rotation(double, double)}.
     *
     * @param speed The new rotation speed in [0,100]
     */
    void rotation_speed_setting(float speed);

    /**
     * Change the turning speed multiplier of the camera. This setting affects the turning speed in interactive mode,
     * and the functions {@link #add_turn(double, double)}, {@link #add_pitch(double)}, {@link #add_yaw(double)}, and
     * {@link #add_roll(double)}.
     *
     * @param speed The new turning speed, from 1 to 100.
     */
    void turning_speed_setting(float speed);

    /**
     * Add a forward movement to the camera with the given value. If value is
     * negative the movement is backwards.
     * <p>
     * This gets a unitless parameter in [-1, 1] to mimic
     * the mouse scroll movement up and down.
     *
     * @param value The magnitude of the movement, between -1 and 1.
     */
    void add_forward(double value);

    /**
     * Add a rotation movement to the camera around the current focus, or a pitch/yaw if in free mode.
     * <p>
     * If the camera is not using the cinematic behaviour ({@link #set_cinematic(boolean)},
     * the rotation movement will not be permanent. Use the cinematic behaviour to have the camera
     * continue to rotate around the focus.
     * <p>
     * This method gets two unitless parameters in [0, 1], <code>deltaX</code> and <code>deltaY</code>, which
     * mimic the delta, in pixels, of the mouse cursor being dragged.
     *
     * @param deltaX The x component, between 0 and 1. Positive is right and
     *               negative is left.
     * @param deltaY The y component, between 0 and 1. Positive is up and negative
     *               is down.
     */
    void add_rotation(double deltaX,
                      double deltaY);

    /**
     * Add a roll force to the camera.
     *
     * @param roll The intensity of the roll.
     */
    void add_roll(double roll);

    /**
     * Add a turn force to the camera (yaw and/or pitch). If the camera is in focus mode, it
     * permanently deviates the line of sight from the focus until centered
     * again.
     * <p>
     * If the camera is not using the cinematic behaviour ({@link #set_cinematic(boolean)},
     * the turn will not be permanent. Use the cinematic behaviour to have the turn
     * persist in time.
     *
     * @param deltaX The x component, between 0 and 1. Positive is right and
     *               negative is left.
     * @param deltaY The y component, between 0 and 1. Positive is up and negative
     *               is down.
     */
    void add_turn(double deltaX,
                  double deltaY);

    /**
     * Add a yaw to the camera. Same as {@link #add_turn(double, double)} with
     * deltaY set to zero.
     *
     * @param amount The amount.
     */
    void add_yaw(double amount);

    /**
     * Add a pitch to the camera. Same as {@link #add_turn(double, double)} with
     * deltaX set to zero.
     *
     * @param amount The amount.
     */
    void add_pitch(double amount);

    /**
     * Land on the object with the given name, if it is a planet or moon. The land location is
     * determined by the line of sight from the current position of the camera
     * to the object.
     *
     * @param name The proper name of the object.
     */
    void land_on(String name);

    /**
     * Land on the object with the given <code>name</code>, if it is
     * a planet or moon, at the
     * location with the given name, if it exists.
     *
     * @param name         The proper name of the object.
     * @param locationName The name of the location to land on
     */
    void land_at_location(String name,
                          String locationName);

    /**
     * Land on the object with the given <code>name</code>, if it is a
     * planet or moon, at the
     * location specified in by [latitude, longitude], in degrees.
     *
     * @param name      The proper name of the object.
     * @param longitude The location longitude, in degrees.
     * @param latitude  The location latitude, in degrees.
     */
    void land_at_location(String name,
                          double longitude,
                          double latitude);

}
