/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.EventManager;
import gaiasky.script.v2.api.GeomAPI;

/**
 * The geometry module provides calls and methods to carry out geometrical operations directly
 * within the scripting system.
 */
public class GeomModule extends APIModule implements GeomAPI {
    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public GeomModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }
}
