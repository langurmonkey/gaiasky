package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Coordinates;

/**
 * Initializes the entities.
 */
public class BaseInitializer implements EntityInitializer {

    private Scene scene;

    public BaseInitializer(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void initializeEntity(Entity entity) {
        Base base = Mapper.base.get(entity);
        if (base.ct == null) {
            base.ct = new ComponentTypes(ComponentType.Others.ordinal());
        }

    }

    @Override
    public void setUpEntity(Entity entity) {
        if (Mapper.coordinates.has(entity)) {
            Coordinates coord = Mapper.coordinates.get(entity);
            if (coord.coordinates != null)
                coord.coordinates.doneLoading(scene, this);
        }
    }
}
