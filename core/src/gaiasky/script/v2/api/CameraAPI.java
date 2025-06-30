/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.scene.api.IFocus;
import gaiasky.script.v2.impl.CameraModule;

/**
 * Public API definition for the camera module, {@link CameraModule}.
 * <p>
 * The camera module contains methods and calls that modify and query the camera.
 */
public interface CameraAPI {

    /**
     * Set the camera in focus mode with the focus object identified by the given
     * <code>focusName</code>. It returns immediately, i.e., it does not wait for
     * the camera direction to finish the transition that makes it point to the new focus object.
     *
     * @param name The name of the new focus object.
     */
    void focus_mode(final String name);

    /**
     * Set the camera in focus mode with the focus object identified by the given
     * <code>focusName</code>. Additionally, <code>waitTimeSeconds</code> contains the amount of time, in seconds, to wait for the camera
     * transition that makes it point to the new focus object to finish. If the transition has not finished after this amount of time, the call
     * returns. If the transition finishes before this amount of time, it returns immediately after finishing.
     *
     * @param name The name of the new focus object.
     * @param wait Maximum time, in seconds, to wait for the camera to face the
     *             focus. If negative, the call waits until the camera transition is finished.
     */
    void focus_mode(final String name, final float wait);

    /**
     * Set the camera in focus mode with the focus object identified by the given <code>focusName</code>.
     * This call is different from {@link #focus_mode(String)} in that the camera direction vector is set to point towards the focus object
     * instantly.
     *
     * @param name The name of the new focus object.
     */
    void focus_mode_instant(final String name);

    /**
     * Set the camera in focus mode with the focus object identified by the given <code>focusName</code>.
     * This call is different from {@link #focus_mode(String)} in that the camera is moved to the vicinity of the focus object instantly, and its
     * direction vector is set to point towards the focus object, also instantly.
     *
     * @param name The name of the new focus object.
     */
    void focus_mode_instant_go(final String name);

    /**
     * This method blocks until the focus is the object indicated by the name.
     * There is an optional timeout time, given in milliseconds. If the focus has not been acquired after this timeout,
     * the call returns.
     *
     * @param name    The name of the focus to wait for.
     * @param timeout Timeout to wait, in milliseconds. Set negative to use no timeout.
     *
     * @return True if the timeout triggered the return. False otherwise.
     */
    boolean wait_focus(String name,
                       long timeout);

    /**
     * Activates or deactivates the camera lock to the focus reference system
     * when in focus mode.
     *
     * @param lock Activate or deactivate the lock.
     */
    void set_focus_lock(boolean lock);

    /**
     * Set the center focus property of the camera. If set to <code>true</code>,
     * the camera centers the focus in view. If set to <code>false</code>,
     * the camera does not seek to center the focus object, leaving the camera in a 'free view' state.
     * <p>
     * Note that the camera is in focus mode, so its movement is determined by the focus object.
     * 'Free view' state and free mode are not the same thing.
     * Use {@link #free_mode()} to set the camera in free mode.
     *
     * @param center Whether to center the focus or not.
     */
    void center_focus(boolean center);

    /**
     * Lock or unlock the orientation of the camera to the focus object's
     * rotation.
     *
     * @param lock Whether to lock or unlock the camera orientation to the focus.
     */
    void set_orientation_lock(boolean lock);

    /**
     * Set the camera in free mode.
     */
    void free_mode();

    /**
     * Set the camera position to the given coordinates, in the internal reference system and kilometres.
     * The default behavior of this method posts a runnable to update the
     * camera after the current frame. If you need to call this method from
     * within a parked runnable, use {@link #set_position(double[], boolean)},
     * with the boolean set to <code>true</code>.
     *
     * @param pos Vector of three components in internal coordinates and Km.
     */
    void set_position(double[] pos);

    /**
     * Set the camera position to the given coordinates, in the internal reference system and kilometres.
     * The <code>immediate</code> parameter enables setting the camera state
     * immediately without waiting for the possible current update
     * operation to finish. Set this to true if you run this function
     * from within a parked runnable.
     *
     * @param pos       Vector of three components in internal coordinates and Km.
     * @param immediate Whether to apply the changes immediately, or wait for the next frame.
     */
    void set_position(double[] pos,
                      boolean immediate);


