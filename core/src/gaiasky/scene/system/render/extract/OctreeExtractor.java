package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Octree;
import gaiasky.scene.view.OctreeObjectView;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.tree.OctreeNode;

import static gaiasky.render.RenderGroup.LINE;

/**
 * Extracts octant data to render an octree with lines.
 */
public class OctreeExtractor extends AbstractExtractSystem {

    private final ParticleSetExtractor particleSetExtractor;

    public OctreeExtractor(Family family, int priority) {
        super(family, priority);
        this.particleSetExtractor = new ParticleSetExtractor(null, 0);
        particleSetExtractor.setRenderer(GaiaSky.instance.sceneRenderer);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var root = Mapper.octant.get(entity);
        var octree = Mapper.octree.get(entity);

        if (base.isVisible() && !base.copy) {
            // Extract objects.
            extractParticleSet(octree);

            // Clear roulette.
            octree.roulette.clear();

            // Extract octree nodes themselves (render octree wireframes).
            addToRenderLists(base, root.octant, camera);
        }
    }

    /**
     * Extracts all observed octree objects.
     */
    private void extractParticleSet(Octree octree) {
        int size = octree.roulette.size();
        for (int i = 0; i < size; i++) {
            Entity entity = ((OctreeObjectView) octree.roulette.get(i)).getEntity();
            particleSetExtractor.extractEntity(entity);
        }
    }

    public void addToRenderLists(Base base, OctreeNode octant, ICamera camera) {
        if (shouldRender(base) && Settings.settings.runtime.drawOctree && octant.observed) {
            boolean added = addToRender(octant, LINE);

            if (added) {
                for (int i = 0; i < 8; i++) {
                    OctreeNode child = octant.children[i];
                    if (child != null) {
                        addToRenderLists(base, child, camera);
                    }
                }
            }
        }
    }
}
