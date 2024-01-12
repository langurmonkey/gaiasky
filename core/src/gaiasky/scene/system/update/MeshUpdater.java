/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;

public class MeshUpdater extends AbstractUpdateSystem {

    private final float[] auxArray;

    public MeshUpdater(Family family, int priority) {
        super(family, priority);
        auxArray = new float[3];
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var model = Mapper.model.get(entity);

        // Update light with global position
        var mc = model.model;
        if (mc != null) {
            var directional = mc.dirLight(0);
            if (directional != null) {
                directional.direction.set(1f, 0f, 0f);
                directional.color.set(1f, 1f, 1f, 1f);
            }

            var body = Mapper.body.get(entity);
            var graph = Mapper.graph.get(entity);
            var mesh = Mapper.mesh.get(entity);

            // Update local transform
            float[] trn = graph.translation.valuesf(auxArray);
            graph.localTransform.idt().translate(trn[0], trn[1], trn[2]).scl(body.size).mul(mesh.coordinateSystem);

            // Affine transformations for meshes are already contained in mesh.coordinateSystem.
        }
    }
}
