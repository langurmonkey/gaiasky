package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;

/**
 * Initializes the entities.
 */
public class BaseInitializationSystem extends IteratingSystem {

    public BaseInitializationSystem(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Base base = Mapper.base.get(entity);
        if(base.ct == null) {
            base.ct = new ComponentTypes(ComponentType.Others.ordinal());
        }
    }
}
