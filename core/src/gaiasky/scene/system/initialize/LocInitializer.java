package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Mapper;
import gaiasky.util.Constants;

/**
 * Initializes location mark entities.
 */
public class LocInitializer extends AbstractInitSystem {

    public LocInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var loc = Mapper.loc.get(entity);

        body.color = new float[] { 1f, 1f, 1f, 1f };
        loc.location3d = new Vector3();
        loc.sizeKm = (float) (body.size * Constants.U_TO_KM);

    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
