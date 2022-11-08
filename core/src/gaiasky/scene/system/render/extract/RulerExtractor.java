package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;

public class RulerExtractor extends AbstractExtractSystem {
    public RulerExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var ruler = Mapper.ruler.get(entity);
        if (mustRender(base) && ruler.rulerOk) {
            var render = Mapper.render.get(entity);
            addToRender(render, RenderGroup.LINE);
            addToRender(render, RenderGroup.FONT_LABEL);
        }

    }
}
