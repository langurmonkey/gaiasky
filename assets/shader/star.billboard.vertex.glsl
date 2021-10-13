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
out vec2 v_texCoords;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
    float alpha = min(1.0, lint(u_apparent_angle, u_th_angle_point, u_th_angle_point * 4.0, 0.0, 1.0));

    v_color = vec4(u_color.rgb, u_color.a * alpha);
    v_texCoords = a_texCoord0;

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
    vec3 s_cam_up = u_camUp;
    mat4 s_proj_view = u_projView;
    float s_size = u_size;
    #include shader/snip_billboard.glsl

    gl_Position = gpos;

    #ifdef velocityBufferFlag
    vec3 prevPos = u_pos + u_dCamPos;
    mat4 ptransform = u_prevProjView;
    translation[3][0] = prevPos.x;
    translation[3][1] = prevPos.y;
    translation[3][2] = prevPos.z;
    ptransform *= translation;
    ptransform *= rotation;
    ptransform[0][0] *= u_size;
    ptransform[1][1] *= u_size;
    ptransform[2][2] *= u_size;

    vec4 gprevpos = ptransform * a_position;
    v_vel = ((gpos.xy / gpos.w) - (gprevpos.xy / gprevpos.w));
    #endif// velocityBufferFlag
}
