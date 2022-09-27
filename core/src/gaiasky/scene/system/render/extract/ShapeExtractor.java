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
        if (shouldRender(base)) {
            var render = Mapper.render.get(entity);
            addToRender(render, RenderGroup.MODEL_BG);
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }
}
