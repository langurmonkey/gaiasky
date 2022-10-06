package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.util.math.Vector3d;

import java.util.Locale;

public class ShapeUpdater extends AbstractUpdateSystem{

    private final Vector3 F31 = new Vector3();

    public ShapeUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var shape = Mapper.shape.get(entity);

        graph.translation.sub(body.pos);
        if (shape.track != null) {
            EntityUtils.getAbsolutePosition(shape.track.getEntity(), shape.trackName.toLowerCase(Locale.ROOT), body.pos);
        } else if (!body.positionSetInScript){
            body.pos.scl(0);
        }
        // Update pos, local transform
        graph.translation.add(body.pos);

        graph.localTransform.idt().translate(graph.translation.put(F31)).scl(body.size);

    }
}
