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
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

public class VRDeviceUpdater extends AbstractUpdateSystem {

    public VRDeviceUpdater(Family family,
                           int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity,
                                 float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    private final Vector3d aux = new Vector3d();
    private final Matrix4d deviceTransform = new Matrix4d();

    @Override
    public void updateEntity(Entity entity,
                             float deltaTime) {
        // The numbers in the beams should not depend on internal units!
        var vr = Mapper.vr.get(entity);
        vr.beamP0.set(0, -0.01f, 0);
        vr.beamP1.set(0, (float) (-0.42), (float) (-0.6));
        vr.beamP2.set(0, (float) (-700), (float) (-1000));
        vr.beamPn.set(0, (float) (-7000), (float) (-10000));

        if (vr.hitUI) {
            if (vr.interacting) {
                // Shorten beam to intersection point.
                vr.beamP1.set(vr.intersection);
                vr.beamP2.set(vr.intersection);
            } else {
                // Cut beam completely.
                aux.set(vr.beamP1).sub(vr.beamP0).nor().scl(0.001);
                vr.beamP1.set(vr.beamP0).add(aux);

                aux.set(vr.beamP2).sub(vr.beamP0).nor().scl(0.002);
                vr.beamP2.set(vr.beamP0).add(aux);
            }
        }

        if (vr.device.isActive()) {
            // Set model to device transform.
            deviceTransform.set(vr.device.aimTransform);
            vr.beamP0.mul(deviceTransform);
            if(!vr.hitUI || !vr.interacting) {
                vr.beamP1.mul(deviceTransform);
                vr.beamP2.mul(deviceTransform);
            }
            vr.beamPn.mul(deviceTransform);
        }

        // Intersection sphere.
        if (vr.intersection != null) {
            vr.intersectionModel.transform.idt().translate((float) vr.intersection.x, (float) vr.intersection.y, (float) vr.intersection.z).scale(0.01f, 0.01f, 0.01f);
        }

    }
}
