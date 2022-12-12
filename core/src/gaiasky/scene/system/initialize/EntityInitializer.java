package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;

/**
 * Defines the interface for all entity initialization systems.
 */
public interface EntityInitializer {

    /**
     * Contains the initialization of this entity before the scene graph
     * structure has been constructed, or the entity is in the index.
     *
     * @param entity The entity.
     */
    void initializeEntity(Entity entity);

    /**
     * Contains the set up of this entity, after the entity has been
     * added to the scene graph and it is in the index.
     *
     * @param entity The entity.
     */
    void setUpEntity(Entity entity);
}
