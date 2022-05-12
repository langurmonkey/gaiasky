package gaiasky.scene;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import gaiasky.scenegraph.Star;

import java.util.Collection;
import java.util.Map;

/**
 * A container for data and logic concerning {@link Archetype}s.
 */
public class Archetypes {

    /** Archetypes map, links old scene graph model objects to artemis archetypes. **/
    protected Map<String, Archetype> archetypes;

    /**
     * Creates a new archetypes container.
     */
    public Archetypes() {
    }

    /**
     * Initializes the archetypes map with an entry for each model object.
     */
    public void initialize(Engine engine) {
        this.archetypes = (new ArchetypeInitializer(engine)).initializeArchetypes();
    }

    public boolean contains(String key) {
        return archetypes.containsKey(key);
    }

    /**
     * Gets an archetype by name (key).
     *
     * @param key The name of the archetype.
     *
     * @return The archetype.
     */
    public Archetype get(String key) {
        return archetypes.get(key);
    }

    /**
     * Gets an archetype by class.
     *
     * @param archetypeClass The class of the archetype.
     *
     * @return The archetype.
     */
    public Archetype get(Class archetypeClass) {
        return archetypes.get(archetypeClass.getName());
    }

    /**
     * Finds a matching archetype given an entity.
     *
     * @param entity The entity.
     *
     * @return The matching archetype if it exists, or null if it does not.
     */
    public Archetype findArchetype(Entity entity) {
        Collection<Archetype> archetypes = this.archetypes.values();
        for (Archetype archetype : archetypes) {
            if (archetype.matches(entity)) {
                return archetype;
            }
        }
        return null;
    }
}
