/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;

public class BoundariesInitializer extends AbstractInitSystem {
    public BoundariesInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var line = Mapper.line.get(entity);
        // Lines.
        line.lineWidth = 1;
        line.renderConsumer = LineEntityRenderSystem::renderConstellationBoundaries;
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
