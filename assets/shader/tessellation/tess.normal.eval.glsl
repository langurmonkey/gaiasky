#version 410 core

layout (triangles) in;

// GEOMETRY (QUATERNIONS)
#if defined(velocityBufferFlag) || defined(relativisticEffects)
#include shader/lib_geometry.glsl
#endif

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
#include shader/lib_relativity.glsl
#endif// relativisticEffects


////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

uniform mat4 u_projViewTrans;

uniform float u_heightScale;
uniform float u_heightNoiseSize;
uniform vec2 u_heightSize;
uniform sampler2D u_heightTexture;
uniform float u_vrScale;

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

#include shader/lib_sampleheight.glsl
#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

    #ifdef normalTextureFlag
// Use normal map
uniform sampler2D u_normalTexture;
vec3 calcNormal(vec2 p, vec2 dp){
    return normalize(texture(u_normalTexture, p).rgb * 2.0 - 1.0);
}
    #else
// maps the height scale in internal units to a normal strength
float computeNormalStrength(float heightScale){
    // to [0,100] km
    vec2 heightSpanKm = vec2(0.0, 80.0);
    vec2 span = vec2(0.2, 1.0);
    heightScale *= u_vrScale * 1e6;
    heightScale = clamp(heightScale, heightSpanKm.x, heightSpanKm.y);
    // normalize to [0,1]
    heightScale = (heightSpanKm.y - heightScale) / (heightSpanKm.y - heightSpanKm.x);
    return span.x + (span.y - span.x) * heightScale;
}
// Use height texture for normals
vec3 calcNormal(vec2 p, vec2 dp){
    vec4 h;
    vec2 size = vec2(computeNormalStrength(u_heightScale), 0.0);
    if (dp.x < 0.0){
        // Generated height using perlin noise
        dp = vec2(3e-4);
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

    vec4 gpos = u_projViewTrans * pos;
    gl_Position = gpos;

    #ifdef velocityBufferFlag
    velocityBufferCam(gpos, pos);
    #endif// velocityBufferFlag

    // Plumbing
    o_fragPosition = pos.xyz;
    o_normalTan = calcNormal(o_texCoords, vec2(1.0 / u_heightSize.x, 1.0 / u_heightSize.y));
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
