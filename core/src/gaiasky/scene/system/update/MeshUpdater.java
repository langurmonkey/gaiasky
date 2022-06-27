package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.scene.Mapper;

public class MeshUpdater extends IteratingSystem implements EntityUpdater {

    private float[] auxArray;

    public MeshUpdater(Family family, int priority) {
        super(family, priority);
        auxArray = new float[3];
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var model = Mapper.model.get(entity);

        // Update light with global position
        var mc = model.model;
        if (mc != null) {
            mc.directional(0).direction.set(1f, 0f, 0f);
            mc.directional(0).color.set(1f, 1f, 1f, 1f);

            var body = Mapper.body.get(entity);
            var graph = Mapper.graph.get(entity);
            var mesh = Mapper.mesh.get(entity);
            var affine = Mapper.affine.get(entity);

            // Update local transform
            float[] trn = graph.translation.valuesf(auxArray);
            graph.localTransform.idt().translate(trn[0], trn[1], trn[2]).scl(body.size).mul(mesh.coordinateSystem);

            // Apply transformations
            affine.apply(graph.localTransform);
        }
    }
}
