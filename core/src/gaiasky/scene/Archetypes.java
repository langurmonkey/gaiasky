/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import gaiasky.util.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Archetypes {
    private static final Logger.Log logger = Logger.getLogger(Archetypes.class);

    /** Archetypes map, links old scene graph model objects to artemis archetypes. **/
    protected Map<String, Archetype> archetypes;
    /** The engine reference. **/
    private Engine engine;

    /**
     * Creates a new archetypes container.
     */
    public Archetypes() {
    }

    /**
     * Initializes the archetypes map with an entry for each model object.
     */
    public void initialize(Engine engine) {
        this.engine = engine;
        this.archetypes = initializeArchetypes();
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
        // Get entity archetype if it exists.
        var base = Mapper.base.get(entity);
        if (base != null && base.archetype != null) {
            return base.archetype;
        }
        // Find match by looking at components.
        Collection<Archetype> archetypes = this.archetypes.values();
        for (Archetype archetype : archetypes) {
            if (archetype.matches(entity)) {
                return archetype;
            }
        }
        return null;
    }

    /**
     * Generates a list of archetype names from the given class names. Each
     * archetype is submitted twice, with and without the (legacy) package name.
     *
     * @param classNames The class names.
     *
     * @return A list of archetype names.
     */
    private String[] modelNames(String... classNames) {
        String[] result = new String[classNames.length * 2];
        int i = 0;
        for (String name : classNames) {
            result[i++] = name;
            result[i++] = modelName(name);
        }
        return result;
    }

    private String modelName(String className) {
        return "gaiasky.scenegraph." + className;
    }

    public Map<String, Archetype> initializeArchetypes() {
        if (engine != null) {
            this.archetypes = new HashMap<>();

            // Load the archetypes from the JSON definition.
            var attributeMapFile = Gdx.files.internal("archetypes/archetypes.json");
            var reader = new JsonReader();
            var root = reader.parse(attributeMapFile);

            if (root.has("archetypes")) {
                var numArchetypes = 0;
                var archetypesElement = root.get("archetypes");
                var archetypeElement = archetypesElement.child;
                while (archetypeElement != null) {
                    // Process component.
                    String[] aliases = archetypeElement.has("aliases") ? archetypeElement.get("aliases").asStringArray() : null;
                    int numNames = aliases != null ? aliases.length + 1 : 1;
                    String[] names = new String[numNames];
                    names[0] = archetypeElement.name;
                    if (aliases != null) {
                        for (int i = 0; i < aliases.length; i++) {
                            names[i + 1] = aliases[i];
                        }
                    }
                    String parent = archetypeElement.getString("parent");
                    String[] componentsString = archetypeElement.get("components").asStringArray();
                    Class<? extends Component>[] components = new Class[componentsString.length];
                    int j = 0;
                    for (String componentString : componentsString) {
                        componentString = componentString.replace("*", "");
                        try {
                            var packageName = componentString.startsWith("Tag") ? "gaiasky.scene.component.tag." : "gaiasky.scene.component.";
                            var clazz = (Class<? extends Component>) Class.forName(packageName + componentString);
                            components[j] = clazz;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        j++;
                    }

                    // Add archetype.
                    addArchetype(names, parent, components);
                    numArchetypes++;
                    // Advance.
                    archetypeElement = archetypeElement.next();
                }
                logger.debug("Initialized " + numArchetypes + " archetypes");
            }
            return archetypes;
        } else {
            throw new RuntimeException("Can't create archetypes: the engine is null!");
        }
    }

    @SafeVarargs
    private void addArchetype(String archetypeName, String parentArchetypeName, Class<? extends Component>... classes) {
        Archetype parent = null;
        if (parentArchetypeName != null && this.archetypes.containsKey(parentArchetypeName)) {
            parent = this.archetypes.get(parentArchetypeName);
        }
        this.archetypes.put(archetypeName, new Archetype(engine, parent, archetypeName, classes));

    }

    @SafeVarargs
    private void addArchetype(String[] archetypeNames, String parentArchetypeName, Class<? extends Component>... classes) {
        for (String archetypeName : archetypeNames) {
            addArchetype(archetypeName, parentArchetypeName, classes);
        }
    }

    @SafeVarargs
    private void addArchetype(String archetypeName, Class<? extends Component>... classes) {
        addArchetype(archetypeName, null, classes);
    }

    @SafeVarargs
    private void addArchetype(String[] archetypeNames, Class<? extends Component>... classes) {
        for (String archetypeName : archetypeNames) {
            addArchetype(archetypeName, classes);
        }
    }

}
