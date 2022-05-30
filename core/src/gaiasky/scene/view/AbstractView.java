package gaiasky.scene.view;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import org.apache.commons.math3.analysis.function.Abs;

/**
 * An abstract view that holds a reference to the current entity.
 * Views can be reused for multiple entities by calling
 */
public abstract class AbstractView {

    /** A reference to the entity. **/
    protected Entity entity;

    /** Creates an empty abstract view without entity. **/
    public AbstractView() {
    }

    /**
     * Creates an abstract view with the given entity.
     *
     * @param entity The entity.
     */
    public AbstractView(Entity entity) {
        setEntity(entity);
    }

    /**
     * Sets the entity behind this view. This method
     * can be used to reuse the {@link AbstractView} instance.
     *
     * @param entity The new entity.
     */
    public void setEntity(Entity entity) {
        entityCheck(entity);
        this.entity = entity;
        entityChanged();
    }

    /** Returns the current entity under this view. **/
    public Entity getEntity() {
       return this.entity;
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
}
