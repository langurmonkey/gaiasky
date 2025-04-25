#version 330 core

#include <shader/lib/math.glsl>
#include <shader/lib/geometry.glsl>
#include <shader/lib/doublefloat.glsl>
#include <shader/lib/angles.glsl>

// UNIFORMS
// time in julian days since epoch, as a 64-bit double encoded with two floats
uniform vec2 u_t;
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camUp;
uniform vec2 u_solidAngleMap;
uniform float u_proximityThreshold;
// App run time in seconds.
uniform float u_appTime;
// Shading style: 0: default, 1: twinkle.
uniform int u_shadingStyle;
// x - alpha
// y - point size/fov factor
// z - star brightness
uniform vec3 u_alphaSizeBr;
// Brightness power
uniform float u_brightnessPower;
// VR scale factor
uniform float u_vrScale;
// Minimum quad solid angle
uniform float u_minQuadSolidAngle;
uniform vec2 u_opacityLimits;
// Fixed angular size
uniform float u_fixedAngularSize;
// Arbitrary affine transformation(s)
uniform bool u_transformFlag = false;
uniform mat4 u_transform;

// INPUT
// Regular attributes
layout(location = 0) in vec4 a_position;
layout(location = 1) in vec2 a_texCoord0;
// Instanced attributes
layout(location = 2) in vec3 a_starPos;
layout(location = 3) in vec3 a_pm;
layout(location = 4) in vec4 a_color;
layout(location = 5) in float a_size;

// OUTPUT
out vec4 v_col;
out vec2 v_uv;

#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

#include <shader/lib/goldennoise.glsl>

#define LEN0 20000.0
#define DAY_TO_YEAR 1.0 / 365.25

void main() {
    // Lengths
    float l0 = LEN0;
    float l1 = l0 * 1e3;

    vec3 particlePos = a_starPos.xyz;
    if (u_transformFlag) {
        vec4 aux = u_transform * vec4(particlePos, 1.0);
        particlePos.xyz = aux.xyz;
    }
    vec3 pos = particlePos - u_camPos;

    // Proper motion using 64-bit emulated arithmetics:
    // pm = a_pm * t * DAY_TO_YEAR
    // pos = pos + pm
    vec3 pms = a_pm;
    vec2 t_yr = ds_mul(u_t, ds_set(DAY_TO_YEAR));
    vec2 pmx = ds_mul(ds_set(pms.x), t_yr);
    vec2 pmy = ds_mul(ds_set(pms.y), t_yr);
    vec2 pmz = ds_mul(ds_set(pms.z), t_yr);
    pos.x = ds_add(ds_set(pos.x), pmx).x;
    pos.y = ds_add(ds_set(pos.y), pmy).x;
    pos.z = ds_add(ds_set(pos.z), pmz).x;

    // Distance to star
    float dist = length(pos);

    #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    float solidAngle;
    float opacity;
    float quadSize;
    if (u_fixedAngularSize <= 0.0) {
        // We omit the arctangent and tangent, as per the small-angle approximation.
        solidAngle = a_size / dist;
        opacity = lint(solidAngle, u_solidAngleMap.x, u_solidAngleMap.y, u_opacityLimits.x, u_opacityLimits.y);
        // Clamp solid angle, and back to physical quad size.
        solidAngle = clamp(radians12(pow(degrees12(solidAngle), u_brightnessPower)), u_minQuadSolidAngle, 3.0e-8);
        quadSize = solidAngle * dist * u_alphaSizeBr.y;
    } else {
        solidAngle = u_fixedAngularSize;
        opacity = 1.0;
        quadSize = 0.25e-5 * (tan(solidAngle) * dist) * u_alphaSizeBr.y;
    }

    // Shading style
    float shadingStyleFactor = 1.0;
    if (u_shadingStyle == 1) {
        float noise = abs(gold_noise(vec2(float(gl_InstanceID)), 2334.943));
        //float reflectionFactor = (1.0 + dot(normalize(pos), normalize(particlePos / vrScale))) * 0.5;
        shadingStyleFactor = clamp(pow(
                    abs(sin(mod(u_appTime + noise * 6.0, 3.141597))), 2.0), 0.5, 1.5);
    }

    // Proximity.
    float fadeFactor = shadingStyleFactor;
    if (u_proximityThreshold > 0.0) {
        fadeFactor = smoothstep(u_proximityThreshold * 1.5, u_proximityThreshold * 0.5, solidAngle);
    }

    float boundaryFade = smoothstep(l0, l1, dist);
    v_col = vec4(a_color.rgb * u_alphaSizeBr.z, clamp(opacity * u_alphaSizeBr.x * boundaryFade * fadeFactor, 0.0, 1.0));

    // Performance trick: If the star is not seen, set it very small so that there is only one fragment, and
    // set the color to 0 to discard it in the fragment shader.
    if (v_col.a <= 1.0e-3 || dist < l0) {
        // Set size very small.
        quadSize = 0.0;
        // The pixels of this star will be discarded in the fragment shader
        v_col = vec4(0.0, 0.0, 0.0, 0.0);
    }

    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    mat4 s_proj_view = u_projView;
    float s_size = quadSize;
    #include <shader/snippet/billboard.glsl>

    gl_Position = gpos;

    v_uv = a_texCoord0;
}
