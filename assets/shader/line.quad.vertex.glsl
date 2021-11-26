#version 330 core

in vec4 a_position;
in vec4 a_color;
in vec2 a_uv;

uniform mat4 u_projView;
uniform vec2 u_viewport;
uniform float u_vrScale;

out vec4 v_col;
out vec2 v_uv;

#include shader/lib_geometry.glsl

#ifdef relativisticEffects
#include shader/lib_relativity.glsl
#endif// relativisticEffects

#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
    vec4 pos = a_position;

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

    #ifdef velocityBufferFlag
    velocityBufferCam(gpos, pos);
    #endif
}
