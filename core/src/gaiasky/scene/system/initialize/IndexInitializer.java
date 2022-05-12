package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Index;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;

/**
 * Initializes the name and id indices.
 */
public class IndexInitializer extends InitSystem {

    /** The index reference. **/
    private Index index;

    public IndexInitializer(Index index, Family family, int priority) {
        super(false, family, priority);
        this.index = index;
    }

    @Override
    public void initializeEntity(Entity entity) {
        // Add entity to index
        index.addToIndex(entity);

        // Unwrap octree objects
        if (Mapper.octant.has(entity)) {
            var octant = Mapper.octant.get(entity);
            // TODO add all children to index
            //for (SceneGraphNode ownode : ow.children) {
            //    addToIndex(ownode);
            //}
        }

        // Add entity to HIP map, for constellations
        index.addToHipMap(entity);

    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
