#version 330 core

#include <shader/lib/geometry.glsl>
#ifdef extendedParticlesFlag
#include <shader/lib/doublefloat.glsl>
#endif // extendedParticlesFlag


// UNIFORMS
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camUp;
uniform vec3 u_camVel;
uniform float u_dt;
uniform float u_uToMpc;
uniform float u_alpha;
uniform float u_sizeFactor;
uniform vec2 u_sizeLimits;
uniform float u_vrScale;
uniform float u_proximityThreshold;
// App run time in seconds.
uniform float u_appTime;
// Shading style: 0: default, 1: twinkle.
uniform int u_shadingStyle;
#ifdef extendedParticlesFlag
// Time in julian days since epoch, as a 64-bit double encoded with two floats
uniform vec2 u_t;
#endif // extendedParticlesFlag
// Arbitrary affine transformation(s)
uniform bool u_transformFlag = false;
uniform mat4 u_transform;

// INPUT
// Regular attributes
layout(location = 0) in vec4 a_position;
layout(location = 1) in vec2 a_texCoord0;
// Instanced attributes
layout(location = 2) in vec3 a_particlePos;
#ifdef extendedParticlesFlag
layout(location = 3) in vec3 a_pm;
layout(location = 4) in vec4 a_color;
layout(location = 5) in float a_size;
layout(location = 6) in float a_textureIndex;
#else
layout(location = 3) in vec4 a_color;
layout(location = 4) in float a_size;
layout(location = 5) in float a_textureIndex;
#endif // extendedParticlesFlag

// OUTPUT
out vec4 v_col;
out vec2 v_uv;
out float v_textureIndex;

// Shading type (fake lighting).
#include <shader/lib/shadingtype.vert.glsl>

#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

#include <shader/lib/goldennoise.glsl>

#define DAY_TO_YEAR 1.0 / 365.25

void main() {
    vec3 particlePos = a_particlePos.xyz;
    if (u_transformFlag) {
        vec4 aux = u_transform * vec4(particlePos, 1.0);
        particlePos.xyz = aux.xyz;
    }
    // Only apply VR scale to far away positions to prevent overflow in VR mode.
    float d = dot(particlePos, particlePos);
    float vrScale;
    if (d < 1.0e30) {
        vrScale = 1.0;
    } else {
        vrScale = u_vrScale;
    }

    vec3 pos = (particlePos - u_camPos) / vrScale;

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

    // Shading style
    float shadingStyleFactor = 1.0;
    if (u_shadingStyle == 1) {
        float noise = abs(gold_noise(vec2(float(gl_InstanceID * 0.001)), 2334.943));
        shadingStyleFactor = clamp(pow(
                    (1.0 + sin(mod(u_appTime + noise * 6.0, 3.141597))) / 2.0, 5.0), 0.0, 1.0);
    }

    // Proximity.
    float fadeFactor = shadingStyleFactor;
    if (u_proximityThreshold > 0.0) {
        float solidAngle = a_size / dist;
        fadeFactor = smoothstep(u_proximityThreshold * 1.5, u_proximityThreshold * 0.5, solidAngle);
    }

    #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves


    v_col = vec4(a_color.rgb, a_color.a * u_alpha * fadeFactor);

    float particleSize = clamp(a_size * u_sizeFactor, u_sizeLimits.x * dist, u_sizeLimits.y * dist);

    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    mat4 s_proj_view = u_projView;
    float s_size = particleSize;
    #include <shader/snippet/billboard.fast.glsl>

    gl_Position = gpos * vrScale;

    computeShadingTypeOutputs(pos, s_up, s_right);

    v_uv = a_texCoord0;
    v_textureIndex = a_textureIndex;
}
