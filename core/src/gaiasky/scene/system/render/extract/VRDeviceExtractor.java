package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;

public class VRDeviceExtractor extends AbstractExtractSystem {

    public VRDeviceExtractor(Family family, int priority) {
        super(family, priority);
    }

    public void setRenderGroup(RenderGroup renderGroup) {
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        if (this.mustRender(base)) {
            var render = Mapper.render.get(entity);
            addToRender(render, RenderGroup.MODEL_PIX);
            if (Mapper.line.has(entity)) {
                addToRender(render, RenderGroup.LINE);
            }
        }
    }
}
