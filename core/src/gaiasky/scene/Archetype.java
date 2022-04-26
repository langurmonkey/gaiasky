package gaiasky.scene;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import java.util.HashSet;
import java.util.Set;

/**
 * An archetype is a class of {@link Entity} containing a pre-defined set of {@link Component}s for convenient
 * creation and extension.
 */
public class Archetype {

    private Engine engine;
    private Archetype parent;
    private Family family;
    private String name;
    private Set<Class<? extends Component>> components;

    public Archetype(final Engine engine, final Archetype parent, final String name, Class<? extends Component>... componentClasses) {
        this.engine = engine;
        this.parent = parent;
        int lastIndex = name.lastIndexOf('.');
        this.name = lastIndex >= 0 ? name.substring(lastIndex + 1) : name;
        this.components = new HashSet<>(componentClasses.length);
        for (Class<? extends Component> componentClass : componentClasses) {
            this.components.add(componentClass);
        }
        this.family = Family.all(componentClasses).get();
    }

    public Archetype(final Engine engine, final String name, Class<? extends Component>... componentClasses) {
        this(engine, null, name, componentClasses);
    }

    public Family getFamily() {
        return family;
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

    /**
     * Checks whether the given entity matches this archetype.
     *
     * @param entity The entity.
     *
     * @return True if the entity is of this archetype (has the same components), false otherwise.
     */
    public boolean matches(Entity entity) {
        ImmutableArray<Component> entityComponents = entity.getComponents();
        if (entityComponents.size() != components.size()) {
            return false;
        }

        for (Component component : entityComponents) {
            boolean found = false;
            for (Class<? extends Component> componentClass : components) {
                if (component.getClass().equals(componentClass)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return name;
    }
}
