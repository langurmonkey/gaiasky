/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;

public class RaymarchingUpdater extends AbstractUpdateSystem {
    public RaymarchingUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    private final double solidAngleThreshold = Math.toRadians(0.001);
    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var rm = Mapper.raymarching.get(entity);

        if (rm != null && rm.raymarchingShader != null) {
            var base = Mapper.base.get(entity);
            var body = Mapper.body.get(entity);

            // Check enable/disable
            if (body.solidAngleApparent > solidAngleThreshold) {
                if (!rm.isOn) {
                    // Turn on
                    EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), true, entity);
                    rm.isOn = true;
                }
            } else {
                if (rm.isOn) {
                    // Turn off
                    EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), false, entity);
                    rm.isOn = false;
                }
            }
        }
    }
}
