/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.Mapper;

public class ElementsSetExtractor extends AbstractExtractSystem {

    private final TrajectoryExtractor trajectoryExtractor;
    private final ModelExtractor modelExtractor;

    public ElementsSetExtractor(Family family, int priority) {
        super(family, priority);
        trajectoryExtractor = new TrajectoryExtractor(null, 0);
        modelExtractor = new ModelExtractor(null, 0);
    }

    @Override
    public void setRenderer(ISceneRenderer renderer) {
        super.setRenderer(renderer);
        trajectoryExtractor.setRenderer(renderer);
        modelExtractor.setRenderer(renderer);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);

        if (mustRender(base)) {
            var render = Mapper.render.get(entity);
            addToRender(render, RenderGroup.ORBITAL_ELEMENTS_GROUP);
        }

        if (graph.children != null) {
            var set = Mapper.orbitElementsSet.get(entity);
            for (int i = 0; i < set.alwaysUpdate.size; i++) {
                Entity child = set.alwaysUpdate.get(i);
                extractSingle(child);
            }
        }
    }

    private void extractSingle(Entity entity) {
        if (Mapper.trajectory.has(entity)) {
            trajectoryExtractor.extractEntity(entity);
        }
        if (Mapper.model.has(entity)) {
            modelExtractor.extractEntity(entity);
        }
    }
}
