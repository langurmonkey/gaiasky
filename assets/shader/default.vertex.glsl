#version 120

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#if defined(specularFlag) || defined(fogFlag)
#define cameraPositionFlag
#endif

attribute vec3 a_position;
uniform mat4 u_projViewTrans;

#if defined(colorFlag)
varying vec4 v_color;
attribute vec4 a_color;
#endif // colorFlag

#ifdef normalFlag
attribute vec3 a_normal;
uniform mat3 u_normalMatrix;
varying vec3 v_normal;
#endif // normalFlag

attribute vec2 a_texCoord0;
varying vec2 v_texCoords0;

uniform mat4 u_worldTrans;

#ifdef shininessFlag
uniform float u_shininess;
#else
const float u_shininess = 20.0;
#endif // shininessFlag
varying float v_time;

#ifdef blendedFlag
uniform float u_opacity;
varying float v_opacity;

#ifdef alphaTestFlag
uniform float u_alphaTest;
varying float v_alphaTest;
#endif //alphaTestFlag
#endif // blendedFlag

#ifdef lightingFlag
varying vec3 v_lightDiffuse;

#ifdef ambientLightFlag
uniform vec3 u_ambientLight;
#endif // ambientLightFlag

#ifdef ambientCubemapFlag
uniform vec3 u_ambientCubemap[6];
#endif // ambientCubemapFlag 

#ifdef sphericalHarmonicsFlag
uniform vec3 u_sphericalHarmonics[9];
#endif //sphericalHarmonicsFlag

#ifdef specularFlag
varying vec3 v_lightSpecular;
#endif // specularFlag

#ifdef cameraPositionFlag
uniform vec4 u_cameraPosition;
#endif // cameraPositionFlag

#ifdef fogFlag
varying float v_fog;
#endif // fogFlag

varying vec3 v_viewVec;

#if defined(numDirectionalLights) && (numDirectionalLights > 0)
struct DirectionalLight
{
	vec3 color;
	vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
#endif // numDirectionalLights

#if defined(numPointLights) && (numPointLights > 0)
struct PointLight
{
	vec3 color;
	vec3 position;
};
uniform PointLight u_pointLights[numPointLights];
#endif // numPointLights

#if defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#ifdef shadowMapFlag
uniform mat4 u_shadowMapProjViewTrans;
varying vec3 v_shadowMapUv;
#define separateAmbientFlag
#endif //shadowMapFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
varying vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif // lightingFlag

////////////////////////////////////////////////////////////////////////////////////
////////// GROUND ATMOSPHERIC SCATTERING - VERTEX
////////////////////////////////////////////////////////////////////////////////////
varying vec3 v_atmosphereColor;
#ifdef atmosphereGround
    uniform vec3 v3PlanetPos; /* The position of the planet */
    uniform vec3 v3CameraPos; /* The camera's current position*/
    uniform vec3 v3LightPos; /* The direction vector to the light source*/
    uniform vec3 v3InvWavelength; /* 1 / pow(wavelength, 4) for the red, green, and blue channels*/
    
    uniform float fCameraHeight;
    uniform float fCameraHeight2; /* fCameraHeight^2*/
    uniform float fOuterRadius; /* The outer (atmosphere) radius*/
    uniform float fOuterRadius2; /* fOuterRadius^2*/
    uniform float fInnerRadius; /* The inner (planetary) radius*/
    uniform float fKrESun; /* Kr * ESun*/
    uniform float fKmESun; /* Km * ESun*/
    uniform float fKr4PI; /* Kr * 4 * PI*/
    uniform float fKm4PI; /* Km * 4 * PI*/
    uniform float fScale; /* 1 / (fOuterRadius - fInnerRadius)*/
    uniform float fScaleDepth; /* The scale depth (i.e. the altitude at which the atmosphere's average density is found)*/
    uniform float fScaleOverScaleDepth; /* fScale / fScaleDepth*/
    
    uniform int nSamples;
    uniform float fSamples;
    
    
    float scale(float fCos)
    {
    	float x = 1.0 - fCos;
    	return fScaleDepth * exp(-0.00287 + x*(0.459 + x*(3.83 + x*(-6.80 + x*5.25))));
    }
    
