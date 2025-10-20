#version 330 core

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
#include <shader/lib/relativity.glsl>
#endif// relativisticEffects

#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif// gravitationalWaves

out vec4 v_color;
out vec2 v_texCoords;

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

    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    mat4 s_proj_view = u_projView;
    float s_size = u_size;
    #include <shader/snippet/billboard.fast.glsl>

    gl_Position = gpos;
}
