/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.TimeModule;

/**
 * API definition for the time module, {@link TimeModule}.
 * <p>
 * The time module contains methods and calls to access, modify, and query the time subsystem.
 */
public interface TimeAPI {
    /**
     * Set the current time frame to <b>real time</b>. All the commands
     * executed after this command becomes active will be in the <b>real
     * time</b> frame (real clock ticks).
     */
    void activate_real_time_frame();

    /**
     * Set the current time frame to <b>simulation time</b>. All the commands
     * executed after this command becomes active will be in the <b>simulation
     * time</b> frame (simulation clock ticks).
     */
    void activate_simulation_time_Frame();
}
