package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.util.Settings;

public class TitleExtractor extends AbstractExtractSystem{
    public TitleExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);

        if (shouldRender(base) && !Settings.settings.program.modeCubemap.active) {
            var render = Mapper.render.get(entity);
            var title = Mapper.title.get(entity);

            addToRender(render, RenderGroup.FONT_LABEL);
            if (title.lines) {
                addToRender(render, RenderGroup.SHAPE);
            }
        }
    }

}
