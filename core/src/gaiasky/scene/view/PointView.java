package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.api.IPointRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Verts;
import gaiasky.scene.entity.EntityUtils;

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
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.verts, Verts.class);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.verts = Mapper.verts.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.verts = null;
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
