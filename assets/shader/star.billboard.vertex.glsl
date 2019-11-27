#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl

// Attributes
in vec4 a_position;
in vec2 a_texCoord0;

// Uniforms
uniform mat4 u_projTrans;
uniform vec4 u_color;
uniform vec4 u_quaternion;
uniform vec3 u_pos;
uniform float u_size;
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

    mat4 transform = u_projTrans;

    vec3 pos = u_pos;
    float dist = length(pos);

    #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    // Compute quaternion
    vec4 quat = u_quaternion;

    // Translate
    mat4 translate = mat4(1.0);

    translate[3][0] = pos.x;
    translate[3][1] = pos.y;
    translate[3][2] = pos.z;
    transform *= translate;

    // Rotate
    mat4 rotation = mat4(0.0);
    float xx = quat.x * quat.x;
    float xy = quat.x * quat.y;
    float xz = quat.x * quat.z;
    float xw = quat.x * quat.w;
    float yy = quat.y * quat.y;
    float yz = quat.y * quat.z;
    float yw = quat.y * quat.w;
    float zz = quat.z * quat.z;
    float zw = quat.z * quat.w;

    rotation[0][0] = 1.0 - 2.0 * (yy + zz);
    rotation[1][0] = 2.0 * (xy - zw);
    rotation[2][0] = 2.0 * (xz + yw);
    rotation[0][1] = 2.0 * (xy + zw);
    rotation[1][1] = 1.0 - 2.0 * (xx + zz);
    rotation[2][1] = 2.0 * (yz - xw);
    rotation[3][1] = 0.0;
    rotation[0][2] = 2.0 * (xz - yw);
    rotation[1][2] = 2.0 * (yz + xw);
    rotation[2][2] = 1.0 - 2.0 * (xx + yy);
    rotation[3][3] = 1.0;
    transform *= rotation;

    // Scale
    transform[0][0] *= u_size;
    transform[1][1] *= u_size;
    transform[2][2] *= u_size;

    // Position
    vec4 gpos = transform * a_position;
    gl_Position = gpos;

    #ifdef velocityBufferFlag
    vec3 prevPos = u_pos + u_dCamPos;
    mat4 ptransform = u_prevProjView;
    translate[3][0] = prevPos.x;
    translate[3][1] = prevPos.y;
    translate[3][2] = prevPos.z;
    ptransform *= translate;
    ptransform *= rotation;
    ptransform[0][0] *= u_size;
    ptransform[1][1] *= u_size;
    ptransform[2][2] *= u_size;

    vec4 gprevpos = ptransform * a_position;
    v_vel = ((gpos.xy / gpos.w) - (gprevpos.xy / gprevpos.w));
    #endif// velocityBufferFlag
}
