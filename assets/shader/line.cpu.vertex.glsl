#version 330 core

in vec4 a_position;
in vec4 a_color;

uniform mat4 u_projView;
uniform float u_vrScale;

out vec4 v_col;

#include <shader/lib/geometry.glsl>

#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

void main() {
    vec4 pos = a_position;

    #ifdef relativisticEffects
        pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    v_col = a_color;

    // Position
    vec4 gpos = u_projView * pos;
    gl_Position = gpos;
}
