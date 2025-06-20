/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2;

import gaiasky.event.Event;
import gaiasky.event.EventManager;

/**
 * The time module contains methods and calls that query and manipulate the simulation time and
 * other time frames.
 */
public class TimeModule extends APIModule{
    /**
     * Create a new module with the given attributes.
     *
     * @param em Reference to the event manager.
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public TimeModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    /**
     * Sets the current time frame to <b>real time</b>. All the commands
     * executed after this command becomes active will be in the <b>real
     * time</b> frame (clock ticks).
     */
    public void activate_real_time_frame() {
        api.base.post_runnable(() -> em.post(Event.EVENT_TIME_FRAME_CMD, this, EventManager.TimeFrame.REAL_TIME));
    }

    /**
     * Sets the current time frame to <b>simulation time</b>. All the commands
     * executed after this command becomes active will be in the <b>simulation
     * time</b> frame (simulation clock in the app).
     */
    public void activate_simulation_time_Frame() {
        api.base.post_runnable(() -> em.post(Event.EVENT_TIME_FRAME_CMD, this, EventManager.TimeFrame.SIMULATION_TIME));
    }
}
