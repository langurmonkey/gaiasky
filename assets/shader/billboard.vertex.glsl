#version 330 core

#include <shader/lib/math.glsl>

// Attributes
in vec4 a_position;
in vec2 a_texCoord0;

// Uniforms
uniform mat4 u_projView;
uniform vec4 u_color;
uniform vec3 u_pos;
uniform float u_size;
uniform vec3 u_camUp;
uniform float u_apparent_angle;
uniform float u_th_angle_point;

#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif// relativisticEffects

#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif// gravitationalWaves

// Varyings
out vec4 v_color;
out vec2 v_uv;

void main() {
    // Solid angle threshold
    float th0 = u_th_angle_point;
    float th1 = th0 * 10.0;

    vec3 pos = u_pos;
    float dist = length(pos);

    float boundaryFade = smoothstep(th0, th1, u_apparent_angle);
    v_color = vec4(u_color.rgb * boundaryFade, u_color.a);
    v_uv = a_texCoord0;

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
