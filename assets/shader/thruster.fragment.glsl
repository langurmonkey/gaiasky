#version 330 core

#if defined(colorFlag)
in vec4 v_color;
#endif

#ifdef blendedFlag
in float v_opacity;
#ifdef alphaTestFlag
in float v_alphaTest;
#endif //alphaTestFlag
#endif //blendedFlag

#if defined(diffuseTextureFlag)
#define textureFlag
in vec2 v_texCoords0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef lightingFlag
in vec3 v_lightDiffuse;

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
in vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif //lightingFlag

// Time in seconds, comes from u_shininess uniform
in float v_time;
// View vector
in vec3 v_viewVec;
// The normal
in vec3 v_normal;

#include <shader/lib/logdepthbuff.glsl>
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#define PI 3.14159

void main() {
	vec2 tc = v_texCoords0;
	tc.y = tc.y - v_time * 4.0;
	// mod
	tc.y = tc.y - floor(tc.y);

	// Mask low ys and high ys
	float mask = pow(sin(v_texCoords0.y * PI), 2.0);

	#if defined(diffuseTextureFlag) && defined(diffuseColorFlag) && defined(colorFlag)
	vec4 diffuse = texture(u_diffuseTexture, tc) * u_diffuseColor * v_color;
	#elif defined(diffuseTextureFlag) && defined(diffuseColorFlag)
	vec4 diffuse = texture(u_diffuseTexture, tc) * u_diffuseColor;
	#endif

	#if (!defined(lightingFlag))
	fragColor.rgb = diffuse.rgb;
	#elif (!defined(specularFlag))
	#if defined(ambientFlag) && defined(separateAmbientFlag)
	fragColor.rgb = (diffuse.rgb * (v_ambientLight + v_lightDiffuse));
	#else
	fragColor.rgb = (diffuse.rgb * v_lightDiffuse);
	#endif
	#else

	#if defined(ambientFlag) && defined(separateAmbientFlag)
	fragColor.rgb = (diffuse.rgb * (v_lightDiffuse + v_ambientLight));
	#else
	fragColor.rgb = (diffuse.rgb * v_lightDiffuse) + (night.rgb * (max(0.0, (0.6 - length(v_lightDiffuse)))));
	#endif
	#endif //lightingFlag

	#ifdef blendedFlag
	fragColor.a = diffuse.a * v_opacity;
	#ifdef alphaTestFlag
	if (fragColor.a <= v_alphaTest)
		discard;
	#endif
	#else
	fragColor.a = 1.0;
	#endif

	// Mask it
	fragColor *= mask;

	// Prevent saturation
	fragColor.rgb = clamp(fragColor.rgb, 0.0, 0.98);

	gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
	layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

	#ifdef ssrFlag
	ssrBuffers();
	#endif // ssrFlag
}