    /**
     * Set the camera position to the given coordinates, in the internal reference system and in the requested units.
     * The <code>immediate</code> parameter enables setting the camera state
     * immediately without waiting for the possible current update
     * operation to finish. Set this to true if you run this function
     * from within a parked runnable.
     *
     * @param pos       Vector of three components in internal coordinates and the requested units.
     * @param units     The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     * @param immediate Whether to apply the changes immediately, or wait for the next frame.
     */
    void set_position(double[] pos,
                      String units,
                      boolean immediate);

    /**
     * Component-wise version of {@link #set_position(double[])}.
     */
    void set_position(double x,
                      double y,
                      double z);

    /**
     * Component-wise version of {@link #set_position(double[], String)}.
     */
    void set_position(double x,
                      double y,
                      double z,
                      String units);

    /**
     * Component-wise version of {@link #set_position(double[], boolean)}.
     */
    void set_position(double x,
                      double y,
                      double z,
                      boolean immediate);

    /**
     * Component-wise version of {@link #set_position(double[], String, boolean)}.
     */
    void set_position(double x,
                      double y,
                      double z,
                      String units,
                      boolean immediate);

    /**
     * Get the current camera position, in km.
     *
     * @return The camera position coordinates in the internal reference system,
     *         in km.
     */
    double[] get_position();

    /**
     * Get the current camera position, in the requested units.
     *
     * @param units The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     *
     * @return The camera position coordinates in the internal reference system and in the requested units.
     */
    double[] get_position(String units);

    /**
     * Set the camera position to the given coordinates, in the internal reference system and the given units.
     * The default behavior of this method posts a runnable to update the
     * camera after the current frame. If you need to call this method from
     * within a parked runnable, use {@link #set_position(double[], boolean)},
     * with the boolean set to <code>true</code>.
     *
     * @param dir   Vector of three components in internal coordinates and the given units.
     * @param units The distance units to use. One of "m", "km", "au", "ly", "pc", "internal".
     */
    void set_position(double[] dir,
                      String units);

    /**
     * Set the camera direction vector to the given vector, in the internal reference system.
     * The <code>immediate</code> parameter enables setting the camera state
     * immediately without waiting for the possible current update
     * operation to finish. Set this to true if you run this function
     * from within a parked runnable.
     *
     * @param dir       The direction vector in the internal reference system.
     * @param immediate Whether to apply the changes immediately, or wait for the next frame.
     */
    void set_direction(double[] dir,
                       boolean immediate);

    /**
     * Get the current camera direction vector.
     *
     * @return The camera direction vector in the internal reference system.
     */
    double[] get_direction();

    /**
     * Set the camera direction vector to the given vector, in the internal reference system.
     * <p>
     * You can convert from spherical coordinates using the following methods:
     * <ul>
     * <li>
     * {@link RefsysAPI#equatorial_cartesian_to_internal(double[], double)}
     * </li>
     * <li>
     * {@link RefsysAPI#galactic_to_cartesian(double, double, double)}
     * </li>
     * <li>
     * {@link RefsysAPI#ecliptic_to_cartesian(double, double, double)}
     * </li>
     * </ul>
     * The default behavior of this method posts a runnable to update the
     * camera after the current frame. If you need to call this method from
     * within a parked runnable, use {@link #set_direction(double[], boolean)},
     * with the boolean set to <code>true</code>.
     *
     * @param dir The direction vector in equatorial cartesian coordinates.
     */
    void set_direction(double[] dir);

    /**
     * Create a smooth camera orientation transition from the current camera orientation
     * to the given sky coordinates, in equatorial coordinates.
     * This method sets the camera in free mode.
     *
     * @param ra  The right ascension, in decimal degrees.
     * @param dec The declination, in decimal degrees.
     */
    void set_direction_equatorial(double ra, double dec);

    /**
     * Create a smooth camera orientation transition from the current camera orientation
     * to the given sky coordinates, in galactic coordinates.
     * This method sets the camera in free mode.
     *
     * @param l The galactic longitude, in decimal degrees.
     * @param b The galactic latitude, in decimal degrees.
     */
    void set_direction_galactic(double l, double b);

