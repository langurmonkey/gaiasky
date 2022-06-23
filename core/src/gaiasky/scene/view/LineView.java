package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.GL30;
import gaiasky.render.ComponentTypes;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.Verts;
import gaiasky.scenegraph.camera.ICamera;

/**
 * An entity view that implements the {@link ILineRenderable} methods.
 */
public class LineView extends AbstractView implements ILineRenderable {

    private Base base;
    private Body body;
    private Verts verts;

    /** Creates an empty line view. **/
    public LineView() {
        super();
    }

    /**
     * Creates an abstract view with the given entity.
     *
     * @param entity The entity.
     */
    public LineView(Entity entity) {
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
        this.verts = Mapper.verts.get(entity);
    }

    @Override
    public float getLineWidth() {
        return verts != null ? verts.primitiveSize : 0.6f;
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        /** Not needed, implemented in {@link gaiasky.scene.system.render.draw.line.LineEntityRenderSystem}. **/
    }

    @Override
    public int getGlPrimitive() {
        return verts != null ? verts.glPrimitive : GL30.GL_LINES;
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
