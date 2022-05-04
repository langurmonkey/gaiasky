package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

public class InitSystem extends IteratingSystem {
    private Log logger;

    private EntityInitializer entityInitializer;

    private enum InitializerMode {
        /** The initialization stage happens at the beginning, right after the entity has been created. **/
        INIT,
        /** The set-up stage happens after all assets and resources have been loaded. **/
        SETUP
    }

    private InitializerMode mode;

    public InitSystem(EntityInitializer initializer, boolean setUp, Family family, int priority) {
        super(family, priority);
        this.entityInitializer = initializer;
        this.mode = setUp ? InitializerMode.SETUP : InitializerMode.INIT;
        this.logger = Logger.getLogger(initializer != null ? initializer.getClass() : getClass());
    }

    public InitSystem(EntityInitializer initializer, Family family, int priority) {
        // Initialize by default
        this(initializer, false, family, priority);
    }

    public void setModeInit() {
        this.mode = InitializerMode.INIT;
    }

    public void setModeSetUp() {
        this.mode = InitializerMode.SETUP;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (entityInitializer != null) {
            switch (mode) {
            case INIT -> entityInitializer.initializeEntity(entity);
            case SETUP -> entityInitializer.setUpEntity(entity);
            }
        } else {
            logger.warn("Can't initialize: initializer is null");
        }
    }
}
