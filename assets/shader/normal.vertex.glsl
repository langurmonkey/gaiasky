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

out vec3 v_tangent;
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

out vec2 v_texCoord0;
#define pushTexCoord0(value) v_texCoord0 = value

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

//////////////////////////////////////////////////////
////// SHADOW MAPPING
//////////////////////////////////////////////////////
#ifdef shadowMapFlag
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
uniform mat4 u_shadowMapProjViewTrans;

out vec3 v_shadowMapUv;
#endif //shadowMapFlag

#include shader/lib_atmscattering.glsl

// GEOMETRY (QUATERNIONS)
#if defined(velocityBufferFlag) || defined(relativisticEffects)
#include shader/lib_geometry.glsl
#endif

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
    #include shader/lib_relativity.glsl
#endif // relativisticEffects


////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
    #include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

#ifdef blendedFlag
    uniform float u_opacity;
#else
    const float u_opacity = 1.0;
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
    #define KM_TO_U 1.0E-6
    #define HIEIGHT_FACTOR 0.001 * KM_TO_U
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
////// POINTS LIGHTS
//////////////////////////////////////////////////////
#ifdef lightingFlag
    #if defined(numPointLights) && (numPointLights > 0)
	#define pointLightsFlag
    #endif // numPointLights
#endif //lightingFlag

#ifdef pointLightsFlag
struct PointLight {
	vec3 color;
	vec3 position;
	float intensity;
};
uniform PointLight u_pointLights[numPointLights];
#endif

//////////////////////////////////////////////////////
////// DIRECTIONAL LIGHTS
//////////////////////////////////////////////////////
#if defined(numDirectionalLights) && (numDirectionalLights > 0)
#define directionalLightsFlag
#endif // numDirectionalLights

#ifdef directionalLightsFlag
struct DirectionalLight {
    vec3 color;
    vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
out DirectionalLight v_directionalLights[numDirectionalLights];
#endif // directionalLightsFlag

out vec3 v_viewDir;
out vec3 v_fragPosWorld;
out float v_opacity;

#ifdef environmentCubemapFlag
out vec3 v_reflect;
#endif

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
    computeAtmosphericScatteringGround();

    v_opacity = u_opacity;

    // Location in world coordinates (world origin is at the camera)
    vec4 pos = u_worldTrans * g_position;

    #ifdef relativisticEffects
        pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
        pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    v_fragPosWorld = pos.xyz;
    vec4 gpos = u_projViewTrans * pos;
    gl_Position = gpos;

    #ifdef velocityBufferFlag
    velocityBufferCam(gpos, pos);
    #endif

    #ifdef shadowMapFlag
	vec4 spos = u_shadowMapProjViewTrans * pos;
	v_shadowMapUv.xyz = (spos.xyz / spos.w) * 0.5 + 0.5;
    #endif //shadowMapFlag


    // Tangent space transform
    calculateTangentVectors();
    g_normal = normalize(u_normalMatrix * g_normal);
    g_binormal = normalize(u_normalMatrix * g_binormal);
    g_tangent = normalize(u_normalMatrix * g_tangent);

    #ifndef heightFlag
    mat3 TBN = mat3(g_tangent, g_binormal, g_normal);
    #endif

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

    #ifdef directionalLightsFlag
        for (int i = 0; i < numDirectionalLights; i++) {
            #ifdef heightFlag
            v_directionalLights[i].direction = normalize(-u_dirLights[i].direction);
            #else
            v_directionalLights[i].direction = normalize(-u_dirLights[i].direction * TBN);
            #endif
            v_directionalLights[i].color = u_dirLights[i].color;
        }
    #endif // directionalLightsFlag

    // Camera is at origin, view direction is inverse of vertex position
    pushNormal();
    #ifdef heightFlag
    v_viewDir = normalize(-pos.xyz);
    #else
    v_viewDir = normalize(-pos.xyz * TBN);
    #endif

    #ifdef environmentCubemapFlag
    #ifndef normalTextureFlag
        // Only if normal map not present, otherwise we perturb the normal in the fragment shader
    	v_reflect = reflect(-pos.xyz, g_normal);
    #endif // normalTextureFlag
    #endif // environmentCubemapFlag

    pushColor(g_color);
    pushTexCoord0(g_texCoord0);
}