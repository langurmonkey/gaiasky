package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.scene.Mapper;

public class BillboardSetUpdater extends IteratingSystem implements EntityUpdater {

    private final Matrix4 M41;
    public BillboardSetUpdater(Family family, int priority) {
        super(family, priority);
        M41 = new Matrix4();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var graph = Mapper.graph.get(entity);
        var body = Mapper.body.get(entity);
        var transform = Mapper.transform.get(entity);

        graph.translation.setToTranslation(graph.localTransform).scl(body.size);
        graph.localTransform.mul(transform.matrix.putIn(M41));
    }
}
