/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.math.Matrix4;

public class ScaleTransform implements ITransform {
    /** Scale **/
    float[] scale;

    public void apply(Matrix4 mat) {
        mat.scale(scale[0], scale[1], scale[2]);
    }

    public void setScale(double[] scale) {
        this.scale = new float[scale.length];
        for (int i = 0; i < scale.length; i++)
            this.scale[i] = (float) scale[i];
    }
}
