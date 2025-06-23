/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.EventManager;
import gaiasky.script.v2.api.OutputAPI;
import gaiasky.util.Settings;

/**
 * The output module contains methods and calls that relate to the frame output system, the
 * screenshot system, and other types of output systems.
 */
public class OutputModule extends APIModule implements OutputAPI {
    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public OutputModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public boolean is_frame_output_active() {
        return Settings.settings.frame.active;
    }

    @Override
    public double get_frame_output_fps() {
        return Settings.settings.frame.targetFps;
    }
}
