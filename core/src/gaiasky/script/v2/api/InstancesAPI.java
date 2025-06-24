/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.CameraModule;
import gaiasky.script.v2.impl.InstancesModule;

/**
 * API definition for the instances module, {@link InstancesModule}.
 * <p>
 * The instances module contains methods and calls to access, modify, and query the connected instances
 * subsystem (primary-replica).
 */
public interface InstancesAPI {
    /**
     * Sets the projection yaw angle (if this is a replica instance), in degrees.
     * The yaw angle turns the camera to the right.
     * This function is intended for multi-projector setups, to configure
     * replicas without restarting Gaia Sky.
     *
     * @param yaw The yaw angle in degrees.
     */
    void set_projection_yaw(float yaw);

    /**
     * Sets the projection pitch angle (if this is a replica instance), in degrees.
     * The pitch angle turns the camera up.
     * This function is intended for multi-projector setups, to configure
     * replicas without restarting Gaia Sky.
     *
     * @param pitch The pitch angle in degrees.
     */
    void set_projection_pitch(float pitch);

    /**
     * Sets the projection roll angle (if this is a replica instance), in degrees.
     * The roll angle rolls the camera clockwise.
     * This function is intended for multi-projector setups, to configure
     * replicas without restarting Gaia Sky.
     *
     * @param roll The roll angle in degrees.
     */
    void set_projection_roll(float roll);

    /**
     * Same as {@link CameraModule#set_fov(float)}, but this method bypasses the restriction of an active
     * projection in the replica (replicas that have an active projection do not accept
     * fov modification events).
     *
     * @param fov The field of view angle.
     */
    void set_projection_fov(float fov);

}