    /**
     * Set the camera up vector to the given vector, in the internal reference system.
     * The <code>immediate</code> parameter enables setting the camera state
     * immediately without waiting for the possible current update
     * operation to finish. Set this to true if you run this function
     * from within a parked runnable.
     *
     * @param up        The up vector in equatorial coordinates.
     * @param immediate Whether to apply the changes immediately, or wait for the next frame.
     */
    void set_up(double[] up,
                boolean immediate);

    /**
     * Get the current camera up vector.
     *
     * @return The camera up vector in the internal reference system.
     */
    double[] get_up();

    /**
     * Set the camera up vector to the given vector, in the internal reference system.
     * The default behavior of this method posts a runnable to update the
     * camera after the current frame. If you need to call this method from
     * within a parked runnable, use {@link #set_up(double[], boolean)},
     * with the boolean set to <code>true</code>.
     *
     * @param up The up vector in equatorial coordinates.
     */
    void set_up(double[] up);

    /**
     * Set the camera orientation to the given quaternion, given as an array of [x, y, z, w].
     *
     * @param q The 4-component quaternion.
     */
    void set_orientation_quaternion(double[] q);

    /**
     * Get the current camera orientation quaternion.
     *
     * @return The current camera orientation quaternion, as an array of [x, y, z, w].
     */
    double[] get_orientation_quaternion();

    /**
     * Set the focus and instantly moves the camera to a point in the line
     * defined by <code>focus</code>-<code>other</code> and rotated
     * <code>rotation</code> degrees around <code>focus</code> using the camera
     * up vector as a rotation axis.
     *
     * @param name  The name of the focus object.
     * @param other The name of the other object, to the fine a line from this to
     *              focus. Usually a light source.
     * @param rot   The rotation angle, in degrees.
     * @param sa    The target solid angle which determines the distance, in degrees.
     */
    void set_position_and_focus(String name,
                                String other,
                                double rot,
                                double sa);

    /**
     * Set the camera in free mode and points it to the given coordinates in equatorial system.
     *
     * @param ra  Right ascension in degrees.
     * @param dec Declination in degrees.
     */
    void point_at_equatorial(double ra,
                             double dec);


    /**
     * Set the camera in focus mode with the given focus object and instantly moves
     * the camera next to the focus object.
     *
     * @param name The name of the new focus object.
     */
    void go_to_object_instant(String name);

    /**
     * Move the camera to the object identified by the given name, internally using smooth camera transition calls,
     * like {@link #transition(double[], double[], double[], double)}.
     * The target position and orientation are computed first during the call execution and are not subsequently updated. This means that
     * the camera does not follow the object if it moves. If time is activated, and the object moves, this call does not go to the
     * object's current position at the end, but at the beginning.
     *
     * @param name         The name of the object to go to.
     * @param pos_duration The duration of the transition in position, in seconds.
     * @param ori_duration The duration of the transition in orientation, in seconds.
     */
    void go_to_object(String name, double pos_duration, double ori_duration);

    /**
     * Same as {@link #go_to_object(String, double, double)}, but with the target solid angle of the object.
     *
     * @param name         The name of the object to go to.
     * @param sa           The target solid angle of the object, in degrees. This
     *                     is used to compute the final distance to the object. The angle
     *                     gets larger and larger as we approach the object.
     * @param pos_duration The duration of the transition in position, in seconds.
     * @param ori_duration The duration of the transition in orientation, in seconds.
     */
    void go_to_object(String name, double sa, double pos_duration, double ori_duration);

    /**
     * Same as {@link #go_to_object(String, double, double)}, but with a boolean that indicates whether the call is synchronous.
     *
     * @param name         The name of the object to go to.
     * @param pos_duration The duration of the transition in position, in seconds.
     * @param ori_duration The duration of the transition in orientation, in seconds.
     * @param sync         If true, the call is synchronous and waits for the camera
     *                     file to finish. Otherwise, it returns immediately.
     */
    void go_to_object(String name, double pos_duration, double ori_duration, boolean sync);

