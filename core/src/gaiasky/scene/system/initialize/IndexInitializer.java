package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;

/**
 * Initializes the name and id indices.
 */
public class IndexInitializer extends InitSystem {

    private Scene scene;

    public IndexInitializer(Scene scene, Family family, int priority) {
        super(false, family, priority);
        this.scene = scene;
    }

    @Override
    public void initializeEntity(Entity entity) {
        // Add entity to index
        scene.addToIndex(entity);

        // Unwrap octree objects
        if (Mapper.octant.has(entity)) {
            var octant = Mapper.octant.get(entity);
            // TODO add all children to index
            //for (SceneGraphNode ownode : ow.children) {
            //    addToIndex(ownode);
            //}
        }

        // Add entity to HIP map, for constellations
        scene.addToHipMap(entity);

    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
