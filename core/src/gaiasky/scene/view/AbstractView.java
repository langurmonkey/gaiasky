/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;

public sealed abstract class AbstractView permits BaseView, PositionView {

    /** A reference to the entity. **/
    protected Entity entity;

    /** Creates an empty abstract view without entity. **/
    protected AbstractView() {
    }

    /**
     * Creates an abstract view with the given entity.
     *
     * @param entity The entity.
     */
    protected AbstractView(Entity entity) {
        setEntity(entity);
    }

    /**
     * Check whether the current components are the same as the components of the given entity.
     *
     * @param entity The entity to check.
     *
     * @return True if the components are the same, false otherwise.
     */
    protected abstract boolean componentsCheck(Entity entity);

    /**
     * Removes the entity (if any) of this view and sets
     * all component references to null.
     */
    public void clearEntity() {
        synchronized (this) {
            this.entity = null;
            entityCleared();
        }
    }

    /** Returns the current entity under this view. **/
    public Entity getEntity() {
        synchronized (this) {
            return this.entity;
        }
    }

    /**
     * Sets the entity behind this view. This method
     * can be used to reuse the {@link AbstractView} instance.
     *
     * @param entity The new entity.
     */
    public void setEntity(Entity entity) {
        synchronized (this) {
            if (entity != null && (this.entity != entity || !componentsCheck(entity))) {
                clearEntity();
                entityCheck(entity);
                this.entity = entity;
                entityChanged();
            }
        }
    }

    /**
     * Checks whether the entity is valid, i.e., it is not null
     * and has at least one component. Removed entities, for instance,
     * have no components.
     *
     * @return Whether the entity is valid.
     */
    public boolean isValid() {
        if (!isEmpty()) {
            return this.entity.getComponents().size() > 0;
        }
        return false;
    }

    /**
     * Checks whether an entity is set in this view. Note that an entity may be set, but the
     * entity may have been removed from the engine. In that case, the entity is no longer
     * valid. Use {@link AbstractView#isValid()}.
     *
     * @return True if an entity is not set. False otherwise.
     */
    public boolean isEmpty() {
        return entity == null;
    }

    /**
     * Checks whether an entity has a component, and throws a {@link RuntimeException} if it does not.
     *
     * @param entity         The entity.
     * @param mapper         The component mapper.
     * @param componentClass The component class.
     */
    protected void check(Entity entity, ComponentMapper mapper, Class<? extends Component> componentClass) {
        if (!mapper.has(entity)) {
            throw new RuntimeException("The given entity does not have a " + componentClass.getSimpleName() + " component: Can't be a " + this.getClass().getSimpleName() + ".");
        }
    }

    /**
     * Checks whether the given entity is suitable for this view. This
     * method should throw a {@link RuntimeException} if the entity is
     * not suitable.
     *
     * @param entity The entity.
     */
    protected abstract void entityCheck(Entity entity);

    /**
     * Contains actions to take after a new entity has been set.
     * This method is typically used to initialize the view components.
     */
    protected abstract void entityChanged();

    /**
     * This method is called when the entity of this view is cleared. It
     * should set all component references to null.
     */
    protected abstract void entityCleared();

}
