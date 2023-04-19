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

public class AxesUpdater extends AbstractUpdateSystem {
    public static final double LINE_SIZE_RAD = Math.tan(Math.toRadians(2.9));

    public AxesUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var body = Mapper.body.get(entity);
        var axis = Mapper.axis.get(entity);

        var camera = GaiaSky.instance.getICamera();

        body.distToCamera = (float) camera.getPos().lenDouble();
        body.size = (float) (LINE_SIZE_RAD * body.distToCamera) * camera.getFovFactor();

        axis.o.set(camera.getInversePos());
        axis.x.set(axis.b0).scl(body.size).add(axis.o);
        axis.y.set(axis.b1).scl(body.size).add(axis.o);
        axis.z.set(axis.b2).scl(body.size).add(axis.o);

    }
}
