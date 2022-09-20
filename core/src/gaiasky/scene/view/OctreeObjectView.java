package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Octant;
import gaiasky.scene.component.StarSet;
import gaiasky.scene.entity.StarSetUtils;
import gaiasky.util.tree.IOctreeObject;
import gaiasky.util.tree.OctreeNode;

/** A view that implements {@link gaiasky.util.tree.IOctreeObject} methods for entities. **/
public class OctreeObjectView extends PositionView implements IOctreeObject {

    public StarSet set;
    Octant octant;

    public OctreeObjectView(Entity entity) {
        super(entity);
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.set = Mapper.starSet.get(entity);
        this.octant = Mapper.octant.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.set = null;
        this.octant = null;
    }


    @Override
    public int getStarCount() {
        return set != null ? set.pointData.size() : 0;
    }

    @Override
    public void dispose() {
        (new StarSetUtils(null)).dispose(entity, set);
    }

    public void setOctant(OctreeNode octreeNode) {
        octant.octant = octreeNode;
    }
}
