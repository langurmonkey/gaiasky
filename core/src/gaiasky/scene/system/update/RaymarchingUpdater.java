package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;

public class RaymarchingUpdater extends AbstractUpdateSystem {
    public RaymarchingUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var rm = Mapper.raymarching.get(entity);

        if (rm != null && rm.raymarchingShader != null) {
            var base = Mapper.base.get(entity);
            var body = Mapper.body.get(entity);

            // Check enable/disable
            if (body.solidAngleApparent > Math.toRadians(0.001)) {
                if (!rm.isOn) {
                    // Turn on
                    EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), true, body.pos);
                    rm.isOn = true;
                }
            } else {
                if (rm.isOn) {
                    // Turn off
                    EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), false, body.pos);
                    rm.isOn = false;
                }
            }
        }
    }
}
