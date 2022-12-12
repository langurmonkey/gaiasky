package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;

public class VertsUpdater extends AbstractUpdateSystem {
    public VertsUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var graph = Mapper.graph.get(entity);
        graph.translation.setToTranslation(graph.localTransform);
    }
}
