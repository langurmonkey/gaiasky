#version 330 core

in vec4 a_position;
in vec4 a_color;
in float a_coord;

uniform mat4 u_worldTransform;
uniform mat4 u_projView;
uniform vec3 u_parentPos;
uniform float u_pointSize;
uniform vec2 u_viewport;

out VS_OUT {
    vec4 color;
    float coord;
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

    pos.xyz -= u_parentPos;
    pos = u_worldTransform * pos;

    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    gl_PointSize = u_pointSize;
    vs_out.color = a_color;
    vs_out.coord = a_coord;

    // Position (view-projection multiplication in geometry shader).
    gl_Position = pos;
}