    float getNearIntersection(vec3 pos, vec3 ray, float distance2, float radius2) {
        float B = 2.0 * dot (pos, ray);
        float C = distance2 - radius2;
        float fDet = max (0.0, B * B - 4.0 * C);
        return 0.5 * (-B - sqrt (fDet));
    }
    
    vec3 calculateAtmosphereGroundColor() {
		// Get the ray from the camera to the vertex and its length (which is the far point of the ray passing through the atmosphere)
		vec3 v3Pos = a_position * fOuterRadius;
		vec3 v3Ray = v3Pos - v3CameraPos;
		float fFar = length (v3Ray);
		v3Ray /= fFar;
	
		// Calculate the closest intersection of the ray with the outer atmosphere (which is the near point of the ray passing through the atmosphere)
		float fNear = getNearIntersection (v3CameraPos, v3Ray, fCameraHeight2, fOuterRadius2);
	
		// Calculate the ray's starting position, then calculate its scattering offset
		vec3 v3Start = v3CameraPos + v3Ray * fNear;
		fFar -= fNear;
		float fStartDepth = exp((fInnerRadius - fOuterRadius) / fScaleDepth);
		float fCameraAngle = dot(-v3Ray, a_position) / length(a_position);
		float fLightAngle = dot(v3LightPos, a_position) / length(a_position);
		float fCameraScale = scale(fCameraAngle);
		float fLightScale = scale(fLightAngle);
		float fCameraOffset = fStartDepth * fCameraScale;
		float fTemp = (fLightScale + fCameraScale);
	
		/* Initialize the scattering loop variables*/
		float fSampleLength = fFar / fSamples;
		float fScaledLength = fSampleLength * fScale;
		vec3 v3SampleRay = v3Ray * fSampleLength;
		vec3 v3SamplePoint = v3Start + v3SampleRay * 0.5;
	
		// Now loop through the sample rays
		vec3 v3FrontColor = vec3(0.0, 0.0, 0.0);
		vec3 v3Attenuate;
		for (int i = 0; i < 11; i++) {
		    float fHeight = length (v3SamplePoint);
		    float fDepth = exp (fScaleOverScaleDepth * (fInnerRadius - fHeight));
		    float fScatter = fDepth * fTemp - fCameraOffset;
		    
		    v3Attenuate = exp(-fScatter * (v3InvWavelength * fKr4PI + fKm4PI));
		    v3FrontColor += v3Attenuate * (fDepth * fScaledLength);
		    v3SamplePoint += v3SampleRay;
		}
		
		return vec3(v3FrontColor * (v3InvWavelength * fKrESun + fKmESun));
    }
#else
    vec3 calculateAtmosphereGroundColor() {
	return vec3(0.0, 0.0, 0.0);
    }
#endif // atmosphereGround


////////////////////////////////////////////////////////////////////////////////////
////////// RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
    uniform float u_vc; // v/c
    uniform vec3 u_velDir; // Camera velocity direction
    
