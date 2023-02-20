package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

public class VRDeviceInitializer extends AbstractInitSystem {
    private static final Log logger = Logger.getLogger(VRDeviceInitializer.class);

    public VRDeviceInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        if (Mapper.vr.has(entity)) {
            initializeVRDevice(entity);
        }

        if (Mapper.tagVRUI.has(entity)) {
            initializeVRUI(entity);
        }

    }

    private void initializeVRUI(Entity entity) {

    }

    private void initializeVRDevice(Entity entity) {
        // VR device.
        var vr = Mapper.vr.get(entity);
        vr.beamP0 = new Vector3();
        vr.beamP1 = new Vector3();

        // Base.
        var base = Mapper.base.get(entity);
        base.setComponentType(ComponentType.Others);

        // Body.
        var body = Mapper.body.get(entity);
        body.color = new float[] { 1f, 0f, 0f };

        // Line.
        var line = Mapper.line.get(entity);
        line.lineWidth = 2f;
        line.renderConsumer = LineEntityRenderSystem::renderVRDevice;

        // Model renderer.
        var model = Mapper.model.get(entity);
        if (vr.device != null) {
            model.model.instance = vr.device.getModelInstance();
        } else {
            logger.error("VR device model has no attached device!");
        }
        if (model.renderConsumer == null) {
            model.renderConsumer = ModelEntityRenderSystem::renderVRDeviceModel;
        }

    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
