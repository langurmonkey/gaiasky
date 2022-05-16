package gaiasky.util.comp;

import gaiasky.scene.Mapper;
import gaiasky.scene.component.Render;

import java.util.Comparator;

/**
 * Compares entities with respect to their distance to the camera.
 *
 * @param <T> The type of entity to compare.
 */
public class DistanceEntityComparator<T> implements Comparator<T> {
    @Override
    public int compare(T o1, T o2) {
        return -Double.compare(Mapper.body.get(((Render) o1).entity).distToCamera, Mapper.body.get(((Render) o2).entity).distToCamera);
    }
}
