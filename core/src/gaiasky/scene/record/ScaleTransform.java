/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.math.Matrix4D;

public class ScaleTransform implements ITransform {
    /** Scale **/
    double[] scale;

    public void apply(Matrix4 mat) {
        mat.scale((float) scale[0], (float) scale[1], (float) scale[2]);
    }

    public void apply(Matrix4D mat) {
        mat.scale(scale[0], scale[1], scale[2]);
    }

    public double[] getScale() {
        return scale;
    }

    public void setScale(double[] scale) {
        this.scale = scale;
    }

    @Override
    public ITransform copy() {
        var c = new ScaleTransform();
        if (this.scale != null)
            c.scale = this.scale.clone();

        return c;
    }
}
