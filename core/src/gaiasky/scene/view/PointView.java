package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import gaiasky.render.api.IPointRenderable;
import gaiasky.render.system.PointRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Verts;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scenegraph.camera.ICamera;

/**
 * An entity view that implements the {@link IPointRenderable} methods.
 */
public class PointView extends RenderView implements IPointRenderable {

    /** The verts component . **/
    private Verts verts;

    /** Creates an empty line view. **/
    public PointView() {

    }

    @Override
    public void render(PointRenderSystem renderer, ICamera camera, float alpha) {
        /** This is implemented in {@link gaiasky.scene.render.draw.PointPrimitiveRenderSystem}. **/
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        if (!Mapper.verts.has(entity)) {
            throw new RuntimeException("The given entity does not have a " + Verts.class.getSimpleName() + " component: Can't be a " + VertsView.class.getSimpleName() + ".");
        }
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.verts = Mapper.verts.get(entity);
    }

    @Override
    public void blend() {
        EntityUtils.blend(verts);
    }

    @Override
    public void depth() {
        EntityUtils.depth(verts);
    }
}
