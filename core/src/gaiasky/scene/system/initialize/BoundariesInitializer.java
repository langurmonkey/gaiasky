package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.LinePrimitiveRenderer;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scenegraph.camera.ICamera;

public class BoundariesInitializer extends AbstractInitSystem {
    public BoundariesInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var line = Mapper.line.get(entity);
        // Lines.
        line.lineWidth = 1;
        line.renderConsumer = (LineEntityRenderSystem rs, Entity e, LinePrimitiveRenderer r, ICamera c, Float a)
                -> rs.renderConstellationBoundaries(e, r, c, a);
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
