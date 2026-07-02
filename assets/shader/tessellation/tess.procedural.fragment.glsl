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

// CUBEMAPS
#ifdef cubemapFlag
#include <shader/lib/cubemap.glsl>
#endif // cubemapFlag

// SVT
#ifdef svtFlag
#include <shader/lib/svt.glsl>
#endif // svtFlag

#if defined(numDirectionalLights) && (numDirectionalLights > 0)
#define directionalLightsFlag
#endif // numDirectionalLights

#ifdef directionalLightsFlag
struct DirectionalLight {
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
    #endif // shadowMapGlobalFlag
    #ifdef numCSM
    vec3 csmLightSpacePos[numCSM];
    #endif // numCSM
    #endif // shadowMapFlag
    vec3 fragPosWorld;
    #ifdef metallicFlag
    vec3 reflect;
    #endif // metallicFlag
    mat3 tbn;
};
in VertexData o_data;

#ifdef atmosphereGround
#include <shader/lib/atmscattering.frag.glsl>
in vec3 o_position;
#endif // atmosphereGround

in vec3 o_normalTan;
// o_fragHeight and o_fragPosition are declared by atmfog.glsl when atmosphereGround is defined
in float o_fragElevation;
in float o_fragMoisture;
in float o_fragTemperature;

// OUTPUT
layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#include <shader/lib/atmfog.glsl>
#include <shader/lib/logdepthbuff.glsl>
#include <shader/lib/pbr.glsl>

#ifdef ssrFlag
#include <shader/lib/pack.glsl>
#endif // ssrFlag

