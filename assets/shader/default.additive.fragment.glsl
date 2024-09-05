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
#include <shader/lib/shadowmap.frag.glsl>
#ifdef shadowMapFlag
#define separateAmbientFlag
in vec3 v_shadowMapUv;
#ifdef shadowMapGlobalFlag
in vec3 v_shadowMapUvGlobal;
#endif // shadowMapGlobalFlag
#ifdef numCSM
in vec3 v_csmLightSpacePos[numCSM];
#endif // numCSM
#endif //shadowMapFlag

in vec3 v_fragPosWorld;

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
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
	vec4 emissive = fetchColorEmissiveTD(v_texCoords0);
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

	// Shadow mapping.
	#ifdef shadowMapFlag
		#ifdef numCSM
			// Cascaded shadow mapping.
			float shdw = clamp(getShadow(v_shadowMapUv, v_csmLightSpacePos, length(v_fragPosWorld)), 0.0, 1.0);
		#else
			// Regular shadow mapping.
			float transparency = 1.0 - texture(u_shadowTexture, v_shadowMapUv.xy).g;

			#ifdef shadowMapGlobalFlag
				float shdw = clamp(getShadow(v_shadowMapUv, v_shadowMapUvGlobal) + transparency, 0.0, 1.0);
			#else
				float shdw = clamp(getShadow(v_shadowMapUv) + transparency, 0.0, 1.0);
			#endif // shadowMapGlobalFlag
		#endif // numCSM
	#else
		float shdw = 1.0;
	#endif // shadowMapFlag

	#if (!defined(lightingFlag))  
		fragColor.rgb = diffuse.rgb;
	#elif (!defined(specularFlag))

		#if defined(ambientFlag) && defined(separateAmbientFlag)
			fragColor.rgb = (diffuse.rgb * (v_ambientLight + shdw * v_lightDiffuse));
		#else
			fragColor.rgb = shdw * (diffuse.rgb * v_lightDiffuse);
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
			fragColor.rgb = (diffuse.rgb * (shdw * v_lightDiffuse + v_ambientLight)) + emissive.rgb + specular;
		#else
			fragColor.rgb = shdw * ((diffuse.rgb * v_lightDiffuse) + specular) + emissive.rgb;
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

	fragColor.rgb *= fragColor.a;
	
	// Prevent saturation
    fragColor = clamp(fragColor, 0.0, 1.0);

	gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
	layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

	#ifdef ssrFlag
	ssrBuffers();
	#endif // ssrFlag

}