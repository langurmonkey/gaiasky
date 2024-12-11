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
import gaiasky.scene.component.*;
import gaiasky.scene.entity.LightingUtils;
import gaiasky.util.DecalUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

public class ModelUpdater extends AbstractUpdateSystem {

    private final ICamera camera;
    private final Vector3d D32;
    private final Matrix4d MD4;
    private final Quaternion QF;
    private final QuaternionDouble QD;

    public ModelUpdater(Family family,
                        int priority) {
        super(family, priority);
        this.camera = GaiaSky.instance.cameraManager;
        this.D32 = new Vector3d();
        this.QD = new QuaternionDouble();
        this.QF = new Quaternion();
        this.MD4 = new Matrix4d();
    }

    @Override
    protected void processEntity(Entity entity,
                                 float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity,
                             float deltaTime) {
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
            EventManager.publish(Event.SPACECRAFT_INFO, this, engine.yaw % 360, engine.pitch % 360, engine.roll % 360, engine.vel.len(),
                    MotorEngine.thrustFactor[engine.thrustFactorIndex], engine.currentEnginePower, engine.yawp, engine.pitchp, engine.rollp);
        }
    }

    protected void updateLocalTransform(Entity entity,
                                        Body body,
                                        GraphNode graph,
                                        ModelScaffolding scaffolding) {
        setToLocalTransform(entity, body, graph, scaffolding.sizeScaleFactor, graph.localTransform, true);
    }

    public void setToLocalTransform(Entity entity,
                                    Body body,
                                    GraphNode graph,
                                    float sizeFactor,
                                    Matrix4 localTransform,
                                    boolean forceUpdate) {
        setToLocalTransform(entity, body, graph, body.size, sizeFactor, localTransform, forceUpdate);
    }

    public void setToLocalTransform(Entity entity,
                                    Body body,
                                    GraphNode graph,
                                    float size,
                                    float sizeFactor,
                                    Matrix4 localTransform,
                                    boolean forceUpdate) {
        // Update translation, orientation and local transform.
        ITimeFrameProvider time = GaiaSky.instance.time;

        // Helio-tropic satellites need this chunk before the actual update is carried out.
        var orientation = Mapper.orientation.get(entity);
        if (Mapper.tagHeliotropic.has(entity)
                && orientation != null
                && orientation.attitudeComponent != null
                && (time.getHdiff() != 0 || forceUpdate)) {
            var quaternionOrientation = orientation.attitudeComponent;
            if (quaternionOrientation.nonRotatedPos != null) {
                quaternionOrientation.nonRotatedPos.set(body.pos);
                // Undo rotation.
                quaternionOrientation.nonRotatedPos.mul(Coordinates.eqToEcl())
                        .rotate(-AstroUtils.getSunLongitude(time.getTime()) - 180, 0, 1, 0);
                // Update attitude from server if needed.
                if (quaternionOrientation.orientationServer != null) {
                    quaternionOrientation.orientationServer.updateOrientation(time.getTime());
                }
            }
        }

        if (sizeFactor != 1 || forceUpdate) {
            var scaffolding = Mapper.modelScaffolding.get(entity);
            var quaternionOrientation = orientation != null ? orientation.attitudeComponent : null;
            var rigidRotation = orientation != null ? orientation.rotationComponent : null;

            // Update quaternion orientation if needed.
            if (quaternionOrientation != null) {
                quaternionOrientation.updateOrientation(time.getTime());
            }

            // Do actual update.
            if (Mapper.tagBillboard.has(entity)) {
                // Billboard orientation computation.
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
            } else if (quaternionOrientation != null) {
                // Satellites have quaternion orientations, typically.

                graph.translation.setToTranslation(localTransform).scl(size * sizeFactor);
                var hasOrientationServer = quaternionOrientation.orientationServer != null
                        && quaternionOrientation.orientationServer.hasOrientation();
                if (hasOrientationServer) {
                    QD.set(quaternionOrientation.getCurrentQuaternion());
                    QF.set((float) QD.x, (float) QD.y, (float) QD.z, (float) QD.w);
                } else {
                    // Use solar longitude.
                    QD.setFromAxis(0, 1, 0, AstroUtils.getSunLongitude(GaiaSky.instance.time.getTime()));
                }

                // Update orientation
                graph.orientation.idt().rotate(QD);
                if (hasOrientationServer) {
                    graph.orientation.rotate(0, 0, 1, 180);
                }

                MD4.set(localTransform).mul(graph.orientation);
                MD4.putIn(localTransform);

            } else if (rigidRotation != null) {
                // Planets and moons have rotation components
                graph.translation.setToTranslation(localTransform)
                        .scl(size * sizeFactor)
                        .rotate(0, 1, 0, (float) rigidRotation.ascendingNode)
                        .mul(Coordinates.getTransformF(scaffolding.refPlaneTransform))
                        .rotate(0, 0, 1, (float) (rigidRotation.inclination + rigidRotation.axialTilt))
                        .rotate(0, 1, 0, (float) rigidRotation.angle);
                graph.orientation.idt().rotate(0, 1, 0, (float) rigidRotation.ascendingNode)
                        .mul(Coordinates.getTransformD(scaffolding.refPlaneTransform))
                        .rotate(0, 0, 1, (float) (rigidRotation.inclination + rigidRotation.axialTilt));
            } else {
                // The rest of bodies are just sitting there, in their reference system
                graph.translation.setToTranslation(localTransform)
                        .scl(size * sizeFactor)
                        .mul(Coordinates.getTransformF(scaffolding.refPlaneTransform));
                graph.orientation.idt()
                        .mul(Coordinates.getTransformD(scaffolding.refPlaneTransform));
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
