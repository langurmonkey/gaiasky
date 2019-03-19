package gaia.cu9.ari.gaiaorbit.util.comp;

import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;

import java.util.Comparator;

/**
 * Compares models. Nearer models go first, further models go last.
 * 
 * @author Toni Sagrista
 */
public class ModelComparator<T> implements Comparator<T> {

    @Override
    public int compare(T o1, T o2) {
        return Double.compare(((AbstractPositionEntity) o1).distToCamera, ((AbstractPositionEntity) o2).distToCamera);
    }

}
