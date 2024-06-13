#version 330 core

#include <shader/lib/math.glsl>
#include <shader/lib/geometry.glsl>
#include <shader/lib/doublefloat.glsl>

// UNIFORMS
// time in julian days since epoch, as a 64-bit double encoded with two floats
uniform vec2 u_t;
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camDir;
uniform int u_cubemap;
uniform vec2 u_opacityLimits;
uniform vec2 u_thAnglePoint;
uniform float u_brightnessPower;
// VR scale factor
uniform float u_vrScale;
uniform float u_proximityThreshold;
// x - alpha
// y - point size/fov factor
// z - star brightness
// w - rc primitive scale factor
uniform vec4 u_alphaSizeBrRc;
// Fixed angular size
uniform float u_fixedAngularSize;
// Arbitrary affine transformation(s)
uniform bool u_transformFlag = false;
uniform mat4 u_transform;

// INPUT
layout (location=0) in vec3 a_position;
layout (location=1) in vec3 a_pm;
layout (location=2) in vec4 a_color;
layout (location=3) in float a_size;

// OUTPUT
out vec4 v_col;

#ifdef relativisticEffects
#include <shader/lib/relativity.glsl>
#endif // relativisticEffects

#ifdef gravitationalWaves
#include <shader/lib/gravwaves.glsl>
#endif // gravitationalWaves

#define len0 20000.0
#define day_to_year 1.0 / 365.25

void main() {
	// Lengths
	float l0 = len0 * u_vrScale;
	float l1 = l0 * 1e3;

    vec3 particlePos = a_position.xyz;
    if (u_transformFlag) {
        vec4 aux = u_transform * vec4(particlePos, 1.0);
        particlePos.xyz = aux.xyz;
    }
    vec3 pos = particlePos - u_camPos;

    // Proper motion using 64-bit emulated arithmetics:
    // pm = a_pm * t * day_to_yr
    // pos = pos + pm
    vec2 t_yr = ds_mul(u_t, ds_set(day_to_year));
    vec2 pmx = ds_mul(ds_set(a_pm.x), t_yr);
    vec2 pmy = ds_mul(ds_set(a_pm.y), t_yr);
    vec2 pmz = ds_mul(ds_set(a_pm.z), t_yr);
    pos.x = ds_add(ds_set(pos.x), pmx).x;
    pos.y = ds_add(ds_set(pos.y), pmy).x;
    pos.z = ds_add(ds_set(pos.z), pmz).x;
    // Pm for use downstream
    vec3 pm = vec3(pmx.x, pmy.x, pmz.x);

    // Distance to star
    float dist = length(pos);

    float cubemapFactor = 1.0;
    if (u_cubemap == 1) {
        // Cosine of angle between star position and camera direction
        // Correct point primitive size error due to perspective projection
        float cosphi = pow(dot(u_camDir, pos) / dist, 2.0);
        cubemapFactor = 1.0 - cosphi * 0.65;
    }

    #ifdef relativisticEffects
    	pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    float solidAngle;
    float opacity;
    float pointSize;
    if (u_fixedAngularSize <= 0.0) {
        solidAngle = (a_size * u_alphaSizeBrRc.z) / dist;
        opacity = lint(solidAngle, u_thAnglePoint.x, u_thAnglePoint.y, u_opacityLimits.x, u_opacityLimits.y);
        pointSize = max(3.3 * u_alphaSizeBrRc.w, pow(solidAngle * 0.5e8, u_brightnessPower) * u_alphaSizeBrRc.y * cubemapFactor);
    } else {
        solidAngle = u_fixedAngularSize;
        opacity = 1.0;
        pointSize = 0.2e4 * solidAngle * u_alphaSizeBrRc.y * cubemapFactor;
    }

    // Proximity.
    float fadeFactor = 1.0;
    if (u_proximityThreshold > 0.0) {
        fadeFactor = smoothstep(u_proximityThreshold * 1.5, u_proximityThreshold * 0.5, solidAngle);
    }

    float boundaryFade = smoothstep(l0, l1, dist);
    v_col = vec4(a_color.rgb, clamp(opacity * u_alphaSizeBrRc.x * boundaryFade * fadeFactor, 0.0, 1.0));

    vec4 gpos = u_projView * vec4(pos, 1.0);
    gl_Position = gpos;
    gl_PointSize = pointSize;

    if (dist < l0){
        // The pixels of this star will be discarded in the fragment shader
        v_col = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
