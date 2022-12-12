package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.util.Logger;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4d;

/**
 * Updates background models and UV grids.
 */
public class BackgroundUpdater extends AbstractUpdateSystem {

    public BackgroundUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        processEntity(entity, deltaTime);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);

        updateLocalTransform(entity, body, graph);
    }

    private void updateLocalTransform(Entity entity, Body body, GraphNode graph) {
        var transform = Mapper.transform.get(entity);
        var model = Mapper.model.get(entity);

        String transformName = transform.transformName;
        ;
        Matrix4 localTransform = graph.localTransform;

        localTransform.idt();
        // Initialize transform.
        localTransform.scl(body.size);

        if (transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transformName);
                Matrix4d trf = (Matrix4d) m.invoke(null);
                Matrix4 aux = trf.putIn(new Matrix4());
                localTransform.mul(aux);
            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transformName + "()");
            }
        }

        // Must rotate due to orientation of the sphere model
        if (model.model != null && model.model.type.equalsIgnoreCase("sphere")) {
            localTransform.rotate(0, 1, 0, 90);
        }
    }
}
