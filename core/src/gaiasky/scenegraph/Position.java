/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.IPosition;

public class Position implements IPosition {

    Vector3d pos;
    Vector3d vel;

    public Position(double x, double y, double z, double vx, double vy, double vz) {
        this.pos = new Vector3d(x, y, z);
        this.vel = new Vector3d(vx, vy, vz);
    }

    @Override
    public Vector3d getPosition() {
        return pos;
    }

    @Override
    public Vector3d getVelocity() {
        return vel;
    }

}