    /**
     * Same as {@link #go_to_object(String, double, double, boolean)}, but with the target solid angle of the object.
     *
     * @param name         The name of the object to go to.
     * @param sa           The target solid angle of the object, in degrees. This
     *                     is used to compute the final distance to the object. The angle
     *                     gets larger and larger as we approach the object.
     * @param pos_duration The duration of the transition in position, in seconds.
     * @param ori_duration The duration of the transition in orientation, in seconds.
     * @param sync         If true, the call is synchronous and waits for the camera
     *                     file to finish. Otherwise, it returns immediately.
     */
    void go_to_object(String name, double sa, double pos_duration, double ori_duration, boolean sync);

    /**
     * Return the distance from the current postion of the camera to the surface of the object identified with the
     * given <code>name</code>. If the object is an abstract node or does not
     * exist, it returns a negative distance.
     *
     * @param name The name or id (HIP, TYC, sourceId) of the object.
     *
     * @return The distance to the object in km if it exists, a negative value
     *         otherwise.
     */
    double get_distance_to_object(String name);

    /**
     * Stop all camera motion.
     */
    void stop();

    /**
     * Center the camera so that its direction vector is aligned with the direction to the focus object, removing any deviation of the line of
     * sight. Useful to center the focus object again after turning.
     * <p>
     * This method only has effect if the camera is in {@link #focus_mode(String)}.
     */
    void center();

    /**
     * Set the maximum speed of the camera as an index pointing to a pre-set value. The index corresponds
     * to one of the following values:
     * <ol start="0">
     * <li>1 Km/h</li>
     * <li>10 Km/h</li>
     * <li>100 Km/h</li>
     * <li>1000 Km/h</li>
     * <li>1 Km/s</li>
     * <li>10 Km/s</li>
     * <li>100 Km/s</li>
     * <li>1000 Km/s</li>
     * <li>0.01 c</li>
     * <li>0.1 c</li>
     * <li>0.5 c</li>
     * <li>0.8 c</li>
     * <li>0.9 c</li>
     * <li>0.99 c</li>
     * <li>0.99999 c</li>
     * <li>1 c</li>
     * <li>2 c</li>
     * <li>10 c</li>
     * <li>1e3 c</li>
     * <li>1 AU/s</li>
     * <li>10 AU/s</li>
     * <li>1000 AU/s</li>
     * <li>10000 AU/s</li>
     * <li>1 pc/s</li>
     * <li>2 pc/s</li>
     * <li>10 pc/s</li>
     * <li>1000 pc/s</li>
     * <li>unlimited</li>
     * </ol>
     *
     * @param index The index of the maximum speed setting.
     */
    void set_max_speed(int index);

    /**
     * Set the camera to track the object with the given name. In this mode,
     * the position of the camera is still dependent on the focus object (if any), but
     * its direction points to the tracking object.
     *
     * @param name The name of the new tracking object.
     */
    void set_tracking_object(String name);

    /**
     * Remove the tracking object from the camera, if any.
     */
    void remove_tracking_object();

    /**
     * Return the closest object to the camera in this instant as a
     * {@link IFocus}.
     *
     * @return The closest object to the camera.
     */
    IFocus get_closest_object();

    /**
     * Set the field of view of the perspective matrix of the camera, in degrees.
     *
     * @param fov The new field of view value in degrees, between {@link gaiasky.util.Constants#MIN_FOV} and
     *            {@link gaiasky.util.Constants#MAX_FOV}.
     */
    void set_fov(float fov);

    /**
     * Set the camera state (position, direction and up vector).
     *
     * @param pos The position of the camera in internal units, not Km.
     * @param dir The direction of the camera.
     * @param up  The up vector of the camera.
     */
    void set_state(double[] pos,
                   double[] dir,
                   double[] up);

    /**
     * Set the camera state (position, direction and up vector) plus the current time.
     *
     * @param pos  The position of the camera in internal units, not Km.
     * @param dir  The direction of the camera.
     * @param up   The up vector of the camera.
     * @param time The new time of the camera as the
     *             number of milliseconds since the epoch (Jan 1, 1970).
     */
    void set_state_and_time(double[] pos,
                            double[] dir,
                            double[] up,
                            long time);

    /**
     * Create a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. This function waits for the transition to finish and then returns control
     * to the script.
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     *
     * @param pos      The target camera position in the internal reference system.
     * @param dir      The target camera direction in the internal reference system.
     * @param up       The target camera up in the internal reference system.
     * @param duration The duration of the transition in seconds.
     */
    void transition(double[] pos,
                    double[] dir,
                    double[] up,
                    double duration);

