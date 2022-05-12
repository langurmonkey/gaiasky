package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;

/**
 * This class contains some general utilities applicable to all entities.
 */
public class EntityUtils {

    /**
     * Returns the absolute position of this entity in the native coordinates
     * (equatorial system) and internal units.
     *
     * @param out Auxiliary vector to put the result in.
     *
     * @return The vector with the position.
     */
    public static Vector3b getAbsolutePosition(Entity entity, Vector3b out) {
        synchronized (entity) {
            Body body = Mapper.body.get(entity);
            out.set(body.pos);
            Entity e = entity;
            GraphNode graph = Mapper.graph.get(e);
            while (graph.parent != null) {
                e = graph.parent;
                graph = Mapper.graph.get(e);
                out.add(Mapper.body.get(e).pos);
            }
            return out;
        }
    }

    /**
     * Checks whether the given entity has a {@link gaiasky.scene.component.Coordinates} component,
     * and the current time is out of range.
     * @return Whether the entity is in time overflow.
     */
    public static boolean isCoordinatesTimeOverflow(Entity entity) {
        return Mapper.coordinates.has(entity) && Mapper.coordinates.get(entity).timeOverflow;
    }

}
