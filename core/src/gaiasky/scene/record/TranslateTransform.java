/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.math.Matrix4d;

public class TranslateTransform implements ITransform {
    /** Translation **/
    double[] vector;

    public void apply(Matrix4 mat) {
        mat.translate((float) vector[0], (float) vector[1], (float) vector[2]);
    }

    public void apply(Matrix4d mat) {
        mat.translate(vector[0], vector[1], vector[2]);
    }

    public double[] getVector() {
        return vector;
    }

    public void setVector(double[] vector) {
        this.vector = vector;
    }
}
