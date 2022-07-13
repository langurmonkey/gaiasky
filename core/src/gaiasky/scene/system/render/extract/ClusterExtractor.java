package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.update.ClusterUpdater;

/**
 * Extracts star cluster data to feed the render stage.
 */
public class ClusterExtractor extends AbstractExtractSystem {

    public ClusterExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);

        if (shouldRender(base)) {
            var body = Mapper.body.get(entity);
            var render = Mapper.render.get(entity);

            if (body.solidAngleApparent >= ClusterUpdater.TH_ANGLE) {
                addToRender(render, RenderGroup.MODEL_VERT_ADDITIVE);
            }
            if (body.solidAngleApparent >= ClusterUpdater.TH_ANGLE || base.forceLabel) {
                addToRender(render, RenderGroup.FONT_LABEL);
            }

            if (body.solidAngleApparent < ClusterUpdater.TH_ANGLE_OVERLAP) {
                addToRender(render, RenderGroup.BILLBOARD_SPRITE);
            }
        }
    }
}
