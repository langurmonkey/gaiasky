/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.render.postprocess.effects.CubmeapProjectionEffect;
import gaiasky.script.v2.impl.GraphicsModule;
import gaiasky.util.Constants;

/**
 * Public API definition for the graphics module, {@link GraphicsModule}.
 * <p>
 * The graphics module contains methods and calls that modify and query the graphics and rendering system.
 */
public interface GraphicsAPI {
    /**
     * Set the brightness level of the render system.
     *
     * @param level The brightness level as a double precision floating point
     *              number in [-1,1]. The neutral value is 0.0.
     */
    void set_image_brightness(double level);

    /**
     * Set the contrast level of the render system.
     *
     * @param level The contrast level as a double precision floating point number
     *              in [0,2]. The neutral value is 1.0.
     */
    void set_image_contrast(double level);

    /**
     * Set the hue level of the render system.
     *
     * @param level The hue level as a double precision floating point number
     *              in [0,2]. The neutral value is 1.0.
     */
    void set_image_hue(double level);

    /**
     * Set the saturation level of the render system.
     *
     * @param level The saturation level as a double precision floating point number
     *              in [0,2]. The neutral value is 1.0.
     */
    void set_image_saturation(double level);

    /**
     * Set the gamma correction level.
     *
     * @param level The gamma correction level in [0,3] as a floating point number.
     *              The neutral value is 1.2.
     */
    void set_gamma_correction(double level);

    /**
     * Set the high dynamic range tone mapping algorithm type. The types can be:
     * <ul>
     *     <li>"auto" - performs an automatic HDR tone mapping based on the current luminosity of the scene</li>
     *     <li>"exposure" - performs an exposure-based HDR tone mapping. The exposure value must be set with {@link #set_exposure_tone_mapping(double)}</li>
     *     <li>"aces" - performs the ACES tone mapping</li>
     *     <li>"uncharted" - performs the tone mapping implemented in Uncharted</li>
     *     <li>"filmic" - performs a filmic tone mapping</li>
     *     <li>"none" - no HDR tone mapping</li>
     * </ul>
     *
     * @param type The HDR tone mapping type. One of ["auto"|"exposure"|"aces"|"uncharted"|"filmic"|"none"].
     */
    void set_hdr_tone_mapping(String type);

    /**
     * Set the exposure level.
     *
     * @param level The exposure level in [0,n]. Set to 0 to disable exposure tone mapping.
     */
    void set_exposure_tone_mapping(double level);

    /**
     * Enable or disable the planetarium mode.
     *
     * @param state The boolean state. True to activate, false to deactivate.
     */
    void mode_planetarium(boolean state);

    /**
     * Enable and disable the cubemap mode.
     *
     * @param state      The boolean state. True to activate, false to deactivate.
     * @param projection The projection as a string.
     */
    void mode_cubemap(boolean state,
                      String projection);

    /**
     * Enable or disable the panorama mode.
     *
     * @param state The boolean state. True to activate, false to deactivate.
     */
    void mode_panorama(boolean state);

    /**
     * Set the resolution (width and height are the same) of each side of the
     * frame buffers used to capture each of the 6 directions that go into the
     * cubemap to construct the equirectangular image for the 360 mode. This
     * should roughly be 1/3 of the output resolution at which the 360 mode are
     * to be captured (or screen resolution).
     *
     * @param resolution The resolution of each of the sides of the cubemap for the 360
     *                   mode.
     */
    void set_cubemap_resolution(int resolution);

    /**
     * Set the cubemap projection to use.
     * <p>
     * Accepted values are:
     * <ul>
     *     <li>"equirectangular" - spherical projection.</li>
     *     <li>"cylindrical" - cylindrical projection.</li>
     *     <li>"hammer" - Hammer projection.</li>
     *     <li>"orthographic" - orthographic projection, with the two hemispheres side-by-side.</li>
     *     <li>"orthosphere" - orthographic projection, with the two hemispheres overlaid. That gives an outside view of the camera's celestial sphere. </li>
     *     <li>"orthosphere_crosseye" - same as orthosphere, but duplicated to produce a stereoscopic cross-eye image (side by side). </li>
     *     <li>"azimuthal_equidistant" - azimuthal equidistant projection, used in Planetarium mode.</li>
     * </ul>
     * See {@link CubmeapProjectionEffect} for possible
     * values.
     *
     * @param projection The projection, in
     *                   ["equirectangular"|"cylindrical"|"hammer"|"orthographic"|"orthosphere"|"orthosphere_crossye"|"azimuthal_equidistant"].
     */
    void set_cubemap_projection(String projection);

