/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.script.v2.api.TimeAPI;

/**
 * The time module contains methods and calls to access, modify, and query the time subsystem.
 */
public class TimeModule extends APIModule implements TimeAPI {
    /**
     * Create a new module with the given attributes.
     *
     * @param em   Reference to the event manager.
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public TimeModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public void activate_real_time_frame() {
        api.base.post_runnable(() -> em.post(Event.EVENT_TIME_FRAME_CMD, this, EventManager.TimeFrame.REAL_TIME));
    }

    @Override
    public void activate_simulation_time_Frame() {
        api.base.post_runnable(() -> em.post(Event.EVENT_TIME_FRAME_CMD, this, EventManager.TimeFrame.SIMULATION_TIME));
    }
}
