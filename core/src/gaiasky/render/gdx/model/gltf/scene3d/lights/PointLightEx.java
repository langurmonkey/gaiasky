/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.gdx.model.gltf.data.extensions.KHRLightsPunctual;

public class PointLightEx extends PointLight {
	/** Optional range in meters.
	 * see {@link KHRLightsPunctual.GLTFLight#range} */
	public Float range;
	
	@Override
	public PointLight set (PointLight copyFrom) {
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
