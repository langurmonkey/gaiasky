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
        var render = Mapper.render.get(entity);
        var label = Mapper.label.get(entity);
        boolean mustRender = mustRender(base);
        if (mustRender) {
            var renderType = Mapper.renderType.get(entity);
            var model = Mapper.model.get(entity);
            if (model.model != null) {
                addToRender(render, renderType.renderGroup != null ? renderType.renderGroup : RenderGroup.MODEL_VERT_ADDITIVE);
            }
        }
        if (mustRender || label.forceLabel) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }
}
