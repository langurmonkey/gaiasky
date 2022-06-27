/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.math.Matrix4d;

public class ScaleTransform implements ITransform {
    /** Scale **/
    double[] scale;

    public void apply(Matrix4 mat) {
        mat.scale((float) scale[0], (float) scale[1], (float) scale[2]);
    }
    public void apply(Matrix4d mat) {
        mat.scale(scale[0], scale[1], scale[2]);
    }

    public void setScale(double[] scale) {
        this.scale = scale;
    }
}
