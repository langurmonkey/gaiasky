#version 330 core

#define nop() {}

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

//////////////////////////////////////////////////////
////// SHADOW MAPPING
//////////////////////////////////////////////////////
#ifdef shadowMapFlag
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
uniform mat4 u_shadowMapProjViewTrans;
#endif //shadowMapFlag

#include <shader/lib/atmscattering.glsl>

// GEOMETRY (QUATERNIONS)
#if defined(velocityBufferFlag) || defined(relativisticEffects)
    #include <shader/lib/geometry.glsl>
#endif

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

// HEIGHT
#ifdef heightTextureFlag
uniform sampler2D u_heightTexture;
#endif // heightTextureFlag

#ifdef heightCubemapFlag
uniform samplerCube u_heightCubemap;
#endif // heightCubemapFlag

#ifdef svtIndirectionHeightTextureFlag
uniform sampler2D u_svtIndirectionHeightTexture;
// We import this here because we only use the SVT for the height channel.
uniform sampler2D u_svtCacheTexture;
#endif // svtIndirectionHeightTextureFlag

#if defined(heightTextureFlag) || defined(heightCubemapFlag) || defined(svtIndirectionHeightTextureFlag)
    #define heightFlag
#endif

#ifdef heightFlag
    uniform float u_heightScale;
    uniform float u_elevationMultiplier;
    uniform vec2 u_heightSize;
    #define KM_TO_U 1.0E-6
    #define HIEIGHT_FACTOR 0.001 * KM_TO_U
#endif // heightFlag

// HEIGHT
#if defined(noHeightFlag)
    #define fetchHeight(texCoord) vec4(0.0)
#elif defined(svtIndirectionHeightTextureFlag)
    #define fetchHeight(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionHeightTexture, texCoord))
#elif defined(heightCubemapFlag)
    #define fetchHeight(texCoord) texture(u_heightCubemap, UVtoXYZ(texCoord))
#elif defined(heightTextureFlag)
    #define fetchHeight(texCoord) texture(u_heightTexture, texCoord)
#else
    #define fetchHeight(texCoord) vec4(0.0)
#endif // height


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
    #ifdef shadowMapFlag
    vec3 shadowMapUv;
    #endif // shadowMapFlag
    vec3 fragPosWorld;
    #ifdef metallicFlag
    vec3 reflect;
    #endif // metallicFlag
    mat3 tbn;
};
out VertexData v_data;

#ifdef heightFlag
out float v_fragHeight;
#endif // heightFlag

#ifdef velocityBufferFlag
#include <shader/lib/velbuffer.vert.glsl>
#endif

void main() {
    computeAtmosphericScatteringGround();

    // Tangent space transform
    calculateTangentVectors();
    g_normal = normalize(u_normalMatrix * g_normal);
    g_binormal = normalize(u_normalMatrix * g_binormal);
    g_tangent = normalize(u_normalMatrix * g_tangent);

    v_data.opacity = u_opacity;

    // Location in world coordinates (world origin is at the camera)
    vec4 pos = u_worldTrans * g_position;

    #if defined(heightFlag) && !defined(parallaxMappingFlag)
    // Use height texture to move vertex along normal.
    float h = fetchHeight(g_texCoord0).r;
    v_fragHeight = h * u_heightScale * u_elevationMultiplier;
    vec3 dh = g_normal * v_fragHeight;
    pos += vec4(dh, 0.0);
    #endif // heightFlag && !parallaxMappingFlag

    #ifdef relativisticEffects
        pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
        pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWave

    v_data.fragPosWorld = pos.xyz;
    vec4 gpos = u_projViewTrans * pos;
    gl_Position = gpos;

    #ifdef velocityBufferFlag
    velocityBufferCam(gpos, pos);
    #endif // velocityBufferFlag

    #ifdef shadowMapFlag
	vec4 spos = u_shadowMapProjViewTrans * pos;
	v_data.shadowMapUv.xyz = (spos.xyz / spos.w) * 0.5 + 0.5;
    #endif // shadowMapFlag


    mat3 TBN = mat3(g_tangent, g_binormal, g_normal);
    v_data.tbn = TBN;

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

    // Camera is at origin, view direction is inverse of vertex position
    #ifdef heightFlag
    v_data.viewDir = normalize(pos.xyz - u_vrOffset);
    #else
    v_data.viewDir = normalize(normalize(pos.xyz - u_vrOffset) * TBN);
    #endif // heightFlag

    #ifdef metallicFlag
    #ifndef normalTextureFlag
        // Only if normal map not present, otherwise we perturb the normal in the fragment shader
    	v_data.reflect = reflect(normalize(pos.xyz - u_vrOffset), g_normal);
    #endif // normalTextureFlag
    #endif // metallicFlag

    pushNormalValue(g_normal);
    pushColor(g_color);
    pushTexCoord0(g_texCoord0);
}