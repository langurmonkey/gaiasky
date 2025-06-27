/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.BaseModule;
import gaiasky.script.v2.impl.DataModule;
import gaiasky.script.v2.impl.OutputModule;
import gaiasky.util.Constants;

/**
 * Public API definition for the {@link BaseModule}, which contains methods and functions that
 * perform essential or basic functionality.
 */
public interface BaseAPI {
    /**
     * Return a string with the version number, the build string, the system, the builder, and the build time.
     *
     * @return A string with the full version information.
     */
    String get_version();

    /**
     * Return the version number string.
     *
     * @return The version number string.
     */
    String get_version_number();

    /**
     * Return the build string.
     *
     * @return The build string.
     */
    String get_build_string();

    /**
     * Gets the location of the asset directory. The asset directory contains some internal files essential for Gaia Sky to function
     * properly, like the default versions of the configuration file, or the UI theme files.
     * <p>
     * This location depends on the operating system and launch method.
     */
    String get_assets_dir();

    /**
     * Get the absolute path of the default output directory for the frame output system.
     * This may be different from where the actual frames are saved, as this is specified in a setting in the configuration file.
     * <p>
     * In order to get the actual current output location for the frame output system, use {@link OutputModule#get_current_frame_output_dir}.
     *
     * @return Absolute path of directory where still frames are saved.
     */
    String get_default_frame_output_dir();

    /**
     * Get the absolute path of the default directory where the screenshots are saved.
     * This may be different from where the actual screenshots are saved, as this is specified in a setting in the configuration file.
     * <p>
     * In order to get the actual current output location for screenshots, use {@link OutputModule#get_current_screenshots_dir}.
     *
     * @return Absolute path of directory where screenshots are saved.
     */
    String get_default_screenshots_dir();

    /**
     * Get the absolute path of the default directory where the camcorder files are saved.
     *
     * @return Absolute path of directory where camcorder files are saved.
     */
    String get_camcorder_dir();

    /**
     * Get the absolute path to the location of the inputListener mappings.
     *
     * @return Absolute path to the location of the inputListener mappings.
     */
    String get_mappings_dir();

    /**
     * Get the absolute path of the local data directory, configured in your <code>config.yaml</code> file.
     *
     * @return Absolute path to the location of the data files.
     */
    String get_data_dir();

    /**
     * Get the absolute path to the location of the configuration directory.
     *
     * @return Absolute path of config directory.
     */
    String get_config_dir();

    /**
     * Get the absolute path to the default datasets directory.
     * This is <code>~/.gaiasky/</code> in Windows and macOS, and <code>~/.local/share/gaiasky</code> in Linux.
     * <p>
     * Note that the actual datasets directory may be different, as it is stored in a setting in the configuration
     * file. This only returns the default location. You can get the actual location where datasets are
     * stored with {@link DataModule#get_datasets_directory()}.
     *
     * @return Absolute path to the default data directory.
     */
    String get_default_datasets_dir();

    /**
     * Sleep for the given number of seconds in the application time (FPS), so
     * if we are capturing frames and the frame rate is set to 30 FPS, the
     * command sleep(1) will put the script to sleep for 30 frames.
     *
     * @param seconds The number of seconds to wait.
     */
    void sleep(float seconds);

    /**
     * Sleep for a number of frames. The frame monitor is notified at the beginning
     * of each frame, before the update-render cycle. When frames is 1, this method
     * returns just before the processing of the next frame starts.
     *
     * @param frames The number of frames to wait.
     */
    void sleep_frames(long frames);


    /**
     * Post a {@link Runnable} to the main loop thread. This {@link Runnable} runs <strong>once</strong> after the update-scene stage, and
     * before the render stage.
     *
     * @param runnable The runnable to run.
     */
    void post_runnable(Runnable runnable);

    /**
     * <p>
     * Park an update {@link Runnable} to the main loop thread, and keeps it running every frame
     * until it finishes, or it is removed by {@link #remove_runnable(String)}.
     * This object runs after the update-scene stage and before the render stage,
     * so it is intended for updating scene objects.
     * </p>
     * <p>
     * Be careful with this function, as it probably needs a cleanup before the script is finished. Otherwise,
     * all parked runnables will keep running until Gaia Sky is restarted, so make sure to
     * remove them with {@link #remove_runnable(String)} if needed.
     * </p>
     *
     * @param id       The string id to identify the runnable.
     * @param runnable The scene update runnable to park.
     */
    void park_scene_runnable(String id, Runnable runnable);

