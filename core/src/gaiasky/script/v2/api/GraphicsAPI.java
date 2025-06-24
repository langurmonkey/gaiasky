/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.GraphicsModule;
import gaiasky.util.Constants;

/**
 * Public API definition for the graphics module, {@link GraphicsModule}.
 * <p>
 * The graphics module contains methods and calls that modify and query the graphics and rendering system.
 */
public interface GraphicsAPI {
    /**
     * Set the ambient light to a certain value.
     *
     * @param value The value of the ambient light in [0,1].
     */
    void set_ambient_light(float value);

    /**
     * Get the star brightness value.
     *
     * @return The brightness value, between 0 and 100.
     */
    float get_star_brightness();

    /**
     * Set the star brightness value.
     *
     * @param brightness The brightness value, between 0 and 100.
     */
    void set_star_brightness(float brightness);

    /**
     * Set the star brightness power profile value in [1.1, 0.9]. Default value is 1.
     * The power is applied to the star solid angle (from camera),
     * before clamping, as sa = pow(sa, r).
     *
     * @param power The power value in [0, 100].
     */
    void set_star_brightness_power(float power);

    /**
     * Set the star glow factor level value. This controls the amount of glow light
     * when the camera is close to stars. Must be between {@link Constants#MIN_STAR_GLOW_FACTOR} and
     * {@link Constants#MAX_STAR_GLOW_FACTOR}.
     * Default is 0.06.
     *
     * @param glowFactor The new glow factor value.
     */
    void set_star_glow_factor(float glowFactor);

    /**
     * Get the current point size value in pixels.
     *
     * @return The size value, in pixels.
     */
    float get_point_size();

    /**
     * Set the base point size.
     *
     * @param size The size value, between {@link Constants#MIN_STAR_POINT_SIZE} and
     *             {@link Constants#MAX_STAR_POINT_SIZE}.
     */
    void set_point_size(float size);

    /**
     * Get the base star opacity.
     *
     * @return The base opacity value.
     */
    float get_star_base_opacity();

    /**
     * Set the base star opacity.
     *
     * @param opacity The base opacity value, between {@link Constants#MIN_STAR_MIN_OPACITY} and
     *                {@link Constants#MAX_STAR_MIN_OPACITY}.
     */
    void set_star_base_opacity(float opacity);

    /**
     * Set the star texture index, in [1, 5].
     * <p>
     * <ol>
     *     <li>horizontal spike</li>
     *     <li>god rays</li>
     *     <li>horizontal and vertical spikes</li>
     *     <li>simple radial profile</li>
     *     <li>diagonal spikes</li>
     * </ol>
     *
     * @param index The new star texture index.
     */
    void set_star_texture_index(int index);

    /**
     * Set the number of nearest stars to be processed for each
     * star set. Use this method with caution, it is mainly intended for internal purposes.
     *
     * @param n The new number of nearest stars.
     */
    void set_star_set_metadata_size(int n);

    /**
     * Enable or disable the rendering of close stars as billboards.
     *
     * @param flag The state flag.
     */
    void set_star_set_billboard(boolean flag);

    /**
     * Set the solid angle below which orbits fade and disappear.
     *
     * @param angleDeg The threshold angle in degrees.
     */
    void set_orbit_solid_angle_threshold(float angleDeg);

    /**
     * Limit the frame rate of Gaia Sky to the given value, in frames per second.
     *
     * @param limitFps The new maximum frame rate as a double-precision floating point number. Set zero or negative to
     *                 unlimited.
     */
    void set_limit_fps(double limitFps);

    /**
     * Limit the frame rate of Gaia Sky to the given value, in frames per second.
     *
     * @param limitFps The new maximum frame rate as an integer number. Set zero or negative to unlimited.
     */
    void set_limit_fps(int limitFps);
}