    /**
     * Enable or disable the orthosphere view mode.
     *
     * @param state The state, true to activate and false to deactivate.
     */
    void mode_orthosphere(boolean state);

    /**
     * Set index of refraction of celestial sphere in orthosphere view mode.
     *
     * @param ior The index of refraction.
     */
    void set_index_of_refraction(float ior);

    /**
     * Enable or disable the stereoscopic mode.
     *
     * @param state The boolean state. True to activate, false to deactivate.
     */
    void mode_stereoscopic(boolean state);

    /**
     * Change the stereoscopic profile.
     *
     * @param index The index of the new profile:
     *              <ul>
     *              <li>0 - VR_HEADSET</li>
     *              <li>1 - HD_3DTV</li>
     *              <li>2 - CROSSEYE</li>
     *              <li>3 - PARALLEL_VIEW</li>
     *              <li>4 - ANAGLYPHIC (red-cyan)</li>
     *              </ul>
     */
    void set_stereo_profile(int index);

    /**
     * Set the re-projection mode. Possible modes are:
     * <ul>
     *     <li>"disabled"</li>
     *     <li>"default"</li>
     *     <li>"accurate"</li>
     *     <li>"stereographic_screen"</li>
     *     <li>"stereographic_long"</li>
     *     <li>"stereographic_short"</li>
     *     <li>"stereographic_180"</li>
     *     <li>"lambert_screen"</li>
     *     <li>"lambert_long"</li>
     *     <li>"lambert_short"</li>
     *     <li>"lambert_180"</li>
     *     <li>"orthographic_screen"</li>
     *     <li>"orthographic_long"</li>
     *     <li>"orthographic_short"</li>
     *     <li>"orthographic_180"</li>
     * </ul>
     *
     * @param mode The re-projection mode, as a string.
     */
    void mode_reprojection(String mode);

    /**
     * Set the scaling factor for the back-buffer.
     *
     * @param scale The back-buffer scaling factor.
     */
    void set_back_buffer_scale(float scale);

    /**
     * Get the current frame number. Useful for timing actions in scripts.
     *
     * @return The current frame number.
     */
    long get_current_frame_number();

    /**
     * Enable or disables the lens flare effect.
     *
     * @param state Activate (true) or deactivate (false).
     */
    void effect_lens_flare(boolean state);

    /**
     * Set the strength of the lens flare effect, in [0,1].
     * <p>
     * Set to 0 to disable the effect.
     *
     * @param strength The strength or intensity of the lens flare, in [0,1].
     */
    void effect_lens_flare(double strength);

    /**
     * Enable or disable the camera motion blur effect.
     *
     * @param state Activate (true) or deactivate (false).
     */
    void effect_motion_blur(boolean state);

    /**
     * Enable or disable the camera motion blur effect.
     *
     * @param strength The strength of camera the motion blur effect, in [{@link Constants#MOTIONBLUR_MIN}, {@link Constants#MOTIONBLUR_MAX}].
     */
    void effect_motion_blur(double strength);

    /**
     * Enable or disable stars' light glowing and spilling over closer objects.
     *
     * @param state Enable (true) or disable (false).
     */
    void effect_star_glow(boolean state);

    /**
     * Set the strength value for the bloom effect.
     *
     * @param value Bloom strength between 0 and 100. Set to 0 to deactivate the
     *              bloom.
     */
    void effect_bloom(float value);

    /**
     * Set the amount of chromatic aberration. Set to 0 to disable the effect.
     *
     * @param value Chromatic aberration amount in [0,0.05].
     */
    void effect_chromatic_aberration(float value);

    /**
     * Set the value of smooth lod transitions, allowing or disallowing octant fade-ins of
     * as they come into view.
     *
     * @param value Activate (true) or deactivate (false).
     */
    void set_smooth_lod_transitions(boolean value);

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
