/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.tree;

import gaiasky.util.math.Vector3d;

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
