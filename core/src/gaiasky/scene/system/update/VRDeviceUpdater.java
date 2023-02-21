package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.scene.Mapper;
import gaiasky.util.Constants;

public class VRDeviceUpdater extends AbstractUpdateSystem {

    public VRDeviceUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
       updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var model = Mapper.model.get(entity);
        var vr = Mapper.vr.get(entity);
        Matrix4 transform = model.model.instance.transform;
        vr.beamP0.set(0, -0.01f, 0).mul(transform);
        vr.beamP1.set(0, (float) -(Constants.MPC_TO_U - Constants.PC_TO_U), (float) -Constants.MPC_TO_U).mul(transform);
    }
}
