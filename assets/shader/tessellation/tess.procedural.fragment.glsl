#version 410 core

// Uniforms which are always available
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

// LUT for diffuse color lookup (moisture X, elevation Y)
uniform sampler2D u_biomeLUT;

// Water level
uniform float u_waterLevel;

// Emissive
#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif
#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
#include <shader/lib/luma.glsl>
#endif

// ECLIPSES
#include <shader/lib/eclipses.glsl>
// SHADOW MAPPING
#include <shader/lib/shadowmap.frag.glsl>

#ifdef svtCacheTextureFlag
uniform sampler2D u_svtCacheTexture;
#endif

#ifdef svtFlag
#include <shader/lib/svt.glsl>
#endif

#ifdef cubemapFlag
#include <shader/lib/cubemap.glsl>
#endif

// COLOR EMISSIVE
#if defined(emissiveTextureFlag)
    #define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord)
#elif defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(tex, texCoord) u_emissiveColor
#endif

#if defined(svtIndirectionEmissiveTextureFlag)
    #define fetchColorEmissive(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionEmissiveTexture, texCoord))
#elif defined(emissiveCubemapFlag)
    #define fetchColorEmissive(texCoord) texture(u_emissiveCubemap, UVtoXYZ(texCoord))
#elif defined(emissiveTextureFlag) || defined(emissiveColorFlag)
    #define fetchColorEmissive(texCoord) fetchColorEmissiveTD(u_emissiveTexture, texCoord)
#else
    #define fetchColorEmissive(texCoord) vec4(0.0, 0.0, 0.0, 0.0)
#endif

#if defined(numDirectionalLights) && (numDirectionalLights > 0)
    #define directionalLightsFlag
#endif

