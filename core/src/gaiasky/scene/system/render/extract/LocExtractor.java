package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.util.math.Vector3d;

public class LocExtractor extends AbstractExtractSystem {

    public LocExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        if (shouldRender(base)) {
            addToRender(Mapper.render.get(entity), RenderGroup.FONT_LABEL);
        }
    }

}
