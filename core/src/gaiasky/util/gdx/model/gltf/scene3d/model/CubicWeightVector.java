/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.model;

public class CubicWeightVector extends WeightVector
{
	public final WeightVector tangentIn;
	public final WeightVector tangentOut;
	
	public CubicWeightVector(int count) {
		super(count);
		tangentIn = new WeightVector(count);
		tangentOut = new WeightVector(count);
	}

	
}
