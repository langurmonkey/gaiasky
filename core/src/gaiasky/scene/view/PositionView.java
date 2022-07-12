package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.IPosition;

/**
 * A view which exposes position and velocity properties of an entity.
 * Can be reused
 */
public class PositionView extends AbstractView implements IPosition {

    /** The body component. **/
    private Body body;

    public PositionView(Entity entity) {
        super(entity);
    }

    @Override
    protected void entityCheck(Entity entity) {
        if (!Mapper.body.has(entity)) {
            throw new RuntimeException("The given entity does not have a " + Body.class.getSimpleName() + " component: Can't be a " + this.getClass().getSimpleName() + ".");
        }
    }

    @Override
    protected void entityChanged() {
        this.body = Mapper.body.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.body = null;
    }

    @Override
    public Vector3b getPosition() {
        return body.pos;
    }

    @Override
    public Vector3d getVelocity() {
        return null;
    }
}
