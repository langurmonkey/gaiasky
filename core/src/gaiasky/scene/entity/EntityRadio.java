package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.IObserver;

/**
 * The entity radio picks up events associated with an entity
 * and acts.
 */
public abstract class EntityRadio implements IObserver {
    protected Entity entity;

    public EntityRadio(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