#ifdef directionalLightsFlag
struct DirectionalLight {
    vec3 color;
    vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
#endif

#if defined(numPointLights) && (numPointLights > 0)
#define pointLightsFlag
#endif

#ifdef pointLightsFlag
struct PointLight {
    vec3 color;
    vec3 position;
    float intensity;
};
uniform PointLight u_pointLights[numPointLights];
#endif

// INPUT
struct VertexData {
    vec2 texCoords;
    vec3 normal;
    vec3 viewDir;
    vec3 ambientLight;
    float opacity;
    vec4 color;
    #ifdef shadowMapFlag
    vec3 shadowMapUv;
    #ifdef shadowMapGlobalFlag
    vec3 shadowMapUvGlobal;
    #endif
    #ifdef numCSM
    vec3 csmLightSpacePos[numCSM];
    #endif
    #endif
    vec3 fragPosWorld;
    #ifdef metallicFlag
    vec3 reflect;
    #endif
    mat3 tbn;
};
in VertexData o_data;

#ifdef atmosphereGround
#include <shader/lib/atmscattering.frag.glsl>
in vec3 o_position;
#endif

in vec3 o_normalTan;
in float o_fragHeight;
in float o_fragElevation;
in float o_fragMoisture;
in float o_fragTemperature;

// OUTPUT
layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
    #include <shader/lib/ssr.frag.glsl>
#endif

#include <shader/lib/atmfog.glsl>
#include <shader/lib/logdepthbuff.glsl>
#include <shader/lib/pbr.glsl>

#ifdef ssrFlag
    #include <shader/lib/pack.glsl>
#endif

// MAIN
void main() {
    vec2 texCoords = o_data.texCoords;

    // Elevation and moisture are interpolated from the tess eval shader (evaluated once per final vertex)
    float elevation = o_fragElevation;
    float moisture = o_fragMoisture;

    // Sample LUT for diffuse color
    // LUT: X = moisture, Y = 1 - elevation (so water is at bottom)
    vec4 diffuse = texture(u_biomeLUT, vec2(moisture, 1.0 - elevation));

    // Specular: water is reflective
    float specular = 0.0;
    if (elevation <= u_waterLevel + 0.01) {
        specular = 1.0; // Water is specular
    }

    vec4 emissive = fetchColorEmissive(texCoords);
    vec3 ambient = o_data.ambientLight;

    #if defined(atmosphereGround) || defined(atmosphereObject)
        vec3 night = max(vec3(0.0, 0.0, 0.0), emissive.rgb - ambient.rgb);
        emissive = vec4(0.0);
    #else
        vec3 night = vec3(0.0);
    #endif

    float ambientOcclusion = 1.0;

    // Alpha value
    float texAlpha = diffuse.a;
    if (texAlpha <= 0.0) {
        discard;
    }

    // Normal — interpolated from tess eval shader
    vec3 N = o_normalTan;
    vec3 normalVector = N;
    vec3 V = o_data.viewDir;

    // Shadow mapping
    #ifdef shadowMapFlag
        #ifdef numCSM
            float shadowMap = clamp(getShadow(o_data.shadowMapUv, o_data.csmLightSpacePos, length(o_data.fragPosWorld)), 0.0, 1.0);
        #else
            float transparency = 1.0 - texture(u_shadowTexture, o_data.shadowMapUv.xy).g;
            #ifdef shadowMapGlobalFlag
                float shadowMap = clamp(getShadow(o_data.shadowMapUv, o_data.shadowMapUvGlobal) + transparency, 0.0, 1.0);
            #else
                float shadowMap = clamp(getShadow(o_data.shadowMapUv) + transparency, 0.0, 1.0);
            #endif
        #endif
    #else
        float shadowMap = 1.0;
    #endif

    // Eclipses
    #ifdef eclipsingBodyFlag
        vec3 lightDirection;
        if (any(notEqual(u_dirLights[0].color, vec3(0.0)))) {
            lightDirection = -u_dirLights[0].direction;
        } else {
            lightDirection = normalize(u_pointLights[0].position - o_data.fragPosWorld);
        }
        float eclshdw;
        vec3 diffractionTint;
        int outline;
        vec4 outlineColor = eclipseColor(o_data.fragPosWorld, lightDirection, normalVector, outline, diffractionTint, eclshdw);
    #endif

    // PBR lighting
    vec3 shadowColor = vec3(0.0);
    vec3 diffuseColor = vec3(0.0);
    vec3 specularColor = vec3(0.0);
    float selfShadow = 1.0;
    vec3 fog = vec3(0.0);

    float NL0;
    vec3 L0;

    int validLights = 0;

    float roughnessValue = 0.5;
    float metallicValue = 0.0;

    // Water is smooth and metallic-ish
    if (specular > 0.5) {
        roughnessValue = 0.05;
        metallicValue = 0.1;
    }

    // DIRECTIONAL LIGHTS
    #ifdef directionalLightsFlag
        for (int i = 0; i < numDirectionalLights; i++) {
            vec3 col = u_dirLights[i].color;
            if (col.r == 0.0 && col.g == 0.0 && col.b == 0.0) {
                continue;
            } else {
                validLights++;
            }
            vec3 L = normalize(-u_dirLights[i].direction * o_data.tbn);
            processLight(col,
                    V,
                    N,
                    L,
                    validLights,
                    vec3(specular),
                    night,
                    diffuse.rgb,
                    metallicValue,
                    roughnessValue,
                    shadowMap,
                    texCoords,
                    NL0,
                    L0,
                    selfShadow,
                    specularColor,
                    shadowColor,
                    diffuseColor);
        }
    #endif

    // POINT LIGHTS
    #ifdef pointLightsFlag
        for (int i = 0; i < numPointLights; i++) {
            vec3 col = u_pointLights[i].color * u_pointLights[i].intensity;
            if (all(equal(col, vec3(0.0)))) {
                continue;
            } else {
                validLights++;
            }
            vec3 L = normalize((u_pointLights[i].position - o_data.fragPosWorld) * o_data.tbn);
            processLight(col,
                    V,
                    N,
                    L,
                    validLights,
                    vec3(specular),
                    night,
                    diffuse.rgb,
                    metallicValue,
                    roughnessValue,
                    shadowMap,
                    texCoords,
                    NL0,
                    L0,
                    selfShadow,
                    specularColor,
                    shadowColor,
                    diffuseColor);
        }
    #endif

    selfShadow = saturate(selfShadow + (ambient.r));

    if (validLights == 0) {
        diffuseColor = ambient;
    }

    vec3 finalAmbient = ambient;
    vec3 finalReflection = vec3(0.0);

    selfShadow = pow(clamp(selfShadow, 0.0, 1.0), 1.0);

    vec3 ambientTerm = diffuse.rgb * finalAmbient;
    vec3 directDiffuseTerm = diffuse.rgb * diffuseColor;

    fragColor = vec4(
            (directDiffuseTerm * selfShadow) +
            (specularColor * selfShadow) +
            ambientTerm +
            finalReflection +
            shadowColor +
            emissive.rgb,
            texAlpha * o_data.opacity
    );

    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    #ifdef atmosphereGround
        vec3 atmosphereColor = computeAtmosphericScatteringGround(o_position);
        fragColor.rgb = clamp(fragColor.rgb + atmosphereColor, 0.0, 1.0);
    #endif

    #ifdef eclipsingBodyFlag
        #ifdef eclipseOutlines
            if (outline > 0) {
                fragColor = outlineColor;
            } else {
                fragColor.rgb = eclipseBlend(fragColor.rgb, diffractionTint, eclshdw);
            }
        #else
            fragColor.rgb = eclipseBlend(fragColor.rgb, diffractionTint, eclshdw);
        #endif
    #endif

    if (fragColor.a <= 0.0) {
        discard;
    }

    #ifdef ssrFlag
        normalBuffer = vec4(normalVector.xyz, 1.0);
    #endif

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
}
