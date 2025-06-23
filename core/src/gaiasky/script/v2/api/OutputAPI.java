/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.OutputModule;

/**
 * API definition for the output module, {@link OutputModule}.
 * <p>
 * The output module contains calls and methods to access, modify, and query the frame output, the screenshots
 * and other kinds of output systems.
 */
public interface OutputAPI {
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
}
