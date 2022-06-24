package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.initialize.ElementsSetInitializer;

public class ElementsSetRadio extends EntityRadio {

    private ElementsSetInitializer initializer;

    public ElementsSetRadio(Entity entity, ElementsSetInitializer initializer) {
        super(entity);
        this.initializer = initializer;
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.GPU_DISPOSE_ORBITAL_ELEMENTS) {
            if (source == entity) {
                initializer.initializeOrbitsWithOrbit(Mapper.graph.get(entity), Mapper.orbitElementsSet.get(entity));
            }
        }
    }
}