    /**
     * Create a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. This function waits for the transition to finish and then returns control
     * to the script.
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     *
     * @param pos      The target camera position in the internal reference system and the given distance units.
     * @param units    The distance units to use. One of "m", "km", "AU", "ly", "pc", "internal".
     * @param dir      The target camera direction in the internal reference system.
     * @param up       The target camera up in the internal reference system.
     * @param duration The duration of the transition in seconds.
     */
    void transition(double[] pos,
                    String units,
                    double[] dir,
                    double[] up,
                    double duration);

    /**
     * Same as {@link #transition(double[], double[], double[], double)} but the
     * camera position is given in Km.
     *
     * @param pos      The target camera position in Km.
     * @param dir      The target camera direction vector.
     * @param up       The target camera up vector.
     * @param duration The duration of the transition in seconds.
     */
    void transition_km(double[] pos,
                       double[] dir,
                       double[] up,
                       double duration);

    /**
     * Create a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. Optionally, the transition may be run synchronously or asynchronously to the
     * current script.
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     *
     * @param pos      The target camera position in the internal reference system.
     * @param dir      The target camera direction in the internal reference system.
     * @param up       The target camera up in the internal reference system.
     * @param duration The duration of the transition in seconds.
     * @param sync     If true, the call waits for the transition to finish before returning, otherwise it returns
     *                 immediately.
     */
    void transition(double[] pos,
                    double[] dir,
                    double[] up,
                    double duration,
                    boolean sync);

    /**
     * Create a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds. Optionally, the transition may be run synchronously or asynchronously to the
     * current script.
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     *
     * @param pos      The target camera position in the internal reference system and the given distance units.
     * @param units    The distance units to use. One of "m", "km", "AU", "ly", "pc", "internal".
     * @param dir      The target camera direction in the internal reference system.
     * @param up       The target camera up in the internal reference system.
     * @param duration The duration of the transition in seconds.
     * @param sync     If true, the call waits for the transition to finish before returning, otherwise it returns
     *                 immediately.
     */
    void transition(double[] pos,
                    String units,
                    double[] dir,
                    double[] up,
                    double duration,
                    boolean sync);

    /**
     * Create a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds.
     * <p>
     * This function accepts smoothing types and factors for the position and orientation.
     * <p>
     * This function will put the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     *
     * @param pos               The target camera position in the internal reference system and the given
     *                          distance units.
     * @param dir               The target camera direction in the internal reference system.
     * @param up                The target camera up in the internal reference system.
     * @param pos_duration      The duration of the transition in position, in seconds.
     * @param pos_smooth_type   The function type to use for the smoothing of positions. Either "logit",
     *                          "logisticsigmoid" or "none".
     *                          <ul>
     *                          <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                          an effect, otherwise, linear interpolation is used.</li>
     *                          <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                          0.09 and 0.01.</li>
     *                          <li>"none": no smoothing is applied.</li>
     *                          </ul>
     * @param pos_smooth_factor Smooth factor for the positions (depends on type).
     * @param ori_duration      The duration of the transition in orientation, in seconds.
     * @param ori_smooth_type   The function type to use for the smoothing of orientations. Either "logit",
     *                          "logisticsigmoid" or "none".
     *                          <ul>
     *                          <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                          an effect, otherwise, linear interpolation is used.</li>
     *                          <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                          0.09 and 0.01.</li>
     *                          <li>"none": no smoothing is applied.</li>
     *                          </ul>
     * @param ori_smooth_factor Smooth factor for the orientations (depends on type).
     */
    void transition(double[] pos,
                    double[] dir,
                    double[] up,
                    double pos_duration,
                    String pos_smooth_type,
                    double pos_smooth_factor,
                    double ori_duration,
                    String ori_smooth_type,
                    double ori_smooth_factor);

