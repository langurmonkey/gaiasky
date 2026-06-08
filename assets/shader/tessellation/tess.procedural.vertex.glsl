#version 330 core

#define nop() {}

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef positionFlag
    in vec3 a_position;
#endif

#if defined(positionFlag)
    vec4 g_position = vec4(a_position, 1.0);
#else
    vec4 g_position = vec4(0.0, 0.0, 0.0, 1.0);
#endif

////////////////////////////////////////////////////////////////////////////////////
////////// COLOR ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef colorFlag
    in vec4 a_color;
#endif

#define pushColor(value) v_data.color = value

#if defined(colorFlag)
    vec4 g_color = a_color;
#else
    vec4 g_color = vec4(1.0, 1.0, 1.0, 1.0);
#endif

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef normalFlag
    in vec3 a_normal;
#endif

#define pushNormalValue(value) v_data.normal = (value)
#if defined(normalFlag)
vec3 g_normal = a_normal;
#else
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#endif
#define pushNormal() pushNormalValue(g_normal)

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef binormalFlag
    in vec3 a_binormal;
#endif

#if defined(binormalFlag)
    vec3 g_binormal = a_binormal;
#else
    vec3 g_binormal = vec3(0.0, 1.0, 0.0);
#endif

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef tangentFlag
    in vec3 a_tangent;
#endif

#if defined(tangentFlag)
    vec3 g_tangent = a_tangent;
#else
    vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#endif

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef texCoord0Flag
    #ifndef texCoordsFlag
    #define texCoordsFlag
    #endif
    in vec2 a_texCoord0;
#endif

#define pushTexCoord0(value) v_data.texCoords = value

#if defined(texCoord0Flag)
    vec2 g_texCoord0 = a_texCoord0;
#else
    vec2 g_texCoord0 = vec2(0.0, 0.0);
#endif

// Uniforms which are always available
uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;
uniform float u_vrScale;
// Use this slot for VROffset
uniform vec3 u_vrOffset = vec3(0.0);

#include <shader/lib/shadowmap.vert.glsl>
#include <shader/lib/atmscattering.vert.glsl>

// GEOMETRY (QUATERNIONS)
#if defined(relativisticEffects)
    #include <shader/lib/geometry.glsl>
#endif

#ifdef blendedFlag
    uniform float u_opacity;
#else
    const float u_opacity = 1.0;
#endif

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
    #define textureFlag
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
    #define specularFlag
#endif

#if defined(specularFlag) || defined(fogFlag)
    #define cameraPositionFlag
#endif

#if defined(normalFlag) && defined(binormalFlag) && defined(tangentFlag)
    #define calculateTangentVectors() nop()
#elif defined(normalFlag) && defined(binormalFlag)
    #define calculateTangentVectors() g_tangent = cross(g_normal, g_binormal)
#elif defined(normalFlag) && defined(tangentFlag)
    #define calculateTangentVectors() g_binormal = cross(g_normal, g_tangent)
#elif defined(binormalFlag) && defined(tangentFlag)
    #define calculateTangentVectors() g_normal = cross(g_binormal, g_tangent)
#elif defined(normalFlag) || defined(binormalFlag) || defined(tangentFlag)
    #if defined(normalFlag)
        void calculateTangentVectors() {
            g_binormal = vec3(0, g_normal.z, -g_normal.y);
            g_tangent = normalize(cross(g_normal, g_binormal));
        }
    #elif defined(binormalFlag)
        void calculateTangentVectors() {
            g_tangent = vec3(-g_binormal.z, 0, g_binormal.x);
            g_normal = normalize(cross(g_binormal, g_tangent));
        }
    #elif defined(tangentFlag)
        void calculateTangentVectors() {
            g_binormal = vec3(-g_tangent.z, 0, g_tangent.x);
            g_normal = normalize(cross(g_tangent, g_binormal));
        }
    #endif
#else
    #define calculateTangentVectors() nop()
#endif

#ifdef ambientLightFlag
    #ifndef ambientFlag
    #define ambientFlag
    #endif
    uniform vec3 u_ambientLight;
#endif

#ifdef ambientCubemapFlag
    uniform vec3 u_ambientCubemap[6];
#endif

// OUTPUT
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
    #ifdef reflectionCubemapFlag
    vec3 reflect;
    #endif
    mat3 tbn;
};
out VertexData v_data;

void main() {
    prepareAtmosphericScattering();

    v_data.opacity = u_opacity;

    // Output model-space position (no world transform).
    // The tessellation eval shader will apply noise displacement
    // and then the world transform.
    gl_Position = g_position;

    // For shadow map UVs and other world-space calculations,
    // we still compute world-space position for the vertex data
    vec4 worldPos = u_worldTrans * g_position;
    v_data.fragPosWorld = worldPos.xyz;

    #ifdef shadowMapFlag
    getShadowMapUv(worldPos, v_data.shadowMapUv);
    #ifdef shadowMapGlobalFlag
    getShadowMapUvGlobal(worldPos, v_data.shadowMapUvGlobal);
    #endif
    #ifdef numCSM
    getCsmLightSpacePos(worldPos, v_data.csmLightSpacePos);
    #endif
    #endif

    // Tangent space transform
    calculateTangentVectors();
    g_normal = normalize(u_normalMatrix * g_normal);
    g_binormal = normalize(u_normalMatrix * g_binormal);
    g_tangent = normalize(u_normalMatrix * g_tangent);

    mat3 TBN = mat3(g_tangent, g_binormal, g_normal);
    v_data.tbn = TBN;

    #ifdef ambientLightFlag
    v_data.ambientLight = u_ambientLight;
    #else
    v_data.ambientLight = vec3(0.0);
    #endif

    #ifdef ambientCubemapFlag
    vec3 squaredNormal = g_normal * g_normal;
    vec3 isPositive = step(0.0, g_normal);
    v_data.ambientLight += squaredNormal.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], isPositive.x) +
    squaredNormal.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], isPositive.y) +
    squaredNormal.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], isPositive.z);
    #endif

    // Camera is at origin in world space, view direction uses world position
    v_data.viewDir = normalize(normalize(worldPos.xyz - u_vrOffset) * TBN);

    #ifdef reflectionCubemapFlag
    #ifndef normalTextureFlag
    v_data.reflect = reflect(worldPos.xyz - u_vrOffset, g_normal);
    #endif
    #endif

    pushNormal();
    pushColor(g_color);
    pushTexCoord0(g_texCoord0);
}
