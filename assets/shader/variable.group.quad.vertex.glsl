#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl
#include shader/lib_doublefloat.glsl

// UNIFORMS
// time in julian days since epoch, as a 64-bit double encoded with two floats
uniform vec2 u_t;
// time in julian days since variablity epoch
uniform float u_s;
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

// INPUT
// Regular attributes
layout (location=0) in vec4 a_position;
layout (location=1) in vec2 a_texCoord;
// Instanced attributes
layout (location=2) in vec3 a_starPos;
layout (location=3) in vec3 a_pm;
layout (location=4) in vec4 a_color;
layout (location=5) in float a_nVari;
// Magnitudes
layout (location=6) in vec4 a_vmags1;
layout (location=7) in vec4 a_vmags2;
layout (location=8) in vec4 a_vmags3;
layout (location=9) in vec4 a_vmags4;
layout (location=10) in vec4 a_vmags5;
// Times
layout (location=11) in vec4 a_vtimes1;
layout (location=12) in vec4 a_vtimes2;
layout (location=13) in vec4 a_vtimes3;
layout (location=14) in vec4 a_vtimes4;
layout (location=15) in vec4 a_vtimes5;

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

#define N_SIZES 20

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
    // Pm for use downstream
    vec3 pm = vec3(pmx.x, pmy.x, pmz.x);

    // Distance to star
    float dist = length(pos);

    #ifdef relativisticEffects
    	pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    // Magnitudes vector
    float vmags[N_SIZES];
    vmags[0] = a_vmags1[0];
    vmags[1] = a_vmags1[1];
    vmags[2] = a_vmags1[2];
    vmags[3] = a_vmags1[3];
    vmags[4] = a_vmags2[0];
    vmags[5] = a_vmags2[1];
    vmags[6] = a_vmags2[2];
    vmags[7] = a_vmags2[3];
    vmags[8] = a_vmags3[0];
    vmags[9] = a_vmags3[1];
    vmags[10] = a_vmags3[2];
    vmags[11] = a_vmags3[3];
    vmags[12] = a_vmags4[0];
    vmags[13] = a_vmags4[1];
    vmags[14] = a_vmags4[2];
    vmags[15] = a_vmags4[3];
    vmags[16] = a_vmags5[0];
    vmags[17] = a_vmags5[1];
    vmags[18] = a_vmags5[2];
    vmags[19] = a_vmags5[3];
    // Times vector
    float vtimes[N_SIZES];
    vtimes[0] = a_vtimes1[0];
    vtimes[1] = a_vtimes1[1];
    vtimes[2] = a_vtimes1[2];
    vtimes[3] = a_vtimes1[3];
    vtimes[4] = a_vtimes2[0];
    vtimes[5] = a_vtimes2[1];
    vtimes[6] = a_vtimes2[2];
    vtimes[7] = a_vtimes2[3];
    vtimes[8] = a_vtimes3[0];
    vtimes[9] = a_vtimes3[1];
    vtimes[10] = a_vtimes3[2];
    vtimes[11] = a_vtimes3[3];
    vtimes[12] = a_vtimes4[0];
    vtimes[13] = a_vtimes4[1];
    vtimes[14] = a_vtimes4[2];
    vtimes[15] = a_vtimes4[3];
    vtimes[16] = a_vtimes5[0];
    vtimes[17] = a_vtimes5[1];
    vtimes[18] = a_vtimes5[2];
    vtimes[19] = a_vtimes5[3];

    // Linear interpolation of time in light curve
    int nVari = int(a_nVari);
    float t0 = vtimes[0];
    float t1 = vtimes[nVari - 1];
    float period = t1 - t0;
    float t = mod(u_s, period);
    float size = vmags[0];
    for (int i = 0; i < nVari - 1; i++) {
        float x0 = vtimes[i] - t0;
        float x1 = vtimes[i+1] - t0;
        if (t >= x0 && t <= x1) {
            size = lint(t, x0, x1, vmags[i], vmags[i+1]);
            break;
        } else {
            // Next
        }
    }

    float solidAngle = atan(size / dist);
    float opacity = lint(solidAngle, u_solidAngleMap.x, u_solidAngleMap.y, u_opacityLimits.x, u_opacityLimits.y);
    float boundaryFade = smoothstep(l0, l1, dist);
    v_col = vec4(a_color.rgb * u_alphaSizeBr.z, clamp(opacity * u_alphaSizeBr.x * boundaryFade, 0.0, 1.0));

    float quadSize = clamp(size * pow(solidAngle * 5e8, u_brightnessPower) * u_alphaSizeBr.y, u_opacityLimits.x * 0.002 * dist, 0.5 * dist);

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
