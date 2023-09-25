#version 330 core

#if defined(colorFlag)
in vec4 v_color;
#endif

#ifdef blendedFlag
in float v_opacity;
#endif //blendedFlag

#if defined(diffuseTextureFlag) || defined(specularTextureFlag) || defined(diffuseCubemapFlag)
#define textureFlag
in vec2 v_texCoords0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef diffuseCubemapFlag
uniform samplerCube u_diffuseCubemap;
#endif

#ifdef lightingFlag
in vec3 v_lightDiffuse;

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#ifdef specularFlag
in vec3 v_lightSpecular;
#endif //specularFlag

#if defined(ambientFlag) && defined(separateAmbientFlag)
in vec3 v_ambientLight;
#endif //separateAmbientFlag

#endif //lightingFlag

#include <shader/lib/logdepthbuff.glsl>
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#ifdef velocityBufferFlag
#include <shader/lib/velbuffer.frag.glsl>
#endif

#ifdef cubemapFlag
#include <shader/lib/cubemap.glsl>
#endif // cubemapFlag

void main() {
	#if defined(diffuseTextureFlag) && defined(diffuseColorFlag) && defined(colorFlag)
	vec4 diffuse = texture(u_diffuseTexture, v_texCoords0) * u_diffuseColor * v_color;
	#elif defined(diffuseTextureFlag) && defined(diffuseColorFlag)
	vec4 diffuse = texture(u_diffuseTexture, v_texCoords0) * u_diffuseColor;
	#elif defined(diffuseTextureFlag) && defined(colorFlag)
	vec4 diffuse = texture(u_diffuseTexture, v_texCoords0) * v_color;
	#elif defined(diffuseTextureFlag)
	vec4 diffuse = texture(u_diffuseTexture, v_texCoords0);

	#elif defined(diffuseCubemapFlag) && defined(diffuseColorFlag) && defined(colorFlag)
	vec4 diffuse = texture(u_diffuseCubemap, UVtoXYZ(v_texCoords0)) * u_diffuseColor * v_color;
	#elif defined(diffuseCubemapFlag) && defined(diffuseColorFlag)
	vec4 diffuse = texture(u_diffuseCubemap, UVtoXYZ(v_texCoords0)) * u_diffuseColor;
	#elif defined(diffuseCubemapFlag) && defined(colorFlag)
	vec4 diffuse = texture(u_diffuseCubemap, UVtoXYZ(v_texCoords0)) * v_color;
	#elif defined(diffuseCubemapFlag)
	vec4 diffuse = texture(u_diffuseCubemap, UVtoXYZ(v_texCoords0));

	#elif defined(diffuseColorFlag) && defined(colorFlag)
	vec4 diffuse = u_diffuseColor * v_color;
	#elif defined(diffuseColorFlag)
	vec4 diffuse = u_diffuseColor;
	#elif defined(colorFlag)
	vec4 diffuse = v_color;
	#else
	vec4 diffuse = vec4(1.0);
	#endif

	fragColor = diffuse * v_opacity;

	// Prevent saturation
    fragColor = clamp(fragColor, 0.0, 1.0);

	gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

	#ifdef ssrFlag
	ssrBuffers();
	#endif // ssrFlag

	#ifdef velocityBufferFlag
	velocityBuffer();
	#endif
}