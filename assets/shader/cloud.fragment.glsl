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
uniform float u_kmToU;
// The radius of the planet goes in here
uniform float u_generic1;

// Varyings computed in the vertex shader
in float v_opacity;
in float v_alphaTest;
in vec3 v_normal;

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

// Eclipses.
#include <shader/lib/eclipses.glsl>
// VR scale
#ifndef GLSL_VR_SCALE
#define GLSL_VR_SCALE
uniform float u_vrScale;
#endif //GLSL_VR_SCALE

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
in float v_fragDist;
in mat3 v_tbn;

layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 layerBuffer;

// Logarithmic depth buffer.
#include <shader/lib/logdepthbuff.glsl>
#include <shader/lib/math.glsl>

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

float cloudLimbFade(vec3 viewDir, vec3 normal, float fadePixels) {
    // The rim value at the silhouette
    float rim = 1.0 - max(0.0001, dot(viewDir, normal));

    // How many rim-units per pixel, right now, at this fragment
    float rimPerPixel = fwidth(rim);

    // Fade over the last pixels
    float fadeStart = 1.0 - rimPerPixel * fadePixels;
    float factor = 1.0 - smoothstep(fadeStart, 1.0, rim);

    return factor;
}

void main() {
    vec2 g_texCoord0 = v_texCoord0;

    vec4 cloud = fetchCloudColor(g_texCoord0, vec4(0.0, 0.0, 0.0, 0.0));
    vec3 ambient = v_ambientLight;

    // Eclipses
    #ifdef eclipsingBodyFlag
        vec3 lightDirection;
        if (any(notEqual(u_dirLights[0].color, vec3(0.0)))) {
            lightDirection = -u_dirLights[0].direction;
        } else {
            lightDirection = normalize(u_pointLights[0].position - v_fragPosWorld);
        }
        float eclshdw;
        vec3 diffractionTint;
        int outline;
        vec3 normalVector = v_normal;
        vec4 outlineColor = eclipseColor(v_fragPosWorld, lightDirection, normalVector, outline, diffractionTint, eclshdw);
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
        float NL = dot(N, L);
        float dayFactor = 1.0 - linstep(-0.25, 0.12, -NL);

        litColor += lightCol * dayFactor;
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
        float NL = dot(N, L);
        float dayFactor = 1.0 - linstep(-0.25, 0.12, -NL);

        litColor += lightCol * dayFactor;
    }
    #endif // pointLightsFlag

    float brightness = clamp(length(litColor + ambient), 0.03, 1.0);
    vec3 cloudColor = cloud.rgb * brightness;

    fragColor = vec4(cloudColor, 1.0) * v_opacity;

    // Eclipses
    #ifdef eclipsingBodyFlag
        fragColor.rgb = eclipseBlend(fragColor.rgb, diffractionTint, eclshdw);
    #endif // eclipsingBodyFlag

    // Cloud limb fade. Compute fade pixels according to vertex distance, with maximum fade of 25 px.
    // Extract the radius of the cloud shell from the world transformation. Usually wt[0,0] = wt[1,1] = wt[2,2], as
    // the same scale is used for x, y, and z. Generi1 has the radius in internal units.
    float toKm = 1.0 / u_kmToU;
    float radiusKm = u_generic1 * toKm;
    float distKm = v_fragDist * toKm;
    float fadePixels = (1.0 - smoothstep(radiusKm / 6.0, radiusKm * 1.5, distKm)) * 25.0;
    float cloudLimbFade = cloudLimbFade(normalize(v_viewDir), normalize(v_normal), fadePixels);
    fragColor *= cloudLimbFade;

    fragColor.a = 1.0;


    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
