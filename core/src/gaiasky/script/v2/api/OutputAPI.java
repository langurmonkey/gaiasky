/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.BaseModule;
import gaiasky.script.v2.impl.OutputModule;

/**
 * API definition for the output module, {@link OutputModule}.
 * <p>
 * The output module contains calls and methods to access, modify, and query the frame output, the screenshots
 * and other kinds of output systems.
 */
public interface OutputAPI {
    /**
     * Configures the screenshot system, setting the resolution of the images,
     * the output directory and the image name prefix.
     *
     * @param w      Width of images.
     * @param h      Height of images.
     * @param path   The output directory path.
     * @param prefix The file name prefix.
     */
    void configure_screenshots(int w,
                               int h,
                               String path,
                               String prefix);

    /**
     * Get the current output directory for screenshots as a string. This comes from a setting stored in the
     * configuration file. To get the default screenshots location, use {@link BaseModule#get_default_screenshots_dir()}.
     *
     * @return The absolute path to the current output directory for screenshots.
     */
    String get_current_screenshots_dir();

    /**
     * Set the screenshot mode. Possible values are <code>simple</code> and <code>advanced</code>.
     * <p>
     * The <b>simple</b> mode is faster and just outputs the last frame rendered to the Gaia Sky window, with the same
     * resolution and containing the UI elements.
     * The <b>advanced</b> mode redraws the last frame using the resolution configured using
     * {@link #configure_screenshots(int, int, String, String)} and
     * it does not draw the UI.
     *
     * @param mode The screenshot mode. <code>simple</code> or <code>advanced</code>.
     */
    void screenshot_mode(String mode);

    /**
     * Take a screenshot of the current frame and saves it to the configured
     * location (see {@link #configure_screenshots(int, int, String, String)}).
     */
    void screenshot();

    /**
     * Configure the frame output system, setting the resolution of the images,
     * the target frames per second, the output directory and the image name
     * prefix. This function sets the frame output mode to 'advanced'.
     *
     * @param w      Width of images.
     * @param h      Height of images.
     * @param fps    Target frames per second (number of images per second).
     * @param path   The output directory path.
     * @param prefix The file name prefix.
     */
    void configure_frame_output(int w,
                                int h,
                                int fps,
                                String path,
                                String prefix);

    /**
     * Get the current output directory for the frame output system as a string. This comes from a setting stored in the
     * configuration file. To get the default frame output location, use {@link BaseModule#get_default_frame_output_dir()}.
     *
     * @return The absolute path to the current output directory for the frame output system.
     */
    String get_current_frame_output_dir();

    /**
     * Configure the frame output system, setting the resolution of the images,
     * the target frames per second, the output directory and the image name
     * prefix. This function sets the frame output mode to 'advanced'.
     *
     * @param w      Width of images.
     * @param h      Height of images.
     * @param fps    Target frames per second (number of images per second).
     * @param path   The output directory path.
     * @param prefix The file name prefix.
     */
    void configure_frame_output(int w,
                                int h,
                                double fps,
                                String path,
                                String prefix);

    /**
     * Set the frame output mode. Possible values are <code>simple</code> and <code>advanced</code>.
     * <p>
     * The <b>simple</b> mode is faster and just outputs the last frame rendered to the Gaia Sky window, with the same
     * resolution and containing the UI elements.
     * The <b>advanced</b> mode redraws the last frame using the resolution configured using
     * {@link #configure_frame_output(int, int, int, String, String)} and
     * it does not draw the UI.
     *
     * @param mode The screenshot mode. <code>simple</code> or <code>advanced</code>.
     */
    void frame_output_mode(String mode);

    /**
     * Activate or deactivate the frame output system. If called with true,
     * the system starts outputting images right away.
     *
     * @param active Whether to activate or deactivate the frame output system.
     */
    void frame_output(boolean active);

    /**
     * Check whether the frame output system is currently on (i.e. frames are being saved to disk).
     *
     * @return True if the render output is active.
     */
    boolean is_frame_output_active();

    /**
     * Get the current frame rate setting of the frame output system.
     *
     * @return The frame rate setting of the frame output system.
     */
    double get_frame_output_fps();

    /**
     * Resets to zero the image sequence number used to generate the file names of the
     * frame output images.
     */
    void reset_frame_output_sequence_number();
}
