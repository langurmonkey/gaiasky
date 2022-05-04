package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Coordinates;
import gaiasky.scene.component.GraphNode;
import gaiasky.util.math.Vector3b;

/**
 * Initializes the base and graph components of entities.
 */
public class BaseInitializer extends InitSystem {

    private Scene scene;

    public BaseInitializer(Scene scene, boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
        this.scene = scene;
    }

    @Override
    public void initializeEntity(Entity entity) {
        Base base = Mapper.base.get(entity);
        GraphNode graph = Mapper.graph.get(entity);

        // Initialize component type, if entity does not have one.
        if (base.ct == null) {
            base.ct = new ComponentTypes(ComponentType.Others.ordinal());
        }

        // Initialize base scene graph structures.
        if (graph != null) {
            graph.localTransform = new Matrix4();
            graph.translation = new Vector3b();
        }

    }

    @Override
    public void setUpEntity(Entity entity) {
        if (Mapper.coordinates.has(entity)) {
            Coordinates coord = Mapper.coordinates.get(entity);
            if (coord.coordinates != null)
                coord.coordinates.doneLoading(scene, entity);
        }
    }
}
