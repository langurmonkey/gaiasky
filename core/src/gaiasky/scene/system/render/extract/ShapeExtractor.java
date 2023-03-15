package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;

public class ShapeExtractor extends AbstractExtractSystem {
    public ShapeExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        if (mustRender(base)) {
            var render = Mapper.render.get(entity);
            var renderType = Mapper.renderType.get(entity);
            addToRender(render, renderType.renderGroup != null ? renderType.renderGroup : RenderGroup.MODEL_VERT_ADDITIVE);
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }
}
