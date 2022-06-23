package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.Octree;
import gaiasky.scene.system.render.extract.ParticleSetExtractor;
import gaiasky.scene.view.OctreeObjectView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.OctreeNode;

public class OctreeUpdater extends IteratingSystem implements EntityUpdater {

    private final GraphUpdater graphUpdater;
    private final ParticleSetUpdater particleSetUpdater;
    private final ParticleSetExtractor particleSetExtractor;

    public OctreeUpdater(Family family, int priority) {
        super(family, priority);

        this.graphUpdater = new GraphUpdater(null, 0, GaiaSky.instance.time);
        this.particleSetUpdater = new ParticleSetUpdater(null, 0);
        this.particleSetExtractor = new ParticleSetExtractor(null, 0);
        particleSetExtractor.setRenderer(GaiaSky.instance.sceneRenderer);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        var root = Mapper.octant.get(entity);
        var octree = Mapper.octree.get(entity);

        // Fade node visibility applies here
        if (base.isVisible()) {
            // Update octants
            if (!base.copy) {

                // Compute observed octants and fill roulette list
                OctreeNode.nOctantsObserved = 0;
                OctreeNode.nObjectsObserved = 0;

                ICamera camera = GaiaSky.instance.cameraManager;

                // Update root node
                root.octant.update(graph.translation, camera, octree.roulette, base.opacity);

                // Call the update method of all entities in the roulette list.
                updateOctreeObjects(base, graph, octree, deltaTime);

                // Reset roulette.
                octree.roulette.clear();

                // Render the octree with lines.
                //addToRenderLists(camera, root);

                // Update focus, just in case
                //IFocus focus = camera.getFocus();
                //if (focus != null) {
                //    SceneGraphNode star = focus.getFirstStarAncestor();
                //    OctreeNode parent = parenthood.get(star);
                //    if (parent != null && !parent.isObserved()) {
                //        star.update(time, star.parent.translation, camera);
                //    }
                //}
            } else {
                // Just update children
                //for (SceneGraphNode node : children) {
                //    node.update(time, translation, camera);
                //}
            }
        }
    }

    /**
     * Updates and extracts all observed octree objects.
     */
    protected void updateOctreeObjects(Base base, GraphNode graph, Octree octree, float deltaTime) {
        updateGraph(base, graph, octree, GaiaSky.instance.time);
        updateParticleSet(octree, deltaTime);
        extractParticleSet(octree);
    }

    private void updateGraph(Base base, GraphNode graph, Octree octree, ITimeFrameProvider time) {
        int size = octree.roulette.size();
        graphUpdater.setCamera(GaiaSky.instance.cameraManager);
        for (int i = 0; i < size; i++) {
            Entity entity = ((OctreeObjectView) octree.roulette.get(i)).getEntity();
            // Use octant opacity
            var octant = Mapper.octant.get(entity);
            graphUpdater.update(entity, time, graph.translation, base.opacity * octant.octant.opacity);
        }
    }

    private void updateParticleSet(Octree octree, float deltaTime) {
        int size = octree.roulette.size();
        for (int i = 0; i < size; i++) {
            Entity entity = ((OctreeObjectView) octree.roulette.get(i)).getEntity();
            particleSetUpdater.updateEntity(entity, deltaTime);
        }
    }

    private void extractParticleSet(Octree octree) {
        int size = octree.roulette.size();
        for (int i = 0; i < size; i++) {
            Entity entity = ((OctreeObjectView) octree.roulette.get(i)).getEntity();
            particleSetExtractor.extractEntity(entity);
        }
    }
}
