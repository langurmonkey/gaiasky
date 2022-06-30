package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;

/**
 * Updates single particles and star objects.
 */
public class ParticleUpdater extends AbstractUpdateSystem {

    public ParticleUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {

    }
}
