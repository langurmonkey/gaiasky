#version 410 core

#define nop() {}

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef positionFlag
in vec3 a_position;
#endif //positionFlag

out vec4 v_position;
#define pushPositionValue(value) v_position = value
#if defined(positionFlag)
vec4 g_position = vec4(a_position, 1.0);
#define passPositionValue(value) pushPositionValue(value)
#else
vec4 g_position = vec4(0.0, 0.0, 0.0, 1.0);
#define passPositionValue(value) nop()
#endif
#define passPosition() passPositionValue(g_position)
#define pushPosition() pushPositionValue(g_position)

////////////////////////////////////////////////////////////////////////////////////
////////// COLOR ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef colorFlag
in vec4 a_color;
#endif //colorFlag

out vec4 v_color;
#define pushColor(value) v_color = value

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

out vec3 v_normal;
#define pushNormalValue(value) v_normal = (value)
#if defined(normalFlag)
vec3 g_normal = a_normal;
#define passNormalValue(value) pushNormalValue(value)
#else
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define passNormalValue(value) nop()
#endif
#define passNormal() passNormalValue(g_normal)
#define pushNormal() pushNormalValue(g_normal)

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef binormalFlag
in vec3 a_binormal;
#endif //binormalFlag

out vec3 v_binormal;
#define pushBinormalValue(value) v_binormal = (value)
#if defined(binormalFlag)
vec3 g_binormal = a_binormal;
#define passBinormalValue(value) pushBinormalValue(value)
#else
vec3 g_binormal = vec3(0.0, 1.0, 0.0);
#define passBinormalValue(value) nop()
#endif // binormalFlag
#define passBinormal() passBinormalValue(g_binormal)
#define pushBinormal() pushBinormalValue(g_binormal)

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef tangentFlag
in vec3 a_tangent;
#endif //tangentFlagvec3

out vec3 v_tangent;
#define pushTangentValue(value) v_tangent = (value)
#if defined(tangentFlag)
vec3 g_tangent = a_tangent;
#define passTangentValue(value) pushTangentValue(value)
#else
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#define passTangentValue(value) nop()
#endif // tangentFlag
#define passTangent() passTangentValue(g_tangent)
#define pushTangent() pushTangentValue(g_tangent)

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - VERTEX
///////////////////////////////////////////////////////////////////////////////////
#ifdef texCoord0Flag
#ifndef texCoordsFlag
#define texCoordsFlag
#endif
in vec2 a_texCoord0;
#endif

out vec2 v_texCoords;
#define pushTexCoord0(value) v_texCoords = value

#if defined(texCoord0Flag)
vec2 g_texCoord0 = a_texCoord0;
#else
vec2 g_texCoord0 = vec2(0.0, 0.0);
#endif // texCoord0Flag

#include <shader/lib/shadowmap.vert.glsl>

#ifdef shadowMapFlag
out vec3 v_shadowMapUv;
#ifdef shadowMapGlobalFlag
out vec3 v_shadowMapUvGlobal;
#endif // shadowMapGlobalFlag
#ifdef numCSM
out vec3 v_csmLightSpacePos[numCSM];
#endif // numCSM
#endif // shadowMapFlag

// Uniforms which are always available
uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat3 u_normalMatrix;

// Other uniforms
out float v_opacity;
#ifdef blendedFlag
uniform float u_opacity;
#else
const float u_opacity = 1.0;
#endif

out float v_alphaTest;
#ifdef alphaTestFlag
uniform float u_alphaTest;
#else
const float u_alphaTest = 0.0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef bumpTextureFlag
uniform sampler2D u_bumpTexture;
#endif


#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#if defined(heightTextureFlag)
uniform sampler2D u_heightTexture;
uniform float u_heightScale;
#define heightFlag
#endif //heightFlag

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



//////////////////////////////////////////////////////
////// AMBIENT LIGHT
//////////////////////////////////////////////////////
out vec3 v_ambientLight;

#ifdef ambientLightFlag
#ifndef ambientFlag
#define ambientFlag
#endif
uniform vec3 u_ambientLight;
#endif

#ifdef ambientCubemapFlag
uniform vec3 u_ambientCubemap[6];
#endif // ambientCubemapFlag

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
#endif // direcitonalLightsFlag

//////////////////////////////////////////////////////
////// POINTS LIGHTS
//////////////////////////////////////////////////////
#if defined(numPointLights) && (numPointLights > 0)
#define pointLightsFlag
#endif // numPointLights

#ifdef pointLightsFlag
struct PointLight
{
    vec3 color;
    vec3 position;
    float intensity;
};
uniform PointLight u_pointLights[numPointLights];
#endif // pointLightsFlag

out vec3 v_lightDir;
out vec3 v_lightCol;
out vec3 v_viewDir;

#ifdef reflectionCubemapFlag
out vec3 v_reflect;
#endif

void main() {
    v_opacity = u_opacity;
    v_alphaTest = u_alphaTest;

    //#ifdef heightFlag
    // Use height texture to move vertex along normal
    //float h = 1.0 - texture(u_heightTexture, a_texCoord0).r;
    //vec3 dh = normalize(a_normal) * h * (u_heightScale / 4.0);
    //g_position += vec4(dh, 0.0);
    //#endif //heightFlag

    // Location in world coordinates (world origin is at the camera)
    vec4 pos = u_worldTrans * g_position;

    //gl_Position = u_projViewTrans * pos;
    // We pass the position in world coordinates
    gl_Position = pos;

    #ifdef shadowMapFlag
    getShadowMapUv(pos, v_shadowMapUv);
    #ifdef shadowMapGlobalFlag
    getShadowMapUvGlobal(pos, v_shadowMapUvGlobal);
    #endif // shadowMapGlobalFlag
    #ifdef numCSM
    getCsmLightSpacePos(pos, v_csmLightSpacePos);
    #endif // numCSM
    #endif // shadowMapFlag


    // Tangent space transform
    calculateTangentVectors();
    g_normal = normalize(u_normalMatrix * g_normal);

    #ifdef ambientLightFlag
    v_ambientLight = u_ambientLight;
    #else
    v_ambientLight = vec3(0.0);
    #endif // ambientLightFlag

    #ifdef ambientCubemapFlag
    vec3 squaredNormal = g_normal * g_normal;
    vec3 isPositive = step(0.0, g_normal);
    v_ambientLight += squaredNormal.x * mix(u_ambientCubemap[0], u_ambientCubemap[1], isPositive.x) +
    squaredNormal.y * mix(u_ambientCubemap[2], u_ambientCubemap[3], isPositive.y) +
    squaredNormal.z * mix(u_ambientCubemap[4], u_ambientCubemap[5], isPositive.z);
    #endif // ambientCubemapFlag

    #if defined(directionalLightsFlag) && defined(pointLightsFlag)
    if (any(notEqual(u_dirLights[0].color, vec3(0.0)))) {
        v_lightDir = normalize(-u_dirLights[0].direction);
        v_lightCol = u_dirLights[0].color;
    } else {
        v_lightDir = normalize(u_pointLights[0].position - pos.xyz);
        v_lightCol = u_pointLights[0].color;
    }
    #else
    v_lightDir = vec3(0.0, 0.0, 0.0);
    v_lightCol = vec3(0.0);
    #endif // directionalLightsFlag

    // Camera is at origin, view direction is inverse of vertex position
    pushNormal();
    v_viewDir = -pos.xyz;

    #ifdef reflectionCubemapFlag
    v_reflect = reflect(-v_viewDir, g_normal);
    #endif // reflectionCubemapFlag

    pushColor(g_color);
    pushTexCoord0(g_texCoord0);
}
