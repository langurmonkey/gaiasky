package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.util.Settings;

public class GridRecExtractor extends AbstractExtractSystem{
    public GridRecExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        if (shouldRender(base)) {
            var render = Mapper.render.get(entity);
            var label = Mapper.label.get(entity);

            addToRender(render, RenderGroup.MODEL_VERT_RECGRID);
            if (label.label) {
                addToRender(render, RenderGroup.FONT_LABEL);
            }
            if (Settings.settings.program.recursiveGrid.origin.isRefSys() && Settings.settings.program.recursiveGrid.projectionLines && camera.hasFocus()) {
                addToRender(render, RenderGroup.LINE);
            }
        }

    }
}
