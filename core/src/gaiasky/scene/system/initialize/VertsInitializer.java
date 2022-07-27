package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;

public class VertsInitializer extends AbstractInitSystem {
    public VertsInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var verts = Mapper.verts.get(entity);

        if (Mapper.line.has(entity)) {
            // Polyline.
            var line = Mapper.line.get(entity);

            line.lineWidth = verts.primitiveSize;
            line.renderConsumer = LineEntityRenderSystem::renderPolyline;

        } else {
            // Normal verts (points).

        }

    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
