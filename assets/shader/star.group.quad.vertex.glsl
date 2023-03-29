#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl
#include shader/lib_doublefloat.glsl

// UNIFORMS
// time in julian days since epoch, as a 64-bit double encoded with two floats
uniform vec2 u_t;
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec2 u_solidAngleMap;
// VR scale factor
uniform float u_vrScale;
// x - alpha
// y - point size/fov factor
// z - star brightness
uniform vec3 u_alphaSizeBr;
// Brightness power
uniform float u_brightnessPower;
uniform vec2 u_opacityLimits;
// Fixed angular size
uniform float u_fixedAngularSize;

// INPUT
// Regular attributes
layout (location=0) in vec4 a_position;
layout (location=1) in vec2 a_texCoord;
// Instanced attributes
layout (location=2) in vec3 a_starPos;
layout (location=3) in vec3 a_pm;
layout (location=4) in vec4 a_color;
layout (location=5) in float a_size;

// OUTPUT
out vec4 v_col;
out vec2 v_uv;

#ifdef relativisticEffects
    #include shader/lib_relativity.glsl
#endif // relativisticEffects

#ifdef gravitationalWaves
    #include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

#define LEN0 20000.0
#define DAY_TO_YEAR 1.0 / 365.25

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
	// Lengths
	float l0 = LEN0 * u_vrScale;
	float l1 = l0 * 1e3;

    vec3 pos = a_starPos - u_camPos;

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
    if (u_fixedAngularSize <= 0) {
        solidAngle = atan(a_size / dist);
        opacity = lint(solidAngle, u_solidAngleMap.x, u_solidAngleMap.y, u_opacityLimits.x, u_opacityLimits.y);
        quadSize = clamp(a_size * pow(solidAngle * 5e8, u_brightnessPower) * u_alphaSizeBr.y, u_opacityLimits.x * 0.003 * dist, 0.5 * dist);
    } else {
        solidAngle = u_fixedAngularSize;
        opacity = 1.0;
        quadSize = 0.25e-5 * (tan(solidAngle) * dist) * u_alphaSizeBr.y;
    }
    float boundaryFade = smoothstep(l0, l1, dist);
    v_col = vec4(a_color.rgb * u_alphaSizeBr.z, clamp(opacity * u_alphaSizeBr.x * boundaryFade, 0.0, 1.0));


    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    mat4 s_proj_view = u_projView;
    float s_size = quadSize;
    #include shader/snip_billboard.glsl

    gl_Position = gpos;
    v_uv = a_texCoord;

    #ifdef velocityBufferFlag
    velocityBufferBillboard(gpos, a_starPos, s_size, a_position, s_quat, s_quat_conj);
    #endif

    if (dist < l0) {
        // The pixels of this star will be discarded in the fragment shader
        v_col = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