    #include shader/lib_geometry.glsl
    #include shader/lib_relativity.glsl
#endif // relativisticEffects


////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
    uniform vec4 u_hterms; // hpluscos, hplussin, htimescos, htimessin
    uniform vec3 u_gw; // Location of gravitational wave, cartesian
    uniform mat3 u_gwmat3; // Rotation matrix so that u_gw = u_gw_mat * (0 0 1)^T
    uniform float u_ts; // Time in seconds since start
    uniform float u_omgw; // Wave frequency
    #include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

void main() {
	v_atmosphereColor = calculateAtmosphereGroundColor();
	v_time = u_shininess;
	v_texCoords0 = a_texCoord0;

	#if defined(colorFlag)
		v_color = a_color;
	#endif // colorFlag

	#ifdef blendedFlag
		v_opacity = u_opacity;
		#ifdef alphaTestFlag
			v_alphaTest = u_alphaTest;
		#endif //alphaTestFlag
	#endif // blendedFlag


	vec4 pos = u_worldTrans * vec4(a_position, 1.0);

        #ifdef relativisticEffects
            pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
        #endif // relativisticEffects
        
        #ifdef gravitationalWaves
            pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
        #endif // gravitationalWaves


	gl_Position = u_projViewTrans * pos;

	#ifdef shadowMapFlag
		vec4 spos = u_shadowMapProjViewTrans * pos;
		v_shadowMapUv.xy = (spos.xy / spos.w) * 0.5 + 0.5;
		v_shadowMapUv.z = min(spos.z * 0.5 + 0.5, 0.998);
	#endif //shadowMapFlag

	#if defined(normalFlag)
		vec3 normal = normalize(u_normalMatrix * a_normal);
		v_normal = normal;
	#endif // normalFlag

        #ifdef fogFlag
            vec3 flen = u_cameraPosition.xyz - pos.xyz;
            float fog = dot(flen, flen) * u_cameraPosition.w;
            v_fog = min(fog, 1.0);
        #endif

	#ifdef lightingFlag
		#if	defined(ambientLightFlag)
        	    vec3 ambientLight = u_ambientLight;
		#elif defined(ambientFlag)
        	    vec3 ambientLight = vec3(0.0);
		#endif

		#ifdef ambientCubemapFlag 		
			vec3 squaredNormal = normal * normal;
			vec3 isPositive  = step(0.0, normal);
			ambientLight += squaredNormal.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], isPositive.x) +
					squaredNormal.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], isPositive.y) +
					squaredNormal.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], isPositive.z);
		#endif // ambientCubemapFlag

		#ifdef sphericalHarmonicsFlag
			ambientLight += u_sphericalHarmonics[0];
			ambientLight += u_sphericalHarmonics[1] * normal.x;
			ambientLight += u_sphericalHarmonics[2] * normal.y;
			ambientLight += u_sphericalHarmonics[3] * normal.z;
			ambientLight += u_sphericalHarmonics[4] * (normal.x * normal.z);
			ambientLight += u_sphericalHarmonics[5] * (normal.z * normal.y);
			ambientLight += u_sphericalHarmonics[6] * (normal.y * normal.x);
			ambientLight += u_sphericalHarmonics[7] * (3.0 * normal.z * normal.z - 1.0);
			ambientLight += u_sphericalHarmonics[8] * (normal.x * normal.x - normal.y * normal.y);			
		#endif // sphericalHarmonicsFlag

		#ifdef ambientFlag
			#ifdef separateAmbientFlag
				v_ambientLight = ambientLight;
				v_lightDiffuse = vec3(0.0);
			#else
				v_lightDiffuse = ambientLight;
			#endif //separateAmbientFlag
		#else
	        v_lightDiffuse = vec3(0.0);
		#endif //ambientFlag


		#ifdef specularFlag
			v_lightSpecular = vec3(0.0);
			vec3 viewVec = normalize(u_cameraPosition.xyz - pos.xyz);
			v_viewVec = viewVec;
		#endif // specularFlag

		#if defined(numDirectionalLights) && (numDirectionalLights > 0) && defined(normalFlag)
			for (int i = 0; i < numDirectionalLights; i++) {
				vec3 lightDir = -u_dirLights[i].direction;
				float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
				vec3 value = u_dirLights[i].color * NdotL;
				v_lightDiffuse += value;
                                
                #ifdef cameraPositionFlag
                    // Add light to tangent zones
                    vec3 view = normalize(u_cameraPosition.xyz - pos.xyz);
                    float VdotN = 1.0 - dot(view, normal);
                    v_lightDiffuse += pow(VdotN, 10.0) * NdotL * 0.1;
                #endif // cameraPositionFlag

				#ifdef specularFlag
					float halfDotView = max(0.0, dot(normal, normalize(lightDir + viewVec)));
					v_lightSpecular += value * pow(halfDotView, u_shininess);
				#endif // specularFlag
			}
		#endif // numDirectionalLights

		#if defined(numPointLights) && (numPointLights > 0) && defined(normalFlag)
			for (int i = 0; i < numPointLights; i++) {
				vec3 lightDir = u_pointLights[i].position - pos.xyz;
				float dist2 = dot(lightDir, lightDir);
				lightDir *= inversesqrt(dist2);
				float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
				vec3 value = u_pointLights[i].color * (NdotL / (1.0 + dist2));
				v_lightDiffuse += value;
				#ifdef specularFlag
					float halfDotView = max(0.0, dot(normal, normalize(lightDir + viewVec)));
					v_lightSpecular += value * pow(halfDotView, u_shininess);
				#endif // specularFlag
			}
		#endif // numPointLights
	#endif // lightingFlag
}