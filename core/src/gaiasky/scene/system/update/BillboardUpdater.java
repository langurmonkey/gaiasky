package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;

public class BillboardUpdater extends AbstractUpdateSystem {
    public BillboardUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        // Recompute opacity from fade
        var base = Mapper.base.get(entity);
        var fade = Mapper.fade.get(entity);

    }
}
