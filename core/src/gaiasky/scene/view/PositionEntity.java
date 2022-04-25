package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.IPosition;

/**
 * A view which exposes position and velocity properties of an entity.
 */
public class PositionEntity implements IPosition {

    private Body body;

    public PositionEntity(Entity entity) {
        if(!Mapper.body.has(entity)){
            throw new RuntimeException("The given entity does not have a Body component: Can't be a PositionEntity");
        }
        this.body = Mapper.body.get(entity);
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