    /**
     * <p>
     * Park a camera update {@link Runnable} to the main loop thread, and keeps it running every frame
     * until it finishes, or it is removed by {@link #remove_runnable(String)}.
     * This object runs after the update-camera stage and before the update-scene, so it is intended for updating the
     * camera only.
     * </p>
     * <p>
     * Be careful with this function, as it probably needs a cleanup before the script is finished. Otherwise,
     * all parked runnables will keep running until Gaia Sky is restarted, so make sure to
     * remove them with {@link #remove_runnable(String)} if needed.
     * </p>
     *
     * @param id       The string id to identify the runnable.
     * @param runnable The camera update runnable to park.
     */
    void park_camera_runnable(String id, Runnable runnable);

    /**
     * Remove the runnable with the given id, if any. Use this method to remove previously parked scene and camera
     * runnables.
     *
     * @param id The id of the runnable to remove.
     */
    void remove_runnable(String id);

    /**
     * <p>Create a backup of the current settings state that can be restored later on.
     * The settings are backed up in a stack, so multiple calls to this method put different copies of the settings
     * on the stack in a LIFO fashion.</p>
     * <p>This method, together with {@link #settings_restore()}, are useful to back up and restore
     * the
     * settings at the beginning and end of your scripts, respectively, and ensure that the user settings are left
     * unmodified after your script ends.</p>
     */
    void settings_backup();

    /**
     * <p>Take the settings object at the top of the settings stack and makes it effective.</p>
     * <p>This method, together with {@link #settings_backup()}, are useful to back up and restore the
     * settings at the beginning and end of your scripts, respectively, and ensure that the user settings are left
     * unmodified after your script ends.</p>
     * <p>WARN: This function applies all settings immediately, and the user interface may be re-initialized.
     * Be aware that the UI may be set to its default state after this call.</p>
     *
     * @return True if the stack was not empty and the settings were restored successfully. False otherwise.
     */
    boolean settings_restore();

    /**
     * Clear the stack of settings objects. This will invalidate all previous calls to
     * {@link #settings_backup()},
     * effectively making the settings stack empty. Calling {@link #settings_restore()} after this
     * method will return false.
     */
    void settings_clear_stack();

    /**
     * Returns the meter to internal unit conversion factor. Use this factor to multiply
     * your coordinates in meters to get them in internal units.
     *
     * @return The factor {@link Constants#M_TO_U}.
     */
    double m_to_internal();

    /**
     * Returns the internal unit to meter conversion factor. Use this factor to multiply
     * your coordinates in internal units to get them in meters.
     *
     * @return The factor {@link Constants#U_TO_M}.
     */
    double internal_to_m();

    /**
     * Converts the value in internal units to metres.
     *
     * @param internalUnits The value in internal units.
     *
     * @return The value in metres.
     */
    double internal_to_m(double internalUnits);

    /**
     * Converts the value in internal units to Kilometers.
     *
     * @param internalUnits The value in internal units.
     *
     * @return The value in Kilometers.
     */
    double internal_to_km(double internalUnits);

    /**
     * Converts the array in internal units to Kilometers.
     *
     * @param internalUnits The array in internal units.
     *
     * @return The array in Kilometers.
     */
    double[] internal_to_km(double[] internalUnits);

    /**
     * Converts the value in internal units to parsecs.
     *
     * @param internalUnits The value in internal units.
     *
     * @return The value in parsecs.
     */
    double internal_to_pc(double internalUnits);

    /**
     * Converts the array in internal units to parsecs.
     *
     * @param internalUnits The array in internal units.
     *
     * @return The array in parsecs.
     */
    double[] internal_to_pc(double[] internalUnits);

    /**
     * Converts the metres to internal units.
     *
     * @param metres The value in metres.
     *
     * @return The value in internal units.
     */
    double m_to_internal(double metres);

    /**
     * Converts the kilometres to internal units.
     *
     * @param kilometres The value in kilometers.
     *
     * @return The value in internal units.
     */
    double km_to_internal(double kilometres);

    /**
     * Converts the parsecs to internal units.
     *
     * @param parsecs The value in parsecs.
     *
     * @return The value in internal units.
     */
    double pc_to_internal(double parsecs);

    /**
     * Print text using the internal logging system.
     *
     * @param message The message.
     */
    void print(String message);

    /**
     * Print text using the internal logging system.
     *
     * @param message The message.
     */
    void log(String message);

    /**
     * Log an error using the internal logging system.
     *
     * @param message The error message.
     */
    void error(String message);

    /**
     * Initiate the quit action to terminate the program. This call causes Gaia Sky to exit.
     */
    void quit();
}
