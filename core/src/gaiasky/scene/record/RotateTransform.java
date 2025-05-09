/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.math.Matrix4D;

public class RotateTransform implements ITransform {
    /** Rotation axis. **/
    double[] axis;
    /** Rotation angle [deg]. **/
    double angle;

    public void apply(Matrix4 mat) {
        mat.rotate((float) axis[0], (float) axis[1], (float) axis[2], (float) angle);
    }

    public void apply(Matrix4D mat) {
        mat.rotate(axis[0], axis[1], axis[2], angle);
    }


    public double[] getAxis() {
        return axis;
    }

    public void setAxis(double[] axis) {
        this.axis = axis;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(Double angle) {
        this.angle = angle;
    }

    @Override
    public ITransform copy() {
        var c = new RotateTransform();
        if (this.axis != null)
            c.axis = this.axis.clone();
        c.angle = this.angle;

        return c;
    }
}
