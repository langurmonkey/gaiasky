package gaiasky.scene.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.tree.OctreeNode;

import static gaiasky.render.RenderGroup.LINE;

/**
 * Extracts octant data to render an octree with lines.
 */
public class OctreeExtractor extends AbstractExtractSystem {

    public OctreeExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var root = Mapper.octant.get(entity);

        addToRenderLists(base, root.octant, camera);
    }

    public void addToRenderLists(Base base, OctreeNode octant, ICamera camera) {
        if (this.shouldRender(base) && Settings.settings.runtime.drawOctree && octant.observed) {
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
