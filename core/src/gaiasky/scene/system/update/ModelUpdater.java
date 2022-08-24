package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.AffineTransformations;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.ModelScaffolding;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.component.ITransform;
import gaiasky.scenegraph.component.RotationComponent;
import gaiasky.util.Constants;
import gaiasky.util.DecalUtils;
import gaiasky.util.camera.Proximity;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.*;
import gaiasky.util.time.ITimeFrameProvider;

import java.util.Date;

/**
 * Updates model objects.
 */
public class ModelUpdater extends AbstractUpdateSystem {

    // At what distance the light has the maximum intensity
    private static final double LIGHT_X0 = 0.1 * Constants.AU_TO_U;
    // At what distance the light is 0
    private static final double LIGHT_X1 = 5e4 * Constants.AU_TO_U;

    private ICamera camera;
    private Vector3 F31;
    private Vector3d D32;
    private Matrix4d MD4;
    private Quaternion QF;
    private Quaterniond QD;

    public ModelUpdater(Family family, int priority) {
        super(family, priority);
        this.camera = GaiaSky.instance.cameraManager;
        this.F31 = new Vector3();
        this.D32 = new Vector3d();
        this.QF = new Quaternion();
        this.QD = new Quaterniond();
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

        // Update light with global position
        if (model.model != null && body.distToCamera <= LIGHT_X1) {
            for (int i = 0; i < Constants.N_DIR_LIGHTS; i++) {
                IFocus lightSource = camera.getCloseLightSource(i);
                if (lightSource != null) {
                    if (lightSource instanceof Proximity.NearbyRecord) {
                        graph.translation.put(model.model.directional(i).direction);
                        Proximity.NearbyRecord nr = (Proximity.NearbyRecord) lightSource;
                        if (nr.isStar() || nr.isStarGroup()) {
                            float[] col = nr.getColor();
                            double closestDist = nr.getClosestDistToCamera();
                            float colFactor = (float) Math.pow(MathUtilsd.lint(closestDist, LIGHT_X0, LIGHT_X1, 1.0, 0.0), 2.0);
                            model.model.directional(i).direction.sub(nr.pos.put(F31));
                            model.model.directional(i).color.set(col[0] * colFactor, col[1] * colFactor, col[2] * colFactor, colFactor);
                        } else {
                            Vector3b campos = camera.getPos();
                            model.model.directional(i).direction.add(campos.x.floatValue(), campos.y.floatValue(), campos.z.floatValue());
                            model.model.directional(i).color.set(1f, 1f, 1f, 1f);
                        }
                    }
                } else {
                    // Disable light
                    model.model.directional(i).color.set(0f, 0f, 0f, 0f);
                }
            }
        }
        updateLocalTransform(entity, body, graph, scaffolding);

        // Atmosphere and cloud
        if (atmosphere != null && atmosphere.atmosphere != null) {
            atmosphere.atmosphere.update(graph.translation);
        }
        if (cloud != null && cloud.cloud != null) {
            cloud.cloud.update(graph.translation);
            setToLocalTransform(entity, body, graph, cloud.cloud.size, 1, cloud.cloud.localTransform, true);
        }
        if (engine != null && engine.render) {
            EventManager.publish(Event.SPACECRAFT_INFO, this, engine.yaw % 360, engine.pitch % 360, engine.roll % 360, engine.vel.len(), engine.thrustFactor[engine.thrustFactorIndex], engine.currentEnginePower, engine.yawp, engine.pitchp, engine.rollp);
        }
    }

    protected void updateLocalTransform(Entity entity, Body body, GraphNode graph, ModelScaffolding scaffolding) {
        setToLocalTransform(entity, body, graph, scaffolding.sizeScaleFactor, graph.localTransform, true);
    }

    public void setToLocalTransform(Entity entity, Body body, GraphNode graph, float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        setToLocalTransform(entity, body, graph, body.size, sizeFactor, localTransform, forceUpdate);
    }

    public void setToLocalTransform(Entity entity, Body body, GraphNode graph, float size, float sizeFactor, Matrix4 localTransform, boolean forceUpdate) {
        // Update translation, orientation and local transform.
        ITimeFrameProvider time = GaiaSky.instance.time;
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
                } catch(RuntimeException e) {
                    // Non-invertible matrix
                }
                localTransform.scale(size, size, size);

                // Rotation for attitude indicator
                engine.rotationMatrix.idt().setToLookAt(engine.directionf, engine.upf);
                engine.rotationMatrix.getRotation(engine.qf);
            } else if (Mapper.attitude.has(entity)) {
                // Satellites have attitude.
                var attitude = Mapper.attitude.get(entity);

                // Update attitude for current time if needed.
                if (time.getHdiff() != 0) {
                    if (Mapper.tagHeliotropic.has(entity)) {
                        attitude.nonRotatedPos.set(body.pos);
                        // Undo rotation.
                        attitude.nonRotatedPos.mul(Coordinates.eqToEcl()).rotate(-AstroUtils.getSunLongitude(time.getTime()) - 180, 0, 1, 0);
                        // Update attitude from server if needed.
                        if (attitude.attitudeServer != null) {
                            attitude.attitude = attitude.attitudeServer.getAttitude(new Date(time.getTime().toEpochMilli()));
                        }
                    }
                }

                graph.translation.setToTranslation(localTransform).scl(size * sizeFactor);
                if (attitude.attitude != null) {
                    QD = attitude.attitude.getQuaternion();
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
        AffineTransformations affine = Mapper.affine.get(entity);
        if (affine != null && affine.transformations != null)
            for (ITransform tc : affine.transformations)
                tc.apply(localTransform);
    }
}
