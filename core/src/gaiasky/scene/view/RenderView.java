package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.render.api.IPointRenderable;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;

/**
 * An entity view that implements the {@link IRenderable} methods.
 */
public class RenderView extends AbstractView implements IRenderable {

    /** The base component. **/
    private Base base;
    /** The body component. **/
    private Body body;

    public RenderView(){

    }

    public RenderView(Entity entity) {
        super(entity);
    }

    @Override
    protected void entityCheck(Entity entity) {
        if (!Mapper.base.has(entity)) {
            throw new RuntimeException("The given entity does not have a " + Base.class.getSimpleName() + " component: Can't be a " + RenderView.class.getSimpleName() + ".");
        }
        if (!Mapper.body.has(entity)) {
            throw new RuntimeException("The given entity does not have a " + Body.class.getSimpleName() + " component: Can't be a " + RenderView.class.getSimpleName() + ".");
        }
    }

    @Override
    protected void entityChanged() {
        this.base = Mapper.base.get(entity);
        this.body = Mapper.body.get(entity);
    }

    @Override
    public ComponentTypes getComponentType() {
        return base.ct;
    }

    @Override
    public double getDistToCamera() {
        return body.distToCamera;
    }

    @Override
    public float getOpacity() {
        return base.opacity;
    }

}
