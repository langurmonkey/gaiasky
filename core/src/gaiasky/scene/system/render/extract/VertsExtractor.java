package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;

public class VertsExtractor extends AbstractExtractSystem {

    public VertsExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var verts = Mapper.verts.get(entity);
        // Lines only make sense with 2 or more points
        if (this.shouldRender(base) && verts.pointCloudData != null && verts.pointCloudData.getNumPoints() > 1) {
            var render = Mapper.render.get(entity);
            addToRender(render, verts.renderGroup);
        }
    }
}
