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

public class VertsInitializer extends AbstractInitSystem {
    public VertsInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var verts = Mapper.verts.get(entity);

        if (Mapper.line.has(entity)) {
            // Polyline.
            var line = Mapper.line.get(entity);

            line.lineWidth = verts.primitiveSize;
            line.renderConsumer = LineEntityRenderSystem::renderPolyline;

        } else {
            // Normal verts (points).

        }

    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
