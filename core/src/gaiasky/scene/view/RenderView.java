package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.ParticleExtra;

/**
 * An entity view that implements the {@link IRenderable} methods.
 */
public class RenderView extends AbstractView implements IRenderable {

    /** The base component. **/
    protected Base base;
    /** The body component. **/
    protected Body body;
    /** Particle component, maybe. **/
    protected ParticleExtra extra;

    public RenderView() {
    }

    public RenderView(Entity entity) {
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
        this.extra = Mapper.extra.get(entity);
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

    public double getRadius() {
        return extra == null ? body.size / 2.0 : extra.radius;
    }

    public float[] textColour() {
        return body.labelColor;
    }
}
