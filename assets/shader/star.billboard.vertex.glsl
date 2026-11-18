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
// World-space axes of the quad and direction to the camera, to rebuild a world-space surface normal.
out vec3 v_right;
out vec3 v_up;
out vec3 v_view;

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

    // Sampling basis from the camera up vector, so the normal map is a real world-space normal.
    vec3 dir = normalize(pos);
    if (isnan(u_camUp.x)) {
        // CUBEMAP: global orientation, same convention as the snippet.
        vec3 upRef = vec3(0.0, 1.0, 0.0);
        v_right = abs(dot(dir, upRef)) > 0.999
                  ? normalize(cross(dir, vec3(1.0, 0.0, 0.0)))
                  : normalize(cross(dir, upRef));
    } else {
        v_right = normalize(cross(-u_camUp, dir));
    }
    v_up = cross(dir, v_right);
    v_view = -dir;
}
