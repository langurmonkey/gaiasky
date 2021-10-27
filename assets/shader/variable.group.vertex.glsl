#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl
#include shader/lib_doublefloat.glsl

in vec3 a_position;
in vec3 a_pm;
in vec4 a_color;
in float a_nVari;
// Magnitudes
in vec4 a_vmags1;
in vec4 a_vmags2;
in vec4 a_vmags3;
in vec4 a_vmags4;
in vec4 a_vmags5;
// Times
in vec4 a_vtimes1;
in vec4 a_vtimes2;
in vec4 a_vtimes3;
in vec4 a_vtimes4;
in vec4 a_vtimes5;

// time in julian days since epoch, as a 64-bit double encoded with two floats
uniform vec2 u_t;
// time in julian days since variablity epoch
uniform float u_s;
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camDir;
uniform int u_cubemap;

uniform vec2 u_pointAlpha;
uniform vec2 u_thAnglePoint;

uniform float u_brPow;

// VR scale factor
uniform float u_vrScale;

#ifdef relativisticEffects
#include shader/lib_relativity.glsl
#endif// relativisticEffects

#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

// x - alpha
// y - point size/fov factor
// z - star brightness
// w - rc primitive scale factor
uniform vec4 u_alphaSizeFovBr;

out vec4 v_col;

#define len0 20000.0
#define day_to_year 1.0 / 365.25

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

#define N_SIZES 20

void main() {
    // Lengths
    float l0 = len0 * u_vrScale;
    float l1 = l0 * 1e3;

    vec3 pos = a_position - u_camPos;

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

    float sizefactor = 1.0;
    if (u_cubemap == 1) {
        // Cosine of angle between star position and camera direction
        // Correct point primitive size error due to perspective projection
        float cosphi = pow(dot(u_camDir, pos) / dist, 2.0);
        sizefactor = 1.0 - cosphi * 0.65;
    }

        #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

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

    float viewAngleApparent = atan((size * u_alphaSizeFovBr.z) / dist);
    float opacity = lint(viewAngleApparent, u_thAnglePoint.x, u_thAnglePoint.y, u_pointAlpha.x, u_pointAlpha.y);

    float boundaryFade = smoothstep(l0, l1, dist);

    v_col = vec4(a_color.rgb, clamp(opacity * u_alphaSizeFovBr.x * boundaryFade, 0.0, 1.0));

    vec4 gpos = u_projView * vec4(pos, 1.0);
    gl_Position = gpos;
    gl_PointSize = max(3.3 * u_alphaSizeFovBr.w, pow(viewAngleApparent * .5e8, u_brPow) * u_alphaSizeFovBr.y * sizefactor);

    #ifdef velocityBufferFlag
    velocityBuffer(gpos, a_position, dist, pm, vec2(500.0, 3000.0), 1.0);
    #endif

    if (dist < l0){
        // The pixels of this star will be discarded in the fragment shader
        v_col = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