    /**
     * Create a smooth transition from the current camera state to the given camera state {camPos, camDir, camUp} in
     * the given number of seconds.
     * <p>
     * This function accepts smoothing types and factors for the position and orientation.
     * <p>
     * Optionally, this call may return immediately (async) or it may wait for the transition to finish (sync).
     * <p>
     * This function puts the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     *
     * @param pos               The target camera position in the internal reference system and the given
     *                          distance units.
     * @param units             The distance units to use. One of "m", "km", "AU", "ly", "pc", "internal".
     * @param dir               The target camera direction in the internal reference system.
     * @param up                The target camera up in the internal reference system.
     * @param pos_duration      The duration of the transition in position, in seconds.
     * @param pos_smooth_type   The function type to use for the smoothing of positions. Either "logit",
     *                          "logisticsigmoid" or "none".
     *                          <ul>
     *                          <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                          an effect, otherwise, linear interpolation is used.</li>
     *                          <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                          0.09 and 0.01.</li>
     *                          <li>"none": no smoothing is applied.</li>
     *                          </ul>
     * @param pos_smooth_factor Smooth factor for the positions (depends on type).
     * @param ori_duration      The duration of the transition in orientation, in seconds.
     * @param ori_smooth_type   The function type to use for the smoothing of orientations. Either "logit",
     *                          "logisticsigmoid" or "none".
     *                          <ul>
     *                          <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                          an effect, otherwise, linear interpolation is used.</li>
     *                          <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                          0.09 and 0.01.</li>
     *                          <li>"none": no smoothing is applied.</li>
     *                          </ul>
     * @param ori_smooth_factor Smooth factor for the orientations (depends on type).
     * @param sync              If true, the call waits for the transition to finish before returning,
     *                          otherwise it returns immediately.
     */
    void transition(double[] pos,
                    String units,
                    double[] dir,
                    double[] up,
                    double pos_duration,
                    String pos_smooth_type,
                    double pos_smooth_factor,
                    double ori_duration,
                    String ori_smooth_type,
                    double ori_smooth_factor,
                    boolean sync);

    /**
     * Create a smooth transition from the current camera position to the given camera position in
     * the given number of seconds.
     * <p>
     * This function accepts smoothing type and factor.
     * <p>
     * Optionally, this call may return immediately (async) or it may wait for the transition to finish (sync).
     * <p>
     * This function puts the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     *
     * @param pos           The target camera position in the internal reference system and the given
     *                      distance units.
     * @param units         The distance units to use. One of "m", "km", "AU", "ly", "pc", "internal".
     * @param duration      The duration of the transition in position, in seconds.
     * @param smooth_type   The function type to use for the smoothing of positions. Either "logit",
     *                      "logisticsigmoid" or "none".
     *                      <ul>
     *                      <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                      an effect, otherwise, linear interpolation is used.</li>
     *                      <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                      0.09 and 0.01.</li>
     *                      <li>"none": no smoothing is applied.</li>
     *                      </ul>
     * @param smooth_factor Smooth factor for the positions (depends on type).
     * @param sync          If true, the call waits for the transition to finish before returning,
     *                      otherwise it returns immediately.
     */
    void transition_position(double[] pos,
                             String units,
                             double duration,
                             String smooth_type,
                             double smooth_factor,
                             boolean sync);

    /**
     * Create a smooth transition from the current camera orientation to the given camera orientation {camDir, camUp}
     * in the given number of seconds.
     * <p>
     * This function accepts smoothing type and factor.
     * <p>
     * Optionally, this call may return immediately (async) or it may wait for the transition to finish (sync).
     * <p>
     * This function puts the camera in free mode, so make sure to change it afterward if you need to. Also,
     * this only works with the natural camera.
     *
     * @param dir           The target camera direction in the internal reference system.
     * @param up            The target camera up in the internal reference system.
     * @param duration      The duration of the transition in orientation, in seconds.
     * @param smooth_type   The function type to use for the smoothing of orientations. Either "logit",
     *                      "logisticsigmoid" or "none".
     *                      <ul>
     *                      <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                      an effect, otherwise, linear interpolation is used.</li>
     *                      <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                      0.09 and 0.01.</li>
     *                      <li>"none": no smoothing is applied.</li>
     *                      </ul>
     * @param smooth_factor Smooth factor for the orientations (depends on type).
     * @param sync          If true, the call waits for the transition to finish before returning,
     *                      otherwise it returns immediately.
     */
    void transition_orientation(double[] dir,
                                double[] up,
                                double duration,
                                String smooth_type,
                                double smooth_factor,
                                boolean sync);
}
