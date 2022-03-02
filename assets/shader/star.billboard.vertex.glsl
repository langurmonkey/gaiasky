#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl

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
uniform float u_vrScale;

#ifdef relativisticEffects
#include shader/lib_relativity.glsl
#endif// relativisticEffects

#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

// Varyings
out vec4 v_color;
out vec2 v_uv;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif // velocityBufferFlag

#define LEN0 20000.0

void main() {
    // Lengths
    float l0 = LEN0 * u_vrScale;
    float l1 = l0 * 1e3;

    vec3 pos = u_pos;
    float dist = length(pos);

    float boundaryFade = 1.0 - smoothstep(l0, l1, dist);
    float alpha = min(1.0, lint(u_apparent_angle, u_th_angle_point, u_th_angle_point * 4.0, 0.0, 1.0));
    v_color = vec4(u_color.rgb * boundaryFade, u_color.a * alpha);
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
    #include shader/snip_billboard.glsl

    gl_Position = gpos;

    #ifdef velocityBufferFlag
    velocityBufferBillboard(gpos, pos, s_size, a_position, s_quat, s_quat_conj);
    #endif// velocityBufferFlag
}
