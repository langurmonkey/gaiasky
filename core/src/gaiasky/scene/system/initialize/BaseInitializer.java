package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;

/**
 * Initializes the entities.
 */
public class BaseInitializer implements EntityInitializer {

    @Override
    public void initializeEntity(Entity entity) {
        Base base = Mapper.base.get(entity);
        if(base.ct == null) {
            base.ct = new ComponentTypes(ComponentType.Others.ordinal());
        }

    }
}
