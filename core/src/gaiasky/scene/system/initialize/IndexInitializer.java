package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Index;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Octant;
import gaiasky.scene.view.OctreeObjectView;
import gaiasky.util.tree.OctreeNode;

/**
 * Initializes the name and id indices.
 */
public class IndexInitializer extends AbstractInitSystem {

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
        if (Mapper.octree.has(entity)) {
            var octree = Mapper.octree.get(entity);
            var octant = Mapper.octant.get(entity);
            initializeOctant(octant.octant);
        }

        // Add entity to HIP map, for constellations
        index.addToHipMap(entity);

    }

    private void initializeOctant(OctreeNode octant) {
        if (octant != null) {

            // Add objects to index.
            if(octant.objects != null) {
                octant.objects.forEach((object) -> {
                    if (object instanceof OctreeObjectView) {
                        index.addToIndex(((OctreeObjectView) object).getEntity());
                    }
                });
            }

            // Process children octants.
            if(octant.children != null) {
                for(OctreeNode child : octant.children) {
                    if(child != null) {
                        initializeOctant(child);
                    }
                }
            }
        }
        ;

    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
