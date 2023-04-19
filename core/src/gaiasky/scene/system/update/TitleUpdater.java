/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;

public class TitleUpdater extends AbstractUpdateSystem {
    public TitleUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var body = Mapper.body.get(entity);

        var camera = GaiaSky.instance.getICamera();

        // Propagate upwards if necessary
        setParentOpacity(entity);

        body.solidAngle = 80f;
        body.solidAngleApparent = body.solidAngle / camera.getFovFactor();

    }

    protected void setParentOpacity(Entity entity) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);

        if (base.opacity > 0 && Mapper.title.has(graph.parent)) {
            // If our parent is a Text2D, we update its opacity

            Entity parent = graph.parent;
            var parentBase = Mapper.base.get(parent);
            parentBase.opacity *= (1 - base.opacity);
            setParentOpacity(parent);
        }
    }
}
