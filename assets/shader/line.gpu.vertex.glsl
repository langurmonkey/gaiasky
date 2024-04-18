#version 330 core

in vec4 a_position;
in vec4 a_color;
in float a_coord;

uniform mat4 u_worldTransform;
uniform mat4 u_projView;
uniform vec3 u_parentPos;
uniform float u_pointSize;
uniform float u_vrScale;
uniform vec2 u_viewport;

out vec4 v_col;
out float v_coord;
out vec2 v_lineCenter;

#include <shader/lib/geometry.glsl>

#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif// relativisticEffects

#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
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
    v_col = a_color;
    v_coord = a_coord;

    // Position
    vec4 gpos = u_projView * pos;
    gl_Position = gpos;

    // Line center
    vec3 ndc = gpos.xyz / gpos.w; //perspective divide/normalize
    vec2 viewportCoord = ndc.xy * 0.5 + 0.5; //ndc is -1 to 1 in GL. scale for 0 to 1
    v_lineCenter = viewportCoord * u_viewport;
}
