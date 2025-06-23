/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.EventManager;
import gaiasky.script.v2.api.CamcorderAPI;
import gaiasky.util.Settings;

/**
 * The camcorder module contains methods and calls related to the camera path subsystem and
 * the camcorder, which enables capturing and playing back camera path files.
 */
public class CamcorderModule extends APIModule  implements CamcorderAPI {
    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public CamcorderModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public double get_camcorder_fps() {
        return Settings.settings.camrecorder.targetFps;
    }

}
