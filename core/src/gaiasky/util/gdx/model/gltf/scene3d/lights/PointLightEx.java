/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.model.gltf.data.extensions.KHRLightsPunctual;

public class PointLightEx extends PointLight {
	/** Optional range in meters.
	 * see {@link KHRLightsPunctual.GLTFLight#range} */
	public Float range;
	
	@Override
	public PointLight set (final PointLight copyFrom) {
		if(copyFrom instanceof PointLightEx){
			return set(copyFrom.color, copyFrom.position, copyFrom.intensity, ((PointLightEx)copyFrom).range);
		}else{
			return set(copyFrom.color, copyFrom.position, copyFrom.intensity);
		}
	}

	public PointLightEx set(Color color, Vector3 position, float intensity, Float range) {
		super.set(color, position, intensity);
		this.range = range;
		return this;
	}
	
}
