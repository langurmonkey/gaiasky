package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.scene.Mapper;
import gaiasky.util.Logger;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Matrix4d;

/**
 * Initializes axes objects.
 */
public class AxesInitializer extends InitSystem {

    public AxesInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
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
