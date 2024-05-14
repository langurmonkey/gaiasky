#version 330 core

#define TEXTURE_LOD_BIAS 0.0

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#ifdef normalFlag
in vec3 v_normal;
#endif //normalFlag

#if defined(colorFlag)
in vec4 v_color;
#endif

#ifdef blendedFlag
in float v_opacity;
#ifdef alphaTestFlag
in float v_alphaTest;
#endif //alphaTestFlag
#endif //blendedFlag

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
in vec2 v_texCoords0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif

#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
#endif

#ifdef lightingFlag
in vec3 v_lightDiffuse;

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#if defined(emissiveTextureFlag) || defined(emissiveColorFlag)
#define emissiveFlag
#endif

#ifdef specularFlag
in vec3 v_lightSpecular;
#endif //specularFlag

// COLOR EMISSIVE
#if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
#define fetchColorEmissiveTD(texCoord) texture(u_emissiveTexture, texCoord) * 1.5 + u_emissiveColor * 2.0
#elif defined(emissiveTextureFlag)
#define fetchColorEmissiveTD(texCoord) texture(u_emissiveTexture, texCoord) * 1.5
#elif defined(emissiveColorFlag)
#define fetchColorEmissiveTD(texCoord) u_emissiveColor * 2.0
#else
#define fetchColorEmissiveTD(texCoord) vec4(0.0)
#endif // emissiveTextureFlag && emissiveColorFlag

// SHADOW MAPPING
#include <shader/lib/shadowmap.glsl>
#ifdef shadowMapFlag
in vec3 v_shadowMapUv;
#define separateAmbientFlag
#endif //shadowMapFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
in vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif //lightingFlag

#ifdef fogFlag
uniform vec4 u_fogColor;
in float v_fog;
#endif // fogFlag

#include <shader/lib/logdepthbuff.glsl>
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
	vec4 emissive = fetchColorEmissiveTD(v_texCoords0);
	#ifdef atmosphereGround
	vec3 night = emissive.rgb;
	emissive = vec4(0.0);
	#else
	vec3 night = vec3(0.0);
	#endif

	#if defined(normalFlag) 
		vec3 normal = v_normal;
	#endif // normalFlag

	#if defined(diffuseTextureFlag) && defined(diffuseColorFlag) && defined(colorFlag)
		vec4 diffuse = texture(u_diffuseTexture, v_texCoords0, TEXTURE_LOD_BIAS) * u_diffuseColor * v_color;
	#elif defined(diffuseTextureFlag) && defined(diffuseColorFlag)
		vec4 diffuse = texture(u_diffuseTexture, v_texCoords0, TEXTURE_LOD_BIAS) * u_diffuseColor;
	#elif defined(diffuseTextureFlag) && defined(colorFlag)
		vec4 diffuse = texture(u_diffuseTexture, v_texCoords0, TEXTURE_LOD_BIAS) * v_color;
	#elif defined(diffuseTextureFlag)
		vec4 diffuse = texture(u_diffuseTexture, v_texCoords0, TEXTURE_LOD_BIAS);
	#elif defined(diffuseColorFlag) && defined(colorFlag)
		vec4 diffuse = u_diffuseColor * v_color;
	#elif defined(diffuseColorFlag)
		vec4 diffuse = u_diffuseColor;
	#elif defined(colorFlag)
		vec4 diffuse = v_color;
	#else
		vec4 diffuse = vec4(1.0);
	#endif

	#if (!defined(lightingFlag))  
		fragColor.rgb = diffuse.rgb;
	#elif (!defined(specularFlag))
		#if defined(ambientFlag) && defined(separateAmbientFlag)
			#ifdef shadowMapFlag
				fragColor.rgb = (diffuse.rgb * (v_ambientLight + getShadow(u_shadowMapUv) * v_lightDiffuse));
			#else
				fragColor.rgb = (diffuse.rgb * (v_ambientLight + v_lightDiffuse));
			#endif //shadowMapFlag
		#else
			#ifdef shadowMapFlag
				fragColor.rgb = getShadow(u_shadowMapUv) * (diffuse.rgb * v_lightDiffuse);
			#else
				fragColor.rgb = (diffuse.rgb * v_lightDiffuse);
			#endif //shadowMapFlag
		#endif
	#else
		#if defined(specularTextureFlag) && defined(specularColorFlag)
			vec3 specular = texture(u_specularTexture, v_texCoords0, TEXTURE_LOD_BIAS).rgb * u_specularColor.rgb * v_lightSpecular;
		#elif defined(specularTextureFlag)
			vec3 specular = texture(u_specularTexture, v_texCoords0, TEXTURE_LOD_BIAS).rgb * v_lightSpecular;
		#elif defined(specularColorFlag)
			vec3 specular = u_specularColor.rgb * v_lightSpecular;
		#else
			vec3 specular = v_lightSpecular;
		#endif

		#if defined(ambientFlag) && defined(separateAmbientFlag)
			#ifdef shadowMapFlag
				fragColor.rgb = (diffuse.rgb * (getShadow(u_shadowMapUv) * v_lightDiffuse + v_ambientLight)) + emissive.rgb + specular;
			#else
				fragColor.rgb = (diffuse.rgb * (v_lightDiffuse + v_ambientLight)) + emissive.rgb + specular;
			#endif //shadowMapFlag
		#else
			#ifdef shadowMapFlag
				fragColor.rgb = getShadow(u_shadowMapUv) * ((diffuse.rgb * v_lightDiffuse) + specular) + emissive.rgb;
			#else
				fragColor.rgb = (diffuse.rgb * v_lightDiffuse) + (night.rgb * (max(0.0, (0.6 - length(v_lightDiffuse))))) + emissive.rgb + specular;
			#endif //shadowMapFlag
		#endif
	#endif //lightingFlag

	#ifdef fogFlag
		fragColor.rgb = mix(fragColor.rgb, u_fogColor.rgb, v_fog);
	#endif // end fogFlag

	#ifdef blendedFlag
		fragColor.a = diffuse.a * v_opacity;
		#ifdef alphaTestFlag
			if (fragColor.a <= v_alphaTest)
				discard;
		#endif
	#else
		fragColor.a = 1.0;
	#endif

	gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

	#ifdef ssrFlag
	ssrBuffers();
	#endif // ssrFlag
}