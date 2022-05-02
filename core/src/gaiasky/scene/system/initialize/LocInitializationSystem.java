package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.LocationMark;
import gaiasky.util.Constants;

/**
 * Initializes location mark entities.
 */
public class LocInitializationSystem extends IteratingSystem {

    public LocInitializationSystem(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        initializeEntity(entity);
    }

    public void initializeEntity(Entity entity) {
        Body body = Mapper.body.get(entity);
        GraphNode graph = Mapper.graph.get(entity);
        LocationMark loc = Mapper.loc.get(entity);

        body.cc = new float[]{1f, 1f, 1f, 1f};
        graph.localTransform = new Matrix4();
        loc.location3d = new Vector3();
        loc.sizeKm = (float) (body.size * Constants.U_TO_KM);

    }
}
