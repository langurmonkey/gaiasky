/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.BaseModule;

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
}
