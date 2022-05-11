package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.tag.TagSetElement;

/**
 * This system initializes orbital element set objects. The most important action
 * it needs to carry out is tag all children elements with a {@link TagSetElement}
 * component.
 */
public class OrbitElementsSetInitializer extends InitSystem {

    public OrbitElementsSetInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
    }

    @Override
    public void setUpEntity(Entity entity) {
        var graph = Mapper.graph.get(entity);
        if (graph != null && graph.children != null) {
            for(Entity child : graph.children) {
                // Tag with set element component
                child.add(new TagSetElement());
            }
        }
    }
}
