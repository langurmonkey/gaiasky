#version 330 core

#include <shader/lib/geometry.glsl>
#ifdef extendedParticlesFlag
#include <shader/lib/doublefloat.glsl>
#endif // extendedParticlesFlag

// UNIFORMS
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camUp;
uniform float u_alpha;
uniform float u_sizeFactor;
uniform vec2 u_sizeLimits;
uniform float u_vrScale;
#ifdef extendedParticlesFlag
// time in julian days since epoch, as a 64-bit double encoded with two floats
uniform vec2 u_t;
#endif // extendedParticlesFlag
// Arbitrary affine transformation(s)
uniform bool u_transformFlag = false;
uniform mat4 u_transform;

// INPUT
// Regular attributes
layout (location=0) in vec4 a_position;
layout (location=1) in vec2 a_texCoord0;
// Instanced attributes
layout (location=2) in vec3 a_particlePos;
#ifdef extendedParticlesFlag
layout (location=3) in vec3 a_pm;
layout (location=4) in vec4 a_color;
layout (location=5) in float a_size;
layout (location=6) in float a_textureIndex;
#else
layout (location=3) in vec4 a_color;
layout (location=4) in float a_size;
layout (location=5) in float a_textureIndex;
#endif // extendedParticlesFlag

// OUTPUT
out vec4 v_col;
out vec2 v_uv;
out float v_textureIndex;

#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

#define DAY_TO_YEAR 1.0 / 365.25

#ifdef velocityBufferFlag
#include <shader/lib/velbuffer.vert.glsl>
#endif // velocityBufferFlag

void main() {
    vec3 particlePos = a_particlePos.xyz;
    if (u_transformFlag) {
        vec4 aux = u_transform * vec4(particlePos, 1.0);
        particlePos.xyz = aux.xyz;
    }
    vec3 pos = (particlePos - u_camPos) / u_vrScale;

    #ifdef extendedParticlesFlag
    // Apply proper motion if it is not zero.
    if (a_pm.x != 0.0 || a_pm.y != 0.0 || a_pm.z != 0.0) {
        // Proper motion using 64-bit emulated arithmetics:
        // pm = a_pm * t * DAY_TO_YEAR
        // pos = pos + pm
        vec2 t_yr = ds_mul(u_t, ds_set(DAY_TO_YEAR));
        vec2 pmx = ds_mul(ds_set(a_pm.x), t_yr);
        vec2 pmy = ds_mul(ds_set(a_pm.y), t_yr);
        vec2 pmz = ds_mul(ds_set(a_pm.z), t_yr);
        pos.x = ds_add(ds_set(pos.x), pmx).x;
        pos.y = ds_add(ds_set(pos.y), pmy).x;
        pos.z = ds_add(ds_set(pos.z), pmz).x;
    }
    #endif // extendedParticlesFlag

    // Distance to point - watch out, if position contains large values, this produces overflow!
    // Downscale before computing length()
    float dist = length(pos * 1e-14) * 1e14;

    #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    v_col = vec4(a_color.rgb, a_color.a * u_alpha);

    float particleSize = clamp(a_size * u_sizeFactor, u_sizeLimits.x * dist, u_sizeLimits.y * dist);

    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    mat4 s_proj_view = u_projView;
    float s_size = particleSize;
    #include <shader/snippet/billboard.glsl>

    gl_Position = gpos * u_vrScale;

    v_uv = a_texCoord0;
    v_textureIndex = a_textureIndex;

    #ifdef velocityBufferFlag
    velocityBufferBillboard(gpos, a_particlePos, s_size, a_position, s_quat, s_quat_conj);
    #endif // velocityBufferFlag
}
