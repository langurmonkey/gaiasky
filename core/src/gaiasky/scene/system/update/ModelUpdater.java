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
import com.badlogic.gdx.math.Quaternion;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.ModelScaffolding;
import gaiasky.scene.component.MotorEngine;
import gaiasky.scene.entity.LightingUtils;
import gaiasky.scene.record.RotationComponent;
import gaiasky.util.DecalUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.Date;

public class ModelUpdater extends AbstractUpdateSystem {

    private final ICamera camera;
    private final Vector3d D32;
    private final Matrix4d MD4;
    private final Quaternion QF;
    private final QuaternionDouble QD;

    public ModelUpdater(Family family, int priority) {
        super(family, priority);
        this.camera = GaiaSky.instance.cameraManager;
        this.D32 = new Vector3d();
        this.QD = new QuaternionDouble();
        this.QF = new Quaternion();
        this.MD4 = new Matrix4d();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var body = Mapper.body.get(entity);
        var model = Mapper.model.get(entity);
        var graph = Mapper.graph.get(entity);
        var scaffolding = Mapper.modelScaffolding.get(entity);
        var atmosphere = Mapper.atmosphere.get(entity);
        var cloud = Mapper.cloud.get(entity);
        var engine = Mapper.engine.get(entity);

        // Update light with global position.
        LightingUtils.updateLights(model, body, graph, camera);
        updateLocalTransform(entity, body, graph, scaffolding);

        // Atmosphere and cloud.
        if (atmosphere != null && atmosphere.atmosphere != null) {
            atmosphere.atmosphere.update(graph.translation);
        }
        if (cloud != null && cloud.cloud != null) {
            cloud.cloud.update(graph.translation);
            setToLocalTransform(entity, body, graph, cloud.cloud.size, 1, cloud.cloud.localTransform, true);
        }
        if (engine != null && engine.render) {
            EventManager.publish(Event.SPACECRAFT_INFO, this, engine.yaw % 360, engine.pitch % 360, engine.roll % 360, engine.vel.len(), MotorEngine.thrustFactor[engine.thrustFactorIndex], engine.currentEnginePower, engine.yawp, engine.pitchp, engine.rollp);
        }
    }

    int lastLevel = -1;

    protected void updateLocalTransform(Entity entity, Body body, GraphNode graph, ModelScaffolding scaffolding) {
        setToLocalTransform(entity, body, graph, scaffolding.sizeScaleFactor, graph.localTransform, true);
    }

    public void setToLocalTransform(Entity entity, Body body, GraphNode graph, float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        setToLocalTransform(entity, body, graph, body.size, sizeFactor, localTransform, forceUpdate);
    }

    public void setToLocalTransform(Entity entity, Body body, GraphNode graph, float size, float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        // Update translation, orientation and local transform.
        ITimeFrameProvider time = GaiaSky.instance.time;

        // Heliotropic satellites need this chunk before the actual update is carried out.
        var attitude = Mapper.attitude.get(entity);
        if (attitude != null && (time.getHdiff() != 0 || forceUpdate)) {
            if (Mapper.tagHeliotropic.has(entity) && attitude.nonRotatedPos != null) {
                attitude.nonRotatedPos.set(body.pos);
                // Undo rotation.
                attitude.nonRotatedPos.mul(Coordinates.eqToEcl()).rotate(-AstroUtils.getSunLongitude(time.getTime()) - 180, 0, 1, 0);
                // Update attitude from server if needed.
                if (attitude.attitudeServer != null) {
                    attitude.attitude = attitude.attitudeServer.getAttitude(new Date(time.getTime().toEpochMilli()));
                }
            }
        }

        if (sizeFactor != 1 || forceUpdate) {
            var rotation = Mapper.rotation.get(entity);
            var scaffolding = Mapper.modelScaffolding.get(entity);
            if (Mapper.tagQuatOrientation.has(entity)) {
                // Billboards use quaternion orientation.
                DecalUtils.setBillboardRotation(QF, body.pos.put(D32).nor(), new Vector3d(0, 1, 0));
                graph.translation.setToTranslation(localTransform).scl(size).rotate(QF);
            } else if (Mapper.engine.has(entity)) {
                // Spacecraft.
                var engine = Mapper.engine.get(entity);

                // Spacecraft
                localTransform.idt().setToLookAt(engine.posf, engine.directionf.add(engine.posf), engine.upf);
                try {
                    localTransform.inv();
                } catch (RuntimeException e) {
                    // Non-invertible matrix
                }
                localTransform.scale(size, size, size);

                // Rotation for attitude indicator
                engine.rotationMatrix.idt().setToLookAt(engine.directionf, engine.upf);
                if (engine.qf != null) {
                    engine.rotationMatrix.getRotation(engine.qf);
                }
            } else if (attitude != null) {
                // Satellites have attitude.

                graph.translation.setToTranslation(localTransform).scl(size * sizeFactor);
                if (attitude.attitude != null) {
                    QD.set(attitude.attitude.getQuaternion());
                    QF.set((float) QD.x, (float) QD.y, (float) QD.z, (float) QD.w);
                } else {
                    QD.setFromAxis(0, 1, 0, AstroUtils.getSunLongitude(GaiaSky.instance.time.getTime()));
                }

                // Update orientation
                graph.orientation.idt().rotate(QD);
                if (attitude.attitude != null) {
                    graph.orientation.rotate(0, 0, 1, 180);
                }

                MD4.set(localTransform).mul(graph.orientation);
                MD4.putIn(localTransform);

            } else if (rotation.rc != null) {
                // Planets and moons have rotation components
                RotationComponent rc = rotation.rc;
                graph.translation.setToTranslation(localTransform).scl(size * sizeFactor).mul(Coordinates.getTransformF(scaffolding.refPlaneTransform)).rotate(0, 1, 0, (float) rc.ascendingNode).rotate(0, 0, 1, (float) (rc.inclination + rc.axialTilt)).rotate(0, 1, 0, (float) rc.angle);
                graph.orientation.idt().mul(Coordinates.getTransformD(scaffolding.refPlaneTransform)).rotate(0, 1, 0, (float) rc.ascendingNode).rotate(0, 0, 1, (float) (rc.inclination + rc.axialTilt));
            } else {
                // The rest of bodies are just sitting there, in their reference system
                graph.translation.setToTranslation(localTransform).scl(size * sizeFactor).mul(Coordinates.getTransformF(scaffolding.refPlaneTransform));
                graph.orientation.idt().mul(Coordinates.getTransformD(scaffolding.refPlaneTransform));
            }
        } else {
            // Nothing whatsoever
            localTransform.set(graph.localTransform);
        }

        // Apply transformations
        var affine = Mapper.affine.get(entity);
        affine.apply(localTransform);
    }
}
