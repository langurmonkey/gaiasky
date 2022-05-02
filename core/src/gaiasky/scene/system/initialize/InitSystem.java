package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

public class InitSystem extends IteratingSystem {
    private Log logger;

    private EntityInitializer entityInitializer;

    public InitSystem(EntityInitializer initializer, Family family, int priority) {
        super(family, priority);
        this.entityInitializer = initializer;
        this.logger = Logger.getLogger(initializer != null ? initializer.getClass() : getClass());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if(entityInitializer != null) {
            entityInitializer.initializeEntity(entity);
        } else {
            logger.warn("Can't initialize: initializer is null");
        }
    }
}
