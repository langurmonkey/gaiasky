package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Octant;
import gaiasky.scenegraph.SceneGraphNode;
import gaiasky.scenegraph.octreewrapper.AbstractOctreeWrapper;

/**
 * Initializes the name and id indices.
 */
public class IndexInitializationSystem extends IteratingSystem {

    private Scene scene;

    public IndexInitializationSystem(Family family, int priority, Scene scene) {
        super(family, priority);
        this.scene = scene;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        // Add entity to index
        scene.addToIndex(entity);

        // Unwrap octree objects
        if (Mapper.octant.has(entity)) {
            Octant octant = Mapper.octant.get(entity);
            // TODO add all children to index
            //for (SceneGraphNode ownode : ow.children) {
            //    addToIndex(ownode);
            //}
        }

        // Add entity to HIP map, for constellations
        scene.addToHipMap(entity);

    }
}
