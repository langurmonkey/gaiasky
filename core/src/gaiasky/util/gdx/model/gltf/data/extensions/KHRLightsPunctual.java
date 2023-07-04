/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.data.extensions;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.model.gltf.data.GLTF;
import gaiasky.util.gdx.model.gltf.data.GLTFObject;
import gaiasky.util.gdx.model.gltf.data.scene.GLTFNode;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFIllegalException;
import gaiasky.util.gdx.model.gltf.loaders.shared.GLTFTypes;
import gaiasky.util.gdx.model.gltf.scene3d.lights.DirectionalLightEx;
import gaiasky.util.gdx.model.gltf.scene3d.lights.PointLightEx;
import gaiasky.util.gdx.model.gltf.scene3d.lights.SpotLightEx;

abstract public class KHRLightsPunctual {
	
	public static final String EXT = "KHR_lights_punctual";
	
	
	public static class GLTFSpotLight {
		public float innerConeAngle = 0;
		public float outerConeAngle = MathUtils.PI / 4f;
	}

	public static class GLTFLight extends GLTFObject {
		public static final String TYPE_DIRECTIONAL = "directional";
		public static final String TYPE_POINT = "point";
		public static final String TYPE_SPOT = "spot";
		
		public String name = "";
		public float [] color = {1f, 1f, 1f};
		
		/** 
		 * in Candela for point/spot lights : Ev(lx) = Iv(cd) / (d(m))2 
		 * in Lux for directional lights : Ev(lx)
		 */
		public float intensity = 1f;
		public String type;
		
		/** 
		 * Hint defining a distance cutoff at which the light's intensity may be considered to have reached zero. 
		 * When null, range is assumed to be infinite.
		 */
		public Float range;
		
		public GLTFSpotLight spot;
	}
	public static class GLTFLights {
		public Array<GLTFLight> lights;
	}
	public static class GLTFLightNode {
		public Integer light;
	}
	
	
	public static BaseLight map(GLTFLight light) {
		if(GLTFLight.TYPE_DIRECTIONAL.equals(light.type)){
			DirectionalLightEx dl = new DirectionalLightEx();
			dl.baseColor.set(GLTFTypes.mapColor(light.color, Color.WHITE));
			dl.intensity = light.intensity;
			return dl;
		}else if(GLTFLight.TYPE_POINT.equals(light.type)){
			PointLightEx pl = new PointLightEx();
			pl.color.set(GLTFTypes.mapColor(light.color, Color.WHITE));
			// Blender exported intensity is the raw value in Watts
			// GLTF spec. states it's in Candela which is lumens per square radian (lm/sr).
			// adjustement is made empirically here (comparing with Blender rendering)
			// TODO find if it's a GLTF Blender exporter issue and find the right conversion.
			pl.intensity = light.intensity / 10f;
			pl.range = light.range;
			return pl;
		}else if(GLTFLight.TYPE_SPOT.equals(light.type)){
			SpotLightEx sl = new SpotLightEx();
			if(light.spot == null) throw new GLTFIllegalException("spot property required for spot light type");
			sl.color.set(GLTFTypes.mapColor(light.color, Color.WHITE));
			
			// same hack as point lights (see point light above)
			sl.intensity = light.intensity / 10f;
			sl.range = light.range;
			
			sl.setConeRad(light.spot.outerConeAngle, light.spot.innerConeAngle);
			
			return sl;
		} else{
			throw new GLTFIllegalException("unsupported light type " + light.type);
		}
	}
}
