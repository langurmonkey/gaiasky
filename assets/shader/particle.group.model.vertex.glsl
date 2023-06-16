#version 330 core

#include shader/lib_geometry.glsl
#ifdef extendedParticlesFlag
#include shader/lib_doublefloat.glsl
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
#include shader/lib_relativity.glsl
#endif // relativisticEffects

#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

#define DAY_TO_YEAR 1.0 / 365.25

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif // velocityBufferFlag

#ifndef PI
#define PI 3.141592653589793238462643383
#endif // PI

void main() {
    vec3 pos = (a_particlePos - u_camPos) / u_vrScale;

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
    // Small-angle approximation, in degrees.
    float solidAngleDeg = (a_size / (dist * u_vrScale)) * 180.0 / PI;
    // When angle goes from 3 to 0.1 degrees, fade factor goes from 1 to 0.15.
    float fadeFactor = smoothstep(0.1, 3.0, solidAngleDeg) * 0.85 + 0.15;

    #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    v_col = vec4(a_color.rgb, a_color.a * u_alpha * fadeFactor);

    float particleSize = clamp(a_size * u_sizeFactor, u_sizeLimits.x * dist, u_sizeLimits.y * dist);

    // Position.
    vec4 vert_pos = vec4(a_position.xyz * particleSize, a_position.w);
    vert_pos.xyz += pos;
    vec4 gpos = u_projView * vert_pos;

    gl_Position = gpos * u_vrScale;

    v_uv = a_texCoord0;
    v_textureIndex = a_textureIndex;

    #ifdef velocityBufferFlag
    velocityBufferCam(gpos, vert_pos);
    #endif // velocityBufferFlag
}
