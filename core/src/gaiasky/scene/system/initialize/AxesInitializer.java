package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.LinePrimitiveRenderer;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

/**
 * Initializes axes objects.
 */
public class AxesInitializer extends AbstractInitSystem {

    public AxesInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var axis = Mapper.axis.get(entity);
        var line = Mapper.line.get(entity);

        // Lines.
        line.lineWidth = 1f;
        line.renderConsumer = LineEntityRenderSystem::renderAxes;

        // Base
        axis.b0 = new Vector3d(1, 0, 0);
        axis.b1 = new Vector3d(0, 1, 0);
        axis.b2 = new Vector3d(0, 0, 1);

        axis.o = new Vector3d();
        axis.x = new Vector3d();
        axis.y = new Vector3d();
        axis.z = new Vector3d();
    }

    @Override
    public void setUpEntity(Entity entity) {
        var axis = Mapper.axis.get(entity);
        var transform = Mapper.transform.get(entity);

        if (transform.transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transform.transformName);
                transform.matrix = (Matrix4d) m.invoke(null);
            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transform.transformName + "()");
            }
            axis.b0.mul(transform.matrix);
            axis.b1.mul(transform.matrix);
            axis.b2.mul(transform.matrix);
        }

        // Axes colors, RGB default
        if (axis.axesColors == null) {
            axis.axesColors = new float[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } };
        }
    }
}