// MAIN
void main() {
    vec2 texCoords = o_data.texCoords;

    // Elevation and moisture are interpolated from the tess eval shader (evaluated once per final vertex)
    float elevation = o_fragElevation;
    float moisture = o_fragMoisture;

    // Sample LUT for diffuse color
    // LUT: X = moisture, Y = 1 - elevation (so water is at bottom)
    //vec4 diffuse = texture(u_biomeLUT, vec2(moisture, 1.0 - elevation));

    vec4 diffuse;
    if (elevation <= u_waterLevel + 0.001) {
        // Blue
        diffuse = vec4(0.1, 0.12, 0.6, 1.0);
    } else {
        // Green
        diffuse = vec4(0.02, 0.5, 0.11, 1.0);
    }

    // Specular: water is reflective
    vec3 specular = vec3(0.0);
    if (elevation <= u_waterLevel + 0.001) {
        specular = vec3(1.0); // Water is specular
    }

    // TODO generate emissive
    vec4 emissive = vec4(0.0, 0.0, 0.0, 0.0);
    vec3 ambient = o_data.ambientLight;

    #if defined(atmosphereGround) || defined(atmosphereObject)
    vec3 night = max(vec3(0.0, 0.0, 0.0), emissive.rgb - ambient.rgb);
    emissive = vec4(0.0);
    #else
    vec3 night = vec3(0.0);
    #endif // atmosphereGround

    // AO
    float ambientOcclusion = 1.0;

    // Alpha value
    float texAlpha = diffuse.a;
    if (texAlpha <= 0.0) {
        discard;
    }

    // Normal — interpolated from tess eval shader
    vec3 N = o_normalTan;
    vec3 normalVector = o_data.normal;
    vec3 V = o_data.viewDir;

    // Shadow mapping.
    #ifdef shadowMapFlag
    #ifdef numCSM
    // Cascaded shadow mapping.
    float shadowMap = clamp(getShadow(o_data.shadowMapUv, o_data.csmLightSpacePos, length(o_data.fragPosWorld)), 0.0, 1.0);
    #else
    // Regular shadow mapping.
    float transparency = 1.0 - texture(u_shadowTexture, o_data.shadowMapUv.xy).g;

    #ifdef shadowMapGlobalFlag
    float shadowMap = clamp(getShadow(o_data.shadowMapUv, o_data.shadowMapUvGlobal) + transparency, 0.0, 1.0);
    #else
    float shadowMap = clamp(getShadow(o_data.shadowMapUv) + transparency, 0.0, 1.0);
    #endif // shadowMapGlobalFlag
    #endif // numCSM
    #else
    float shadowMap = 1.0;
    #endif // shadowMapFlag

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
    vec4 outlineColor = eclipseColor(o_data.fragPosWorld, lightDirection, normalVector.xyz, outline, diffractionTint, eclshdw);
    #endif // eclipsingBodyFlag

    // Reflection (metallic and roughness values)
    vec3 reflectionColor = vec3(0.0);

    float roughnessValue = 0.5; // Default
    float metallicValue = 0.0; // Default

    vec3 F_env = vec3(0.0);
    #ifdef metallicFlag
    // Fetch Roughness
    #if defined(roughnessTextureFlag) || defined(roughnessCubemapFlag) || defined(svtIndirectionRoughnessTextureFlag) || defined(roughnessColorFlag) || defined(occlusionMetallicRoughnessTextureFlag)
    vec3 roughness3 = fetchColorRoughness(texCoords);
    roughnessValue = roughness3.r;
    #elif defined(shininessFlag)
    roughnessValue = clamp(sqrt(2.0 / (u_shininess + 2.0)), 0.0, 1.0);
    #endif // roughness

    // Fetch Metallic
    vec3 fetchedMetallic = fetchColorMetallic(texCoords);
    metallicValue = fetchedMetallic.r;

    // Handle Environment Reflections (Indirect Specular)
    #ifdef reflectionCubemapFlag
    // Sample cubemap with roughness-based LOD (13.0 is approx max mip level)
    float lod = roughnessValue * 13.0;
    reflectionColor = texture(u_reflectionCubemap,
            vec3(-reflectDir.x, reflectDir.y, reflectDir.z),
            lod).rgb;

    // PBR Fresnel for Environment:
    // Non-metals reflect ~4% (white), Metals reflect Albedo color.
    vec3 F0_env = mix(vec3(0.04), diffuse.rgb, metallicValue);

    // Use roughness-dependent Fresnel
    F_env = F0_env + (max(vec3(1.0 - roughnessValue), F0_env) - F0_env) * pow(clamp(1.0 - max(dot(N, V), 0.0), 0.0, 1.0), 5.0);

    // Apply F_env (do not use fresnelSchlick approximation)
    reflectionColor *= F_env;
    #endif // reflectionCubemapFlag

    // SSR (Screen Space Reflections) Support
    #ifdef ssrFlag
    vec3 rmc = diffuse.rgb * metallicValue;
    reflectionMask = vec4(rmc.r, pack2(rmc.gb), roughnessValue, 1.0);
    #endif // ssrFlag
    #endif // metallicFlag

    //#ifdef iorFlag
    //    vec3 f0 = vec3(pow(( u_ior - 1.0) /  (u_ior + 1.0), 2.0));
    //#else
    //    vec3 f0 = vec3(0.04); // from ior 1.5 value
    //#endif // iorFlag

    // PBR lighting
    vec3 shadowColor = vec3(0.0);
    vec3 diffuseColor = vec3(0.0);
    vec3 specularColor = vec3(0.0);
    float selfShadow = 1.0;
    vec3 fog = vec3(0.0);

    float NL0;
    vec3 L0;

    int validLights = 0;

    // DIRECTIONAL LIGHTS
    #ifdef directionalLightsFlag
    // Loop for directional light contributions.
    for (int i = 0; i < numDirectionalLights; i++) {
        vec3 col = u_dirLights[i].color;
        // Skip non-lights
        if (col.r == 0.0 && col.g == 0.0 && col.b == 0.0) {
            continue;
        } else {
            validLights++;
        }
        // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
        vec3 L = normalize(-u_dirLights[i].direction * o_data.tbn);
        processLight(col,
            V,
            N,
            L,
            validLights,
            specular,
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
    #endif // directionalLightsFlag

    // POINT LIGHTS
    #ifdef pointLightsFlag
    // Loop for point light contributions.
    for (int i = 0; i < numPointLights; i++) {
        vec3 col = u_pointLights[i].color * u_pointLights[i].intensity;
        // Skip non-lights
        if (all(equal(col, vec3(0.0)))) {
            continue;
        } else {
            validLights++;
        }
        // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
        vec3 L = normalize((u_pointLights[i].position - o_data.fragPosWorld) * o_data.tbn);
        processLight(col,
            V,
            N,
            L,
            validLights,
            specular,
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
    #endif // pointLightsFlag
    // Ambient light in shadow
    selfShadow = saturate(selfShadow + (ambient.r));

    // Diffuse texture contribution.
    if (validLights == 0) {
        // Only ambient contribution
        diffuseColor = ambient;
    }

    // Apply AO
    vec3 finalAmbient = (ambient * ambientOcclusion) * (vec3(1.0) - F_env);
    vec3 finalReflection = reflectionColor * ambientOcclusion;

    // Diffuse scattering
    #ifdef diffuseScatteringColorFlag
    vec3 baseScattering = diffuse.rgb * fetchColorDiffuseScattering() * ambientOcclusion;
    #else
    vec3 baseScattering = vec3(0.0);
    #endif // diffuseScatteringColorFlag

    // Clamp self-shadow
    selfShadow = pow(clamp(selfShadow, 0.0, 1.0), 1.0);

    // Calculate the Indirect (Ambient) part
    vec3 ambientTerm = diffuse.rgb * finalAmbient;

    // Calculate the Direct part (Accumulated from light loops)
    // Note: diffuseColor at this point is just (kD / PI) * LightColor * NdotL
    vec3 directDiffuseTerm = diffuse.rgb * diffuseColor;

    // Calculate scattering effect
    vec3 shadedScattering = baseScattering * shadowMap;

    // Final color computation
    vec3 surfaceDayLighting = (directDiffuseTerm * selfShadow) +
            (specularColor * selfShadow) +
            shadedScattering +
            ambientTerm +
            finalReflection;
    vec3 surfaceEmission = shadowColor + emissive.rgb;

    #ifdef atmosphereGround
    vec3 atmGlow = vec3(0.0);
    vec3 atmTransmittance = vec3(1.0);

    // Extract raw atmospheric properties along this ray
    computeAtmosphericScatteringGround(o_position, atmGlow, atmTransmittance);

    // Final composite math:
    // - Day lighting is heavily choked by atmospheric thickness (attenuated).
    // - Emission/Night lights scale inversely with daytime transmittance so they shine cleanly on the night side.
    // - Atmospheric scattering glow is added over everything.
    vec3 finalRGB = (surfaceDayLighting * atmTransmittance) +
            surfaceEmission +
            atmGlow;

    fragColor = vec4(finalRGB, texAlpha * o_data.opacity);

    #if defined(heightFlag)
    fragColor.rgb = applyFog(fragColor.rgb, V, L0 * -1.0, NL0);
    #endif // heightFlag
    #else
    // Fallback standard blending if atmosphere is toggled off
    fragColor = vec4(
            surfaceDayLighting + surfaceEmission,
            texAlpha * o_data.opacity
        );
    #endif // atmosphereGround

    #ifdef eclipsingBodyFlag
    #ifdef eclipseOutlines
    if (outline > 0) {
        fragColor = outlineColor;
    } else {
        fragColor.rgb = eclipseBlend(fragColor.rgb, diffractionTint, eclshdw);
        // Uncomment line below (and comment above) to make the eclipse turn on the lights (night texture).
        // fragColor.rgb = fragColor.rgb * eclshdw + night.rgb * 0.5 * (1.0 - eclshdw);
    }
    #else
    fragColor.rgb = eclipseBlend(fragColor.rgb, diffractionTint, eclshdw);
    // Uncomment line below (and comment above) to make the eclipse turn on the lights (night texture).
    // fragColor.rgb = fragColor.rgb * eclshdw + night.rgb * 0.5 * (1.0 - eclshdw);
    #endif // eclipseOutlines
    #endif // eclipsingBodyFlag

    if (fragColor.a <= 0.0) {
        discard;
    }

    #ifdef ssrFlag
    normalBuffer = vec4(normalVector.xyz, 1.0);
    #endif // ssrFlag

    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);
    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
}
