package gaia.cu9.ari.gaiaorbit.util.tree;

import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Interface that must implement all entities that have a position.
 * 
 * @author Toni Sagrista
 *
 */
public interface IPosition {

    Vector3d getPosition();

    Vector3d getVelocity();

}
