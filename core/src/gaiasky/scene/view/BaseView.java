package gaiasky.scene.view;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;

/**
 * A basic view with the base and body components that all entities have.
 */
public abstract class BaseView extends AbstractView {

    /** The base component. **/
    public Base base;
    /** The body component. **/
    public Body body;

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

    public <T extends Component> T getComponent(Class<T> c) {
       return entity.getComponent(c) ;
    }

    public float getOpacity() {
        assert base != null;
        return base.opacity;
    }

    public Base getBase() {
        return base;
    }

    public Body getBody() {
        return body;
    }
}
