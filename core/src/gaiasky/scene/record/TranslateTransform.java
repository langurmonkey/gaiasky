/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.math.Matrix4D;

public class TranslateTransform implements ITransform {
    /** Translation **/
    double[] vector;

    public void apply(Matrix4 mat) {
        mat.translate((float) vector[0], (float) vector[1], (float) vector[2]);
    }

    public void apply(Matrix4D mat) {
        mat.translate(vector[0], vector[1], vector[2]);
    }

    public double[] getVector() {
        return vector;
    }

    public void setVector(double[] vector) {
        this.vector = vector;
    }

    @Override
    public ITransform copy() {
        var c = new TranslateTransform();
        if (this.vector != null)
            c.vector = this.vector.clone();

        return c;
    }
}
