package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;

public class MeshExtractor extends AbstractExtractSystem {

    public MeshExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var render = Mapper.render.get(entity);
        var mesh = Mapper.mesh.get(entity);

        if (shouldRender(base) && GaiaSky.instance.isInitialised()) {
            switch (mesh.shading) {
            case ADDITIVE -> addToRender(render, RenderGroup.MODEL_VERT_ADDITIVE);
            case REGULAR -> addToRender(render, RenderGroup.MODEL_PIX_EARLY);
            case DUST -> addToRender(render, RenderGroup.MODEL_PIX_DUST);
            }

            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }
}
