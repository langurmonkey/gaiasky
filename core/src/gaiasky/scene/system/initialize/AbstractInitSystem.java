package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

/**
 * An abstract initialization system, providing some common utilities useful for concrete
 * initializers.
 */
public abstract class AbstractInitSystem extends IteratingSystem implements EntityInitializer {
    protected Log logger;

    protected Engine engineBackup;

    private enum InitializerMode {
        /** The initialization stage happens at the beginning, right after the entity has been created. **/
        INIT,
        /** The set-up stage happens after all assets and resources have been loaded. **/
        SETUP
    }

    private InitializerMode mode;

    public AbstractInitSystem(boolean setUp, Family family, int priority) {
        super(family, priority);
        this.mode = setUp ? InitializerMode.SETUP : InitializerMode.INIT;
        this.logger = Logger.getLogger(getClass());
    }

    public AbstractInitSystem(Family family, int priority) {
        // Initialize by default
        this(false, family, priority);
    }

    public void setModeInit() {
        this.mode = InitializerMode.INIT;
    }

    public void setModeSetUp() {
        this.mode = InitializerMode.SETUP;
    }

    public boolean isModeInit() {
        return this.mode != null && this.mode == InitializerMode.INIT;
    }

    public boolean isModeSetUp() {
        return this.mode != null && this.mode == InitializerMode.SETUP;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        switch (mode) {
        case INIT -> initializeEntity(entity);
        case SETUP -> setUpEntity(entity);
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public void addedToEngine(Engine engine) {
        super.addedToEngine(engine);
        this.engineBackup = engine;
    }
}
