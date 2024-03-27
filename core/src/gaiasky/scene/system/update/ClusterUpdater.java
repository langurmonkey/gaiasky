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
import gaiasky.scene.Mapper;
import gaiasky.util.math.MathUtilsDouble;

public class ClusterUpdater extends AbstractUpdateSystem {

    private final Vector3 F31;

    public ClusterUpdater(Family family, int priority) {
        super(family, priority);
        F31 = new Vector3();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        if (base.opacity > 0) {
            var body = Mapper.body.get(entity);
            var graph = Mapper.graph.get(entity);
            var cluster = Mapper.cluster.get(entity);
            var sa = Mapper.sa.get(entity);

            base.opacity *= 0.1f * base.getVisibilityOpacityFactor();
            cluster.fadeAlpha = (float) MathUtilsDouble.flint(body.solidAngleApparent, sa.thresholdPoint, sa.thresholdQuad, 0f, 1f);
            body.labelColor[3] = 8.0f * cluster.fadeAlpha;

            // Compute local transform.
            graph.localTransform.idt().translate(graph.translation.put(F31)).scl(body.size);
        }
    }
}
