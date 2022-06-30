package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;

/**
 * A basic view with the base and body components that all entities have.
 */
public abstract class BaseView extends AbstractView {

    /** The base component. **/
    protected Base base;
    /** The body component. **/
    protected Body body;

    public BaseView() {
    }
    public BaseView(Entity entity) {
        super(entity);
    }

    @Override
    protected void entityCheck(Entity entity) {
        check(entity, Mapper.base, Base.class);
        check(entity, Mapper.body, Body.class);
    }

    @Override
    protected void entityChanged() {
        this.base = Mapper.base.get(entity);
        this.body = Mapper.body.get(entity);
    }

    public void setColor(float[] color) {
        body.setColor(color);
    }
}
