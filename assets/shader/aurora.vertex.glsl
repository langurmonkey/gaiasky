#version 330 core

#define nop() {}

// Scene time and simulation time.
uniform float u_time;
uniform float u_simuTime;
uniform vec3 u_cameraPos;

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef positionFlag
in vec3 a_position;
#endif //positionFlag

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
#endif //colorFlag

#define pushColor(value) v_data.color = value

#if defined(colorFlag)
vec4 g_color = a_color;
#else
vec4 g_color = vec4(1.0, 1.0, 1.0, 1.0);
#endif // colorFlag

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef normalFlag
in vec3 a_normal;
#endif //normalFlag

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
#endif //binormalFlag

#if defined(binormalFlag)
vec3 g_binormal = a_binormal;
#else
vec3 g_binormal = vec3(0.0, 1.0, 0.0);
#endif // binormalFlag

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef tangentFlag
in vec3 a_tangent;
#endif //tangentFlagvec3

#if defined(tangentFlag)
vec3 g_tangent = a_tangent;
#else
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#endif // tangentFlag

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
#endif // texCoord0Flag

// Uniforms which are always available
uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;
uniform float u_vrScale;
// Use this slot for VROffset
uniform vec3 u_vrOffset = vec3(0.0);

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif // relativisticEffects

////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

#ifdef blendedFlag
uniform float u_opacity;
#else
const float u_opacity = 1.0;
#endif

#ifdef svtCacheTextureFlag
uniform sampler2D u_svtCacheTexture;
#endif

// NORMAL
#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef normalCubemapFlag
uniform samplerCube u_normalCubemap;
#endif

#ifdef svtIndirectionNormalTextureFlag
uniform sampler2D u_svtIndirectionNormalTexture;
#endif

// COLOR NORMAL
#if defined(svtIndirectionNormalTextureFlag)
#define fetchColorNormal(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionNormalTexture, texCoord))
#elif defined(normalCubemapFlag)
#define fetchColorNormal(texCoord) texture(u_normalCubemap, UVtoXYZ(texCoord))
#elif defined(normalTextureFlag)
#define fetchColorNormal(texCoord) texture(u_normalTexture, texCoord)
#endif// normal

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
uniform vec3 u_ambientLight;
#endif // ambientLightFlag

#ifdef ambientCubemapFlag
uniform vec3 u_ambientCubemap[6];
#endif // ambientCubemapFlag

// OUTPUT
struct VertexData {
    vec2 texCoords;
    vec3 normal;
    vec3 viewDir;
    vec3 ambientLight;
    float opacity;
    vec4 color;
    vec3 fragPosWorld;
    mat3 tbn;
};
out VertexData v_data;
out vec3 v_normalTan;

vec3 calcNormal(vec2 p, vec2 dp) {
    return vec3(0.0, 0.0, 1.0);
}

#include <shader/lib/geometry.glsl>

float rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}
float noise(vec2 n) {
    const vec2 d = vec2(0.0, 1.0);
    vec2 b = floor(n), f = smoothstep(vec2(0.0), vec2(1.0), fract(n));
    return mix(mix(rand(b), rand(b + d.yx), f.x), mix(rand(b + d.xy), rand(b + d.yy), f.x), f.y);
}

void main() {
    // Tangent space transform
    calculateTangentVectors();
    g_normal = normalize(u_normalMatrix * g_normal);
    g_binormal = normalize(u_normalMatrix * g_binormal);
    g_tangent = normalize(u_normalMatrix * g_tangent);

    mat3 TBN = mat3(g_tangent, g_binormal, g_normal);
    v_data.tbn = TBN;

    #if !defined(normalTextureFlag) && !defined(normalCubemapFlag) && !defined(svtIndirectionNormalTextureFlag)
    v_normalTan = calcNormal(g_texCoord0, vec2(0.0));
    #endif // !normalTextureFlag && !normalCubemapFlag && !normalSVT

    v_data.opacity = u_opacity;

    // Wiggle the vertices in local space.
    float t = u_simuTime / 25000.0 + 0.5;
    vec3 p = g_position.xyz;
    p.xz += vec2(0.04 * noise(p.zx * 20.0 * t), 0.04 * noise(p.xz * 20.0 * t));
    vec4 g_pos = vec4(p.xyz, 1.0);

    // Location in world coordinates (world origin is at the camera)
    vec4 pos = u_worldTrans * g_pos;

    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWave

    #if defined(heightFlag) && !defined(parallaxMappingFlag)
    o_fragPosition = pos.xyz;
    #endif // heightFlag

    v_data.fragPosWorld = pos.xyz;

    vec4 gpos = u_projViewTrans * pos;
    gl_Position = gpos;

    #ifdef ambientLightFlag
    v_data.ambientLight = u_ambientLight;
    #else
    v_data.ambientLight = vec3(0.0);
    #endif // ambientLightFlag

    #ifdef ambientCubemapFlag
    vec3 squaredNormal = g_normal * g_normal;
    vec3 isPositive = step(0.0, g_normal);
    v_data.ambientLight += squaredNormal.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], isPositive.x) +
            squaredNormal.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], isPositive.y) +
            squaredNormal.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], isPositive.z);
    #endif // ambientCubemapFlag

    v_data.viewDir = normalize(normalize(pos.xyz - u_vrOffset) * TBN);

    pushNormal();
    pushColor(g_color);
    pushTexCoord0(g_texCoord0);
}
