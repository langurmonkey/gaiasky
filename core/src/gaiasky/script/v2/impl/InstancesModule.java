/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.script.v2.api.InstancesAPI;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.SlaveManager;

/**
 * The instances module contains methods and calls to access, modify, and query the connected instances
 * subsystem (primary-replica).
 */
public class InstancesModule extends APIModule implements InstancesAPI {
    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public InstancesModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public void set_projection_yaw(float yaw) {
        if (SlaveManager.projectionActive()) {
            api.base.post_runnable(() -> {
                Settings.settings.program.net.slave.yaw = yaw;
                SlaveManager.instance.yaw = yaw;
            });
        }
    }

    @Override
    public void set_projection_pitch(float pitch) {
        if (SlaveManager.projectionActive()) {
            api.base.post_runnable(() -> {
                Settings.settings.program.net.slave.pitch = pitch;
                SlaveManager.instance.pitch = pitch;
            });
        }
    }

    @Override
    public void set_projection_roll(float roll) {
        if (SlaveManager.projectionActive()) {
            api.base.post_runnable(() -> {
                Settings.settings.program.net.slave.roll = roll;
                SlaveManager.instance.roll = roll;
            });
        }
    }

    @Override
    public void set_projection_fov(float fov) {
        if (api.validator.checkNum(fov, Constants.MIN_FOV, 170f, "newFov"))
            api.base.post_runnable(() -> {
                SlaveManager.instance.cameraFov = fov;
                em.post(Event.FOV_CHANGED_CMD, this, fov);
            });
    }
}
