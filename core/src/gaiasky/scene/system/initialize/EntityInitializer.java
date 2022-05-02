package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;

/**
 * Defines the interface for all entity initialization systems.
 */
public interface EntityInitializer {

    void initializeEntity(Entity entity);
}
