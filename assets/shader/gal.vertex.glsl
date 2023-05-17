#version 330 core

#include shader/lib_geometry.glsl

in vec4 a_position;
in vec2 a_texCoord0;

uniform mat4 u_projView;
uniform vec4 u_color;
uniform vec3 u_pos;
uniform float u_size;
// Distance in u to the star
uniform float u_distance;
uniform float u_apparent_angle;
uniform float u_vrScale;
uniform vec3 u_camUp;

#ifdef relativisticEffects
#include shader/lib_relativity.glsl
#endif// relativisticEffects

#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

out vec4 v_color;
out vec2 v_texCoords;

#define distfac 6.24e-8 / 60000.0
#define distfacinv 60000.0 / 3.23e-8

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
    v_color = u_color;
    v_texCoords = a_texCoord0;

    mat4 transform = u_projView;

    vec3 pos = u_pos;
    float dist = length(pos);

    #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    // Scale
    float size = u_size;
    if(u_distance > distfacinv){
        size *= u_distance * distfac;
    }

    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    mat4 s_proj_view = u_projView;
    float s_size = size;
    #include shader/snip_billboard.glsl

    gl_Position = gpos;

    #ifdef velocityBufferFlag
    velocityBufferBillboard(gpos, pos, s_size, a_position, s_quat, s_quat_conj);
    #endif// velocityBufferFlag
}
