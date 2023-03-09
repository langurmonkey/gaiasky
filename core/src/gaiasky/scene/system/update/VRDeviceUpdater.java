package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.util.Constants;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

public class VRDeviceUpdater extends AbstractUpdateSystem {

    public VRDeviceUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    private final Vector3d aux = new Vector3d();
    private final Matrix4d deviceTransform = new Matrix4d();

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        // The numbers in the beams should not depend on internal units!
        var model = Mapper.model.get(entity);
        var vr = Mapper.vr.get(entity);
        vr.beamP0.set(0, -0.01f, 0);
        vr.beamP1.set(0, (float) (-0.42 ), (float) (-0.6));
        vr.beamP2.set(0, (float) (-700), (float) (-1000));
        vr.beamPn.set(0, (float) (-70000), (float) (-100000));
        if (vr.hitUI) {
            if (vr.interacting) {
                // Shorten beam.
                aux.set(vr.beamP1).sub(vr.beamP0).nor().scl(0.2);
                vr.beamP1.set(vr.beamP0).add(aux);

                aux.set(vr.beamP2).sub(vr.beamP0).nor().scl(0.5);
                vr.beamP2.set(vr.beamP0).add(aux);
            } else {
                // Cut beam completely.
                aux.set(vr.beamP1).sub(vr.beamP0).nor().scl(0.001);
                vr.beamP1.set(vr.beamP0).add(aux);

                aux.set(vr.beamP2).sub(vr.beamP0).nor().scl(0.002);
                vr.beamP2.set(vr.beamP0).add(aux);
            }
        }

        if (vr.device.transform != null) {
            // Set model to device transform.
            if (model.model != null && model.model.instance != null) {
                //model.model.instance.transform.set(vr.device.transform);
            }
            deviceTransform.set(model.model.instance.transform);
            vr.beamP0.mul(deviceTransform);
            vr.beamP1.mul(deviceTransform);
            vr.beamP2.mul(deviceTransform);
            vr.beamPn.mul(deviceTransform);
        }

    }
}
