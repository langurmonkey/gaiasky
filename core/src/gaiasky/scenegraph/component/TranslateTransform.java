/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.math.Matrix4;

public class TranslateTransform implements ITransform {
    /** Translation **/
    float[] vector;

    public void apply(Matrix4 mat){
        mat.translate(vector[0], vector[1], vector[2]);
    }
    
    public void setVector(double[] vector){
        this.vector = new float[vector.length];
        for(int i =0; i< vector.length; i++)
            this.vector[i] = (float) vector[i];
    }
}
