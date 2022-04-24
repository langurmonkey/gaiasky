package gaiasky.scene;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import java.util.HashSet;
import java.util.Set;

/**
 * An archetype is a class of entity containing a pre-defined set of components for easy
 * creation and extension.
 */
public class Archetype {

    private Engine engine;
    private Archetype parent;
    private Set<Class<? extends Component>> components;

    public Archetype(final Engine engine, final Archetype parent, Class<? extends Component>... componentClasses) {
        this.engine = engine;
        this.parent = parent;
        this.components = new HashSet<>(componentClasses.length);
        for (Class<? extends Component> componentClass : componentClasses) {
            this.components.add(componentClass);
        }

    }

    public Archetype(final Engine engine, Class<? extends Component>... componentClasses) {
        this(engine, null, componentClasses);
    }

    public Entity createEntity() {
        Entity entity = engine.createEntity();
        addComponentsRecursive(entity);
        return entity;
    }

    public void addComponentsRecursive(Entity entity) {
        if (parent != null) {
            parent.addComponentsRecursive(entity);
        }
        for (Class<? extends Component> component : components) {
            try {
                Component c = ClassReflection.newInstance(component);
                entity.add(c);
            } catch (ReflectionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
