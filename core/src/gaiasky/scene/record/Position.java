/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3D;
import gaiasky.util.tree.IPosition;

public class Position implements IPosition {

    Vector3Q pos;
    Vector3D vel;

    public Position(double x, double y, double z, double vx, double vy, double vz) {
        this.pos = new Vector3Q(x, y, z);
        this.vel = new Vector3D(vx, vy, vz);
    }

    @Override
    public Vector3Q getPosition() {
        return pos;
    }

    @Override
    public Vector3D getVelocity() {
        return vel;
    }

}
