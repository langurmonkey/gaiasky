#version 330 core

#if defined(specularColorFlag)
#define specularFlag
#endif

#if defined(specularFlag)
#define cameraPositionFlag
#endif

in vec3 a_position;
uniform mat4 u_projViewTrans;

#if defined(colorFlag)
out vec4 v_color;
in vec4 a_color;
#endif // colorFlag

#ifdef normalFlag
in vec3 a_normal;
uniform mat3 u_normalMatrix;
out vec3 v_normal;
#endif // normalFlag

in vec2 a_texCoord0;
out vec2 v_texCoords0;

uniform mat4 u_worldTrans;
uniform float u_vrScale;

#ifdef timeFlag
uniform float u_time;
#else
const float u_time = 0.0;
#endif // timeFlag
out float v_time;

#ifdef blendedFlag
uniform float u_opacity;
out float v_opacity;

#ifdef alphaTestFlag
uniform float u_alphaTest;
out float v_alphaTest;
#endif //alphaTestFlag
#endif // blendedFlag

#ifdef lightingFlag
out vec3 v_lightDiffuse;

#ifdef ambientLightFlag
uniform vec3 u_ambientLight;
#endif // ambientLightFlag

#ifdef ambientCubemapFlag
uniform vec3 u_ambientCubemap[6];
#endif // ambientCubemapFlag 

#ifdef sphericalHarmonicsFlag
uniform vec3 u_sphericalHarmonics[9];
#endif //sphericalHarmonicsFlag

#ifdef cameraPositionFlag
uniform vec4 u_cameraPosition;
#endif // cameraPositionFlag

out vec3 v_viewVec;

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

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#include <shader/lib/shadowmap.vert.glsl>

#if defined(ambientFlag) && defined(separateAmbientFlag)
out vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif // lightingFlag

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#if defined(relativisticEffects)
#include <shader/lib/geometry.glsl>
#endif // relativisticEffects

////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

out float v_depth;


void main() {
	v_time = u_time;
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

	vec4 gpos = u_projViewTrans * pos;
	gl_Position = gpos;

	#ifdef shadowMapFlag
	getShadowMapUv(pos, v_shadowMapUv);
	#ifdef shadowMapGlobalFlag
	getShadowMapUvGlobal(pos, v_shadowMapUvGlobal);
	#endif // shadowMapGlobalFlag
	#ifdef numCSM
	getCsmLightSpacePos(pos, v_csmLightSpacePos);
	#endif // numCSM
	#endif // shadowMapFlag

	#if defined(normalFlag)
		vec3 normal = normalize(u_normalMatrix * a_normal);
		v_normal = normal;
	#endif // normalFlag

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
			vec3 viewVec = normalize(u_cameraPosition.xyz - pos.xyz);
			v_viewVec = viewVec;
		#endif // specularFlag

		#if defined(numDirectionalLights) && (numDirectionalLights > 0) && defined(normalFlag)
			for (int i = 0; i < numDirectionalLights; i++) {
				vec3 lightDir = -u_dirLights[i].direction;
				float NdotL = clamp(dot(normal, lightDir), 0.0, 1.0);
				vec3 value = u_dirLights[i].color * NdotL;
				v_lightDiffuse += value;
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
			}
		#endif // numPointLights
	#endif // lightingFlag
}