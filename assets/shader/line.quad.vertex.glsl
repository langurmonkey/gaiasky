#version 330 core

in vec4 a_position;
in vec4 a_color;
in vec2 a_uv;

uniform mat4 u_projView;
uniform vec2 u_viewport;

out vec4 v_col;
out vec2 v_uv;

#ifdef relativisticEffects
uniform vec3 u_velDir;// Velocity vector
uniform float u_vc;// Fraction of the speed of light, v/c

#include shader/lib_geometry.glsl
#include shader/lib_relativity.glsl
#endif// relativisticEffects

#ifdef gravitationalWaves
uniform vec4 u_hterms;// hpluscos, hplussin, htimescos, htimessin
uniform vec3 u_gw;// Location of gravitational wave, cartesian
uniform mat3 u_gwmat3;// Rotation matrix so that u_gw = u_gw_mat * (0 0 1)^T
uniform float u_ts;// Time in seconds since start
uniform float u_omgw;// Wave frequency
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

#include shader/lib_velbuffer.vert.glsl

void main() {
    vec4 pos = a_position;
    vec4 prevPos = pos + vec4(u_dCamPos, 0.0);

    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    v_col = a_color;
    v_uv = a_uv;

    // Position
    vec4 gpos = u_projView * pos;
    gl_Position = gpos;

    // Velocity buffer
    vec4 gprevpos = u_prevProjView * prevPos;
    v_vel = ((gpos.xy / gpos.w) - (gprevpos.xy / gprevpos.w));
}
