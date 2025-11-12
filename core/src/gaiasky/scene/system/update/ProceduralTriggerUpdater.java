/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.util.Logger;

public class ProceduralTriggerUpdater extends AbstractUpdateSystem {
    protected static final Logger.Log logger = Logger.getLogger(ProceduralTriggerUpdater.class);

    private final Scene scene;

    public ProceduralTriggerUpdater(Scene scene, Family family, int priority) {
        super(family, priority);
        this.scene = scene;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var trigger = Mapper.trigger.get(entity);

        if (trigger.proceduralGeneration &&
                trigger.billboardGroup == null &&
                body.distToCamera < body.size * 70) {

            logger.info("Procedurally generating galaxy: " + base.getName());

            var pair = GaiaSky.instance.scripting().apiv2().scene.createNewProceduralGalaxy(base.getName() + " procedural",
                                                                                            body.size / 2.0,
                                                                                            body.pos);
            var entityFull = pair.getFirst();
            var entityHalf = pair.getSecond();
            // Add to scene.
            GaiaSky.postRunnable(() -> {
                scene.initializeEntity(entityFull);
                scene.initializeEntity(entityHalf);
                scene.setUpEntity(entityFull);
                scene.setUpEntity(entityHalf);
                EventManager.instance.post(Event.SCENE_ADD_OBJECT_CMD, this, entityFull, true);
                trigger.billboardGroup = entityFull;
            });
        }
    }
}
