#version 330 core

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() { }

in vec4 v_position;
#define pullPosition() { return v_position; }


in vec2 v_texCoord0;

// Uniforms which are always available
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

// Varyings computed in the vertex shader
in float v_opacity;
in float v_alphaTest;

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef diffuseCubemapFlag
uniform samplerCube u_diffuseCubemap;
#endif

#ifdef svtCacheTextureFlag
uniform sampler2D u_svtCacheTexture;
#endif

#ifdef svtIndirectionDiffuseTextureFlag
uniform sampler2D u_svtIndirectionDiffuseTexture;
#endif

// AMBIENT LIGHT
in vec3 v_ambientLight;

#include <shader/lib/luma.glsl>

// CLOUD TEXTURE
#if defined(svtIndirectionDiffuseTextureFlag)
#include <shader/lib/svt.glsl>
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    return texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionDiffuseTexture, texCoord));
}
#elif defined(diffuseCubemapFlag)
#include <shader/lib/cubemap.glsl>
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    return texture(u_diffuseCubemap, UVtoXYZ(texCoord));
}
#elif defined(diffuseTextureFlag)
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    return texture(u_diffuseTexture, texCoord);
}
#else
vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
    return defaultValue;
}
#endif// diffuseCubemapFlag && diffuseTextureFlag

#ifdef eclipsingBodyFlag
uniform float u_eclipsingBodyRadius;
uniform vec3 u_eclipsingBodyPos;

#include <shader/lib/math.glsl>
#endif // eclipsingBodyFlag


//////////////////////////////////////////////////////
////// DIRECTIONAL LIGHTS
//////////////////////////////////////////////////////
#ifdef lightingFlag
#if defined(numDirectionalLights) && (numDirectionalLights > 0)
#define directionalLightsFlag
#endif // numDirectionalLights
#endif //lightingFlag

#ifdef directionalLightsFlag
struct DirectionalLight
{
    vec3 color;
    vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
#endif

in vec3 v_lightDir;
in vec3 v_lightCol;
in vec3 v_viewDir;
in vec3 v_fragPosWorld;

layout (location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#ifdef velocityBufferFlag
#include <shader/lib/velbuffer.frag.glsl>
#endif

void main() {
    vec2 g_texCoord0 = v_texCoord0;

    vec4 cloud = fetchCloudColor(g_texCoord0, vec4(0.0, 0.0, 0.0, 0.0));
    vec3 ambient = v_ambientLight;

    // Normal in pixel space
    vec3 N = vec3(0.0, 0.0, 1.0);
    vec3 L = normalize(v_lightDir);
    float NL = clamp(dot(N, L) * 2.0, 0.0, 1.0);

    float shdw = 1.0;
    // Eclipses
    #ifdef eclipsingBodyFlag
    vec3 f = v_fragPosWorld;
    vec3 m = u_eclipsingBodyPos;
    vec3 l = -u_dirLights[0].direction;
    vec3 fl = f + l;
    float dist = dist_segment_point(f, fl, m);
    if (dist < u_eclipsingBodyRadius) {
        shdw *= dist / u_eclipsingBodyRadius;
    }
    #endif // eclipsingBodyFlag

    vec3 cloudColor = clamp(v_lightCol * cloud.rgb, 0.0, 1.0) * shdw;
    float opacity = v_opacity * clamp(NL + luma(ambient), 0.0, 1.0) * 1.3;
    fragColor = vec4(cloudColor, cloud.a) * opacity;

    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}