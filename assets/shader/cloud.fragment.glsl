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
#if defined(numDirectionalLights) && (numDirectionalLights > 0)
#define directionalLightsFlag
#endif // numDirectionalLights

#ifdef directionalLightsFlag
struct DirectionalLight
{
    vec3 color;
    vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
#endif // directionalLightsFlag

#if defined(numPointLights) && (numPointLights > 0)
#define pointLightsFlag
#endif// numPointLights

#ifdef pointLightsFlag
struct PointLight {
    vec3 color;
    vec3 position;
    float intensity;
};
uniform PointLight u_pointLights[numPointLights];
#endif // numPointLights

in vec3 v_viewDir;
in vec3 v_fragPosWorld;
in mat3 v_tbn;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 layerBuffer;

#include <shader/lib/logdepthbuff.glsl>

#define saturate(x) clamp(x, 0.0, 1.0)

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    vec2 g_texCoord0 = v_texCoord0;

    vec4 cloud = fetchCloudColor(g_texCoord0, vec4(0.0, 0.0, 0.0, 0.0));
    vec3 ambient = v_ambientLight;

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

    // Stores lighting contribution.
    vec3 litColor = vec3(0.0, 0.0, 0.0);

    // DIRECTIONAL LIGHTS
    #ifdef directionalLightsFlag
    // Loop for directional light contributions.
    for (int i = 0; i < numDirectionalLights; i++) {
        // Normal in pixel space.
        vec3 lightCol = u_dirLights[i].color;
        vec3 N = vec3(0.0, 0.0, 1.0);
        vec3 L = normalize(u_dirLights[i].direction * v_tbn);
        float NL = clamp(dot(N, L) * 2.0, 0.0, 1.0);

        litColor += lightCol * NL;
    }
    #endif // directionalLightsFlag

    // POINT LIGHTS
    #ifdef pointLightsFlag
    // Loop for point light contributions.
    for (int i = 0; i < numPointLights; i++) {
        // Normal in pixel space.
        vec3 lightCol = u_pointLights[i].color * u_pointLights[i].intensity;
        vec3 N = vec3(0.0, 0.0, 1.0);
        vec3 L = normalize((u_pointLights[i].position - v_fragPosWorld) * v_tbn);
        float NL = clamp(dot(N, L) * 2.0, 0.0, 1.0);

        litColor += lightCol * NL;
    }
    #endif // pointLightsFlag

    float brightness = clamp(length(litColor + ambient), 0.1, 1.0);
    vec3 cloudColor = cloud.rgb * brightness;

    fragColor = vec4(cloudColor * shdw, 1.0) * v_opacity;

    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
