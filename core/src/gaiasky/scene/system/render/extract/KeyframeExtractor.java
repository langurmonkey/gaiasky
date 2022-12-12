package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;

public class KeyframeExtractor extends AbstractExtractSystem {
    public KeyframeExtractor(Family family, int priority) {
        super(family, priority);
    }

    private void extractVerts(Entity entity) {
        var base = Mapper.base.get(entity);
        var verts = Mapper.verts.get(entity);
        if (mustRender(base) && verts.pointCloudData != null && verts.pointCloudData.getNumPoints() > 0) {
            var render = Mapper.render.get(entity);
            addToRender(render, verts.renderGroup);
        }
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var kf = Mapper.keyframes.get(entity);
        // Extract all children paths
        for (Entity object : kf.objects) {
            extractVerts(object);
        }

        // Extract main object
        var base = Mapper.base.get(entity);
        if (mustRender(base) && (kf.selected != null || kf.highlighted != null)) {
            var render = Mapper.render.get(entity);
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }
}
