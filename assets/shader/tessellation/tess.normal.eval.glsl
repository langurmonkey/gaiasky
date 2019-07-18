#version 410 core

layout (triangles) in;

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
uniform float u_vc;// v/c
uniform vec3 u_velDir;// Camera velocity direction

#include shader/lib_geometry.glsl
#include shader/lib_relativity.glsl
#endif// relativisticEffects


////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
uniform vec4 u_hterms;// hpluscos, hplussin, htimescos, htimessin
uniform vec3 u_gw;// Location of gravitational wave, cartesian
uniform mat3 u_gwmat3;// Rotation matrix so that u_gw = u_gw_mat * (0 0 1)^T
uniform float u_ts;// Time in seconds since start
uniform float u_omgw;// Wave frequency
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves


uniform mat4 u_worldTrans;
uniform mat4 u_projViewTrans;

uniform float u_heightScale;
uniform float u_heightNoiseSize;
uniform vec2 u_heightSize;
uniform sampler2D u_heightTexture;

#include shader/lib_logdepthbuff.glsl
out float o_depth;
out vec3 o_normalTan;
out vec3 o_fragPosition;
out float o_fragHeight;

in float l_opacity[gl_MaxPatchVertices];
out float o_opacity;

in vec2 l_texCoords[gl_MaxPatchVertices];
out vec2 o_texCoords;

in vec3 l_normal[gl_MaxPatchVertices];
out vec3 o_normal;

in vec3 l_viewDir[gl_MaxPatchVertices];
out vec3 o_viewDir;

in vec3 l_lightCol[gl_MaxPatchVertices];
out vec3 o_lightCol;

in vec3 l_lightDir[gl_MaxPatchVertices];
out vec3 o_lightDir;

in vec3 l_ambientLight[gl_MaxPatchVertices];
out vec3 o_ambientLight;

in vec4 l_color[gl_MaxPatchVertices];
out vec4 o_color;

#ifdef atmosphereGround
in vec4 l_atmosphereColor[gl_MaxPatchVertices];
out vec4 o_atmosphereColor;

in float l_fadeFactor[gl_MaxPatchVertices];
out float o_fadeFactor;
#endif

#ifdef shadowMapFlag
in vec3 l_shadowMapUv[gl_MaxPatchVertices];
out vec3 o_shadowMapUv;
#endif

#include shader/tessellation/lib_sampleheight.glsl

    #ifdef normalTextureFlag
// Use normal map
uniform sampler2D u_normalTexture;
vec3 calcNormal(vec2 p, vec2 dp){
    return normalize(texture(u_normalTexture, p).rgb * 2.0 - 1.0);
}
    #else
// Use height texture for normals
vec3 calcNormal(vec2 p, vec2 dp){
    vec4 h;
    const vec2 size = vec2(1.0, 0.0);
    if (dp.x < 0.0){
        dp = vec2(.5e-3);
    }
    h.x = sampleHeight(u_heightTexture, vec2(p.x - dp.x, p.y)).r;
    h.y = sampleHeight(u_heightTexture, vec2(p.x + dp.x, p.y)).r;
    h.z = sampleHeight(u_heightTexture, vec2(p.x, p.y - dp.y)).r;
    h.w = sampleHeight(u_heightTexture, vec2(p.x, p.y + dp.y)).r;
    vec3 va = normalize(vec3(size.xy, h.x - h.y));
    vec3 vb = normalize(vec3(size.yx, h.z - h.w));
    vec3 n = cross(va, vb);
    return normalize(n);
}
    #endif

void main(void){
    vec4 pos = (gl_TessCoord.x * gl_in[0].gl_Position +
    gl_TessCoord.y * gl_in[1].gl_Position +
    gl_TessCoord.z * gl_in[2].gl_Position);

    o_texCoords = (gl_TessCoord.x * l_texCoords[0] + gl_TessCoord.y * l_texCoords[1] + gl_TessCoord.z * l_texCoords[2]);

    // Normal to apply height
    o_normal = normalize(gl_TessCoord.x * l_normal[0] + gl_TessCoord.y * l_normal[1] + gl_TessCoord.z * l_normal[2]);

    // Use height texture to move vertex along normal
    float h = 1.0 - sampleHeight(u_heightTexture, o_texCoords).r;
    o_fragHeight = h * u_heightScale;
    vec3 dh = o_normal * o_fragHeight;
    pos += vec4(dh, 0.0);

    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    gl_Position = u_projViewTrans * pos;

    // Plumbing
    o_fragPosition = pos.xyz;
    o_normalTan = calcNormal(o_texCoords, vec2(1.0 / u_heightSize.x, 1.0 / u_heightSize.y));
    o_depth = getDepthValue(length(pos.xyz));
    o_opacity = l_opacity[0];
    o_color = l_color[0];
    o_viewDir = (gl_TessCoord.x * l_viewDir[0] + gl_TessCoord.y * l_viewDir[1] + gl_TessCoord.z * l_viewDir[2]);
    o_lightCol = (gl_TessCoord.x * l_lightCol[0] + gl_TessCoord.y * l_lightCol[1] + gl_TessCoord.z * l_lightCol[2]);
    o_lightDir = (gl_TessCoord.x * l_lightDir[0] + gl_TessCoord.y * l_lightDir[1] + gl_TessCoord.z * l_lightDir[2]);
    o_ambientLight = (gl_TessCoord.x * l_ambientLight[0] + gl_TessCoord.y * l_ambientLight[1] + gl_TessCoord.z * l_ambientLight[2]);

    #ifdef atmosphereGround
    o_atmosphereColor = (gl_TessCoord.x * l_atmosphereColor[0] + gl_TessCoord.y * l_atmosphereColor[1] + gl_TessCoord.z * l_atmosphereColor[2]);
    o_fadeFactor = (gl_TessCoord.x * l_fadeFactor[0] + gl_TessCoord.y * l_fadeFactor[1] + gl_TessCoord.z * l_fadeFactor[2]);
    #endif

    #ifdef shadowMapFlag
    o_shadowMapUv = (gl_TessCoord.x * l_shadowMapUv[0] + gl_TessCoord.y * l_shadowMapUv[1] + gl_TessCoord.z * l_shadowMapUv[2]);
    #endif
}
