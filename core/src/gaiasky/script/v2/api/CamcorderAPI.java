/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.BaseModule;
import gaiasky.script.v2.impl.CamcorderModule;

/**
 * Public API definition for the camcorder module, {@link CamcorderModule}.
 * <p>
 * This module contains methods and calls related to the camera path subsystem and the camcorder,
 * which enables capturing and playing back camera path files.
 */
public interface CamcorderAPI {
    /**
     * Set the target frame rate of the camcorder. This artificially sets the frame rate (inverse of frame time) of Gaia
     * Sky to this value while the camera is recording and playing. Make sure to use the right FPS setting during
     * playback.
     *
     * @param fps The target frame rate for the camcorder.
     */
    void set_fps(double fps);

    /**
     * Get the current frame rate setting of the camcorder.
     *
     * @return The frame rate setting of the camcorder.
     */
    double get_fps();

    /**
     * Start recording the camera path to an auto-generated file in the default
     * camera directory ({@link BaseModule#get_camcorder_dir()}). This command has no
     * effect if the camera is already being recorded.
     */
    void start();

    /**
     * Start recording a camera path with the given filename. The filename
     * is without extension or path. The final path with the camera file, after
     * invoking {@link #stop()}, is:
     * <p>
     * <code>{@link BaseModule#get_camcorder_dir()} + "/" + filename + ".gsc"</code>
     * <p>
     * This command has no effect if the camera is already being recorded.
     *
     * @param path Path to the camera file to play.
     */
    void start(String path);

    /**
     * Stop the current camera recording. This command has no effect if the
     * camera was not being recorded.
     */
    void stop();

    /**
     * Play a <code>.gsc</code> camera path file and returns immediately. This
     * method does not wait for the camera path file to finish playing.
     *
     * @param path The path to the camera file. Path is relative to the application's root directory or absolute.
     */
    void play(String path);

    /**
     * Runs a .gsc camera path file and returns immediately. This
     * function accepts a boolean indicating whether to wait for the
     * camera path file to finish or not.
     *
     * @param path The path to the camera file. Path is relative to the application's root directory or absolute.
     * @param sync If true, the call is synchronous and waits for the camera
     *             file to finish. Otherwise, it returns immediately.
     */
    void play(String path,
              boolean sync);

}
