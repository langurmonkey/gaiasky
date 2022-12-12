package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;

/**
 * Extracts background model object and UV grid data to feed to the render stage.
 */
public class BackgroundExtractor extends AbstractExtractSystem {
    public BackgroundExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        addToRenderLists(entity);
    }

    private void addToRenderLists(Entity entity) {
        var base = Mapper.base.get(entity);

        if (mustRender(base)) {
            var render = Mapper.render.get(entity);

            if (Mapper.grid.has(entity)) {
                // UV grid
                addToRender(render, RenderGroup.MODEL_VERT_GRID);
                addToRender(render, RenderGroup.FONT_ANNOTATION);
            } else {
                // Regular background model (skybox)
                var label = Mapper.label.get(entity);
                var renderType = Mapper.renderType.get(entity);

                addToRender(render, renderType.renderGroup);
                if (label.label) {
                    addToRender(render, RenderGroup.FONT_LABEL);
                }
            }
        }
    }
}
