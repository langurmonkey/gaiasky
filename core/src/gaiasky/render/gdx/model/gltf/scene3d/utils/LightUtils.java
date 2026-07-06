/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.utils;

import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import gaiasky.render.gdx.shader.Environment;
import gaiasky.render.gdx.shader.attribute.DirectionalLightsAttribute;
import gaiasky.render.gdx.shader.attribute.PointLightsAttribute;
import gaiasky.render.gdx.shader.attribute.SpotLightsAttribute;

public class LightUtils {

	public static class LightsInfo{
		public int dirLights;
		public int pointLights;
		public int spotLights;
		public int miscLights;
		
		public void reset(){
			dirLights = 0;
			pointLights = 0;
			spotLights = 0;
			miscLights = 0;
		}
	}
	
	public static LightsInfo getLightsInfo(LightsInfo info, Environment environment){
		info.reset();
		DirectionalLightsAttribute dla = environment.get(DirectionalLightsAttribute.class, DirectionalLightsAttribute.Type);
		if(dla != null) info.dirLights = dla.lights.size;
		PointLightsAttribute pla = environment.get(PointLightsAttribute.class, PointLightsAttribute.Type);
		if(pla != null) info.pointLights = pla.lights.size;
		SpotLightsAttribute sla = environment.get(SpotLightsAttribute.class, SpotLightsAttribute.Type);
		if(sla != null) info.spotLights = sla.lights.size;
		return info;
	}
	
	public static LightsInfo getLightsInfo(LightsInfo info, Iterable<BaseLight> lights){
		info.reset();
		for(BaseLight light : lights){
			if(light instanceof DirectionalLight){
				info.dirLights++;
			}else if(light instanceof PointLight){
				info.pointLights++;
			}else if(light instanceof SpotLight){
				info.spotLights++;
			}else{
				info.miscLights++;
			}
		}
		return info;
	}
}
