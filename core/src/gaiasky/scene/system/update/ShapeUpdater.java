/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.LightingUtils;

public class ShapeUpdater extends AbstractUpdateSystem {

    private final Vector3 F31 = new Vector3();

    public ShapeUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var shape = Mapper.shape.get(entity);
        var model = Mapper.model.get(entity);
        var coord = Mapper.coordinates.get(entity);
        var transform = Mapper.transform.get(entity);
        var affine = Mapper.affine.get(entity);

        if (model.model != null && !model.model.isStaticLight()) {
            // Update light with global position
            LightingUtils.updateLights(model, body, graph, GaiaSky.instance.cameraManager);
        }

        // Compute local transform.
        graph.localTransform.idt().translate(graph.translation.put(F31)).scl(body.size);
        // Apply reference system transform.
        if (transform.matrixf != null) {
            graph.localTransform.mul(transform.matrixf);
        }
        // Apply affine transformations.
        if (affine.transformations != null && !affine.transformations.isEmpty()) {
            affine.apply(graph.localTransform);
        }

    }
}
