#version 330 core

in vec4 a_position;
in vec4 a_color;

uniform vec2 u_viewport;
uniform float u_vrScale;

out VS_OUT {
    vec4 color;
} vs_out;

#include shader/lib_geometry.glsl

#ifdef relativisticEffects
#include shader/lib_relativity.glsl
#endif// relativisticEffects

#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

void main() {
    vec4 pos = a_position;

    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    vs_out.color = a_color;

    // Position (view-projection multiplication in geometry shader).
    gl_Position = pos;
}
