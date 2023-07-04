/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.utils;

import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.attribute.DirectionalLightsAttribute;
import gaiasky.util.gdx.shader.attribute.PointLightsAttribute;
import gaiasky.util.gdx.shader.attribute.SpotLightsAttribute;

public class LightUtils {

	public static class LightsInfo{
		public int dirLights = 0;
		public int pointLights = 0;
		public int spotLights = 0;
		public int miscLights = 0;
		
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
