/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;

public class BillboardSetUpdater extends AbstractUpdateSystem {

    private final Matrix4 M41;

    public BillboardSetUpdater(Family family, int priority) {
        super(family, priority);
        M41 = new Matrix4();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var graph = Mapper.graph.get(entity);
        var body = Mapper.body.get(entity);
        var transform = Mapper.transform.get(entity);

        graph.translation.setToTranslation(graph.localTransform).scl(body.size);
        if (transform.matrix != null)
            graph.localTransform.mul(transform.matrix.putIn(M41));
    }
}
