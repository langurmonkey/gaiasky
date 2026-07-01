#version 410 core

layout (triangles) in;

#define tessellationEvaluationShader

// GEOMETRY (QUATERNIONS)
#if defined(relativisticEffects)
#include <shader/lib/geometry.glsl>
#endif

// NOISE
#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/perlin.glsl>

// SVT
#ifdef svtCacheTextureFlag
uniform sampler2D u_svtCacheTexture;
#endif

#ifdef cubemapFlag
    #include <shader/lib/cubemap.glsl>
#endif // cubemapFlag

#ifdef svtFlag
    #include <shader/lib/svt.glsl>
#endif // svtFlag

////////////////////////////////////////////////////////////////////////////////////////
////////// RELATIVISTIC EFFECTS
////////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif

////////////////////////////////////////////////////////////////////////////////////////
////////// GRAVITATIONAL WAVES
////////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif

uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

uniform float u_heightScale;
uniform float u_elevationMultiplier;
uniform float u_vrScale;

// Noise options for elevation (height)
uniform float u_elevationSeed;
uniform float u_elevationAmplitude;
uniform float u_elevationPersistence;
uniform float u_elevationFrequency;
uniform float u_elevationLacunarity;
uniform vec3  u_elevationScale;
uniform float u_elevationPower;
uniform int   u_elevationOctaves;
uniform bool  u_elevationTurbulence;
uniform bool  u_elevationRidge;
// Noise options for moisture
uniform float u_moistureSeed;
uniform float u_moistureAmplitude;
uniform float u_moisturePersistence;
uniform float u_moistureFrequency;
uniform float u_moistureLacunarity;
uniform vec3  u_moistureScale;
uniform float u_moisturePower;
uniform int   u_moistureOctaves;
uniform bool  u_moistureTurbulence;
uniform bool  u_moistureRidge;

// Water level: everything at or below this elevation is water
uniform float u_waterLevel;

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
    #ifdef reflectionCubemapFlag
    vec3 reflect;
    #endif // reflectionCubemapFlag
    mat3 tbn;
};

// INPUT — gl_Position from control shader is MODEL-space
in VertexData l_data[gl_MaxPatchVertices];
#ifdef atmosphereGround
uniform float fInnerRadius; /* The inner (planetary) radius*/
in vec3 l_position[gl_MaxPatchVertices];
#endif // atmosphereGround

// OUTPUT
out VertexData o_data;
#ifdef atmosphereGround
out vec3 o_position;
#endif // atmosphereGround
out vec3 o_normalTan;
out vec3 o_fragPosition;
// Raw elevation in [0,1] range (before scaling, after water clamping)
out float o_fragElevation;
// Displaced height in world units (elevation * heightScale * elevationMultiplier)
out float o_fragHeight;
out float o_fragMoisture;
out float o_fragTemperature;

// --- Noise evaluation functions ---

float evaluateElevation(vec3 point) {
    gln_tFBMOpts opts = gln_tFBMOpts(
            u_elevationSeed, u_elevationAmplitude, u_elevationPersistence,
            u_elevationFrequency, u_elevationLacunarity, u_elevationScale,
            u_elevationPower, u_elevationOctaves, u_elevationTurbulence,
            u_elevationRidge
    );
    float elevation = clamp(gln_pfbm(point, opts), 0.0, 1.0);
    // Clamp water level: everything at or below u_waterLevel becomes flat water
    if (elevation <= u_waterLevel) {
        elevation = u_waterLevel;
    }
    return elevation;
}

float evaluateMoisture(vec3 point) {
    gln_tFBMOpts opts = gln_tFBMOpts(
            u_moistureSeed, u_moistureAmplitude, u_moisturePersistence,
            u_moistureFrequency, u_moistureLacunarity, u_moistureScale,
            u_moisturePower, u_moistureOctaves, u_moistureTurbulence,
            u_moistureRidge
    );
    return gln_pfbm(point, opts);
}

void main(void) {
    float u = gl_TessCoord.x;
    float v = gl_TessCoord.y;
    float w = gl_TessCoord.z;

    // Interpolate MODEL-space position from gl_in[].gl_Position
    vec3 modelPos = u * gl_in[0].gl_Position.xyz + v * gl_in[1].gl_Position.xyz + w * gl_in[2].gl_Position.xyz;

    // Interpolate other attributes
    o_data.texCoords = u * l_data[0].texCoords + v * l_data[1].texCoords + w * l_data[2].texCoords;
    o_data.normal = normalize(modelPos);

    // Procedural height
    float elevation = evaluateElevation(modelPos);
    o_fragElevation = elevation;
    o_fragHeight = elevation * u_heightScale * u_elevationMultiplier;

    // Displace along normal in model-space
    vec3 dh = o_data.normal * o_fragHeight;
    vec4 pos = vec4(modelPos + dh, 1.0);

    // Moisture
    o_fragMoisture = evaluateMoisture(modelPos);

    // Apply world transform AFTER noise evaluation and displacement
    pos = u_worldTrans * pos;

    #ifdef relativisticEffects
        pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
        pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    gl_Position = u_projViewTrans * pos;

    // Plumbing
    o_fragPosition = pos.xyz;
    o_data.opacity = u * l_data[0].opacity + v * l_data[1].opacity + w * l_data[2].opacity;
    o_data.color = u * l_data[0].color + v * l_data[1].color + w * l_data[2].color;
    o_data.viewDir = u * l_data[0].viewDir + v * l_data[1].viewDir + w * l_data[2].viewDir;
    o_data.fragPosWorld = u * l_data[0].fragPosWorld + v * l_data[1].fragPosWorld + w * l_data[2].fragPosWorld;
    o_data.ambientLight = u * l_data[0].ambientLight + v * l_data[1].ambientLight + w * l_data[2].ambientLight;
    #ifdef reflectionCubemapFlag
        o_data.reflect = u * l_data[0].reflect + v * l_data[1].reflect + w * l_data[2].reflect;
    #endif // reflectionCubemapFlag

    #ifdef atmosphereGround
        o_position = (u * l_position[0] + v * l_position[1] + w * l_position[2]);
        // Add displacement
        o_position = o_position * (1.0 + o_fragHeight / fInnerRadius);
    #endif

    #ifdef shadowMapFlag
    o_data.shadowMapUv = u * l_data[0].shadowMapUv + v * l_data[1].shadowMapUv + w * l_data[2].shadowMapUv;
        #ifdef shadowMapGlobalFlag
    o_data.shadowMapUvGlobal = u * l_data[0].shadowMapUvGlobal + v * l_data[1].shadowMapUvGlobal + w * l_data[2].shadowMapUvGlobal;
        #endif
        #ifdef numCSM
    for (int i = 0; i < numCSM; i++) {
        o_data.csmLightSpacePos[i] = u * l_data[0].csmLightSpacePos[i] + v * l_data[1].csmLightSpacePos[i] + w * l_data[2].csmLightSpacePos[i];
    }
        #endif
    #endif

    o_data.tbn = u * l_data[0].tbn + v * l_data[1].tbn + w * l_data[2].tbn;
}
