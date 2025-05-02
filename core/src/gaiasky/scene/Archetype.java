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
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An archetype is a blueprint to build an object instance in the ECS. It maintains a set with the component classes
 * of the archetype and a reference to a possible parent.
 */
public class Archetype {

    private final Engine engine;
    private final Archetype parent;
    private final Family family;
    private final String name;
    private final Set<Class<? extends Component>> components;

    @SafeVarargs
    public Archetype(final Engine engine, final Archetype parent, final String name, Class<? extends Component>... componentClasses) {
        this.engine = engine;
        this.parent = parent;
        int lastIndex = name.lastIndexOf('.');
        this.name = lastIndex >= 0 ? name.substring(lastIndex + 1) : name;
        this.components = new HashSet<>(componentClasses.length);
        Collections.addAll(this.components, componentClasses);
        this.family = Family.all(componentClasses).get();
    }

    @SafeVarargs
    public Archetype(final Engine engine, final String name, Class<? extends Component>... componentClasses) {
        this(engine, null, name, componentClasses);
    }

    public Family getFamily() {
        return family;
    }

    public Entity createEntity() {

        Entity entity = engine.createEntity();
        addComponentsRecursive(entity);

        // Set archetype reference.
        if (Mapper.base.has(entity)) {
            Mapper.base.get(entity).archetype = this;
        }

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

    public int numComponents() {
        int n = 0;
        Archetype current = this;

        while (current != null) {
            n += current.components.size();
            current = current.parent;
        }
        return n;
    }

    public boolean hasComponent(Component component) {
        Archetype current = this;

        while (current != null) {
            for (Class<? extends Component> componentClass : current.components) {
                if (component.getClass().equals(componentClass)) {
                    return true;
                }
            }
            current = current.parent;
        }

        return false;

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
        if (entityComponents.size() != numComponents()) {
            return false;
        }

        for (Component component : entityComponents) {
            if (!hasComponent(component)) {
                return false;
            }
        }
        return true;
    }

    public String getName() {
        return name;
    }
}
