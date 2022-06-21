package gaiasky.scene.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;

public class BillboardSetExtractor extends AbstractExtractSystem {

    public BillboardSetExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var fade = Mapper.fade.get(entity);

        if (shouldRender(base) && (fade.fadeIn == null || fade.currentDistance > fade.fadeIn.x) && (fade.fadeOut == null || fade.currentDistance < fade.fadeOut.y)) {
            var render = Mapper.render.get(entity);

            if (renderText()) {
                addToRender(render, RenderGroup.FONT_LABEL);
            }
            addToRender(render, RenderGroup.BILLBOARD_GROUP);
        }
    }
}
