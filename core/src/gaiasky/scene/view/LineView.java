package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.GL30;
import gaiasky.render.ComponentTypes;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scene.system.render.draw.LinePrimitiveRenderer;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scenegraph.camera.ICamera;

/**
 * An entity view that implements the {@link ILineRenderable} methods.
 */
public class LineView extends BaseView implements ILineRenderable {
    public Verts verts;
    public Line line;

    private LineEntityRenderSystem renderSystem;

    /** Creates an empty line view. **/
    public LineView() {
        super();
        renderSystem = new LineEntityRenderSystem(this);
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
    protected void entityChanged() {
        super.entityChanged();
        this.verts = Mapper.verts.get(entity);
        this.line = Mapper.line.get(entity);
    }

    @Override
    public float getLineWidth() {
        return verts != null ? verts.primitiveSize : (line != null ? line.lineWidth : 0.6f);
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        // Run consumer.
        line.renderConsumer.apply(renderSystem, entity, (LinePrimitiveRenderer) renderer, camera, alpha);
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
