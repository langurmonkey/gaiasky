/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3d;

public class QuaternionTransform implements ITransform {

    QuaternionDouble quaternion;
    Quaternion quatFloat;

    @Override
    public void apply(Matrix4 mat) {
        if (quaternion != null) {
            if (quatFloat == null) {
                quatFloat = new Quaternion();
            }
            quatFloat.set((float) quaternion.x, (float) quaternion.y, (float) quaternion.z, (float) quaternion.w);
            mat.rotate(quatFloat);
        }
    }

    @Override
    public void apply(Matrix4d mat) {
        if (quaternion != null)
            mat.rotate(quaternion);
    }

    public void setQuaternion(QuaternionDouble quaternion) {
        this.quaternion = new QuaternionDouble(quaternion);
    }

    public void setQuaternion(Vector3d axis, double angle) {
        this.quaternion = new QuaternionDouble(axis, angle);
    }

    public void setQuaternion(double axisX, double axisY, double axisZ, double angle) {
        this.quaternion = new QuaternionDouble(new Vector3d(axisX, axisY, axisZ), angle);
    }

    public void setQuaternion(double[] xyzw) {
        this.quaternion = new QuaternionDouble(xyzw[0], xyzw[1], xyzw[2], xyzw[3]);
    }

    @Override
    public ITransform copy() {
        var c = new QuaternionTransform();
        if (this.quaternion != null)
            c.quaternion = new QuaternionDouble(this.quaternion);
        if (this.quatFloat != null)
            c.quatFloat = new Quaternion(this.quatFloat);

        return c;
    }
}
