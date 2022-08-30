package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;

public class RaymarchingExtractor extends AbstractExtractSystem {

    public RaymarchingExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var rm = Mapper.raymarching.get(entity);

        if (rm != null && rm.raymarchingShader != null)
            camera.checkClosestBody(entity);
    }
}
