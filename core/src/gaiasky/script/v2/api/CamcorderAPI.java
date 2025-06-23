/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.CamcorderModule;

/**
 * Public API definition for the camcorder module, {@link CamcorderModule}.
 * <p>
 * This module contains methods and calls related to the camera path subsystem and the camcorder,
 * which enables capturing and playing back camera path files.
 */
public interface CamcorderAPI {
    /**
     * Get the current frame rate setting of the camcorder.
     *
     * @return The frame rate setting of the camcorder.
     */
    double get_camcorder_fps();
}
