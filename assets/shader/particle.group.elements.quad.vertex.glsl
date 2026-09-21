#version 330 core

#include <shader/lib/geometry.glsl>
#include <shader/lib/doublefloat.glsl>

// UNIFORMS
uniform mat4 u_projView;
uniform vec3 u_camUp;
uniform float u_alpha;
// Base dataset position (comes from parent position)
uniform vec3 u_datasetPos;
uniform float u_sizeFactor;
uniform mat4 u_refSysTransform;
uniform vec2 u_sizeLimits;
// Current julian date, in days, emulates a double in vec2
uniform vec2 u_t;
// VR scale factor
uniform float u_vrScale;
// Arbitrary affine transformation(s)
uniform bool u_transformFlag = false;
uniform mat4 u_transform;

// INPUT
// Regular attributes
layout(location = 0) in vec4 a_position;
layout(location = 1) in vec2 a_texCoord0;
// Instanced attributes
layout(location = 2) in vec4 a_color;
layout(location = 3) in vec4 a_orbitelems01;
layout(location = 4) in vec4 a_orbitelems02;
layout(location = 5) in float a_size;
layout(location = 6) in float a_textureIndex;

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

#define KM_TO_U 1e-6
#define D_TO_S 86400.0

// see https://downloads.rene-schwarz.com/download/M001-Keplerian_Orbit_Elements_to_Cartesian_State_Vectors.pdf
vec4 keplerToCartesian() {
    // Period (d)
    float period = a_orbitelems01.x;
    // Epoch (jd)
    float epoch = a_orbitelems01.y;
    // Semi-major axis (km)
    float a = a_orbitelems01.z;
    // Eccentricity
    float e = a_orbitelems01.w;
    // Inclination (rad)
    float i = a_orbitelems02.x;
    // Longitude of ascending node (rad)
    float raan = a_orbitelems02.y;
    // Argument of periapsis (rad)
    float argp = a_orbitelems02.z;
    // Mean anomaly at epoch (rad)
    float M0 = a_orbitelems02.w;

    // Mean motion (rad/day)
    float n = PI2 / period;

    // Time since epoch, in days (double, emulated)
    vec2 deltat_d;
    if (epoch == 0.0) {
        // A: epoch is start-of-time; u_t is already "time since epoch".
        deltat_d = u_t;
    } else {
        deltat_d = ds_add(u_t, ds_set(-epoch));
    }

    // Mean anomaly: accumulate in double, reduce mod 2π in double,
    // then convert to float. This is the key precision step.
    // First reduce deltat modulo the orbital period (in double): M is
    // exactly periodic in `period`, so this is equivalent, but it keeps
    // n * deltat small so that the single-precision rounding error of n
    // (~2e-7 relative) is not amplified over large time spans (JD ~ 2.5e6 d
    // would otherwise produce errors of several radians).
    vec2 deltat_p = ds_mod(deltat_d, period);
    vec2 nd = ds_mul(deltat_p, ds_set(n));      // n * deltat, double
    vec2 M_d = ds_add(ds_set(M0), nd);
    M_d = ds_mod(M_d, PI2);
    float M = M_d.x;                            // now in [0, 2π), float-exact

    // Solve Kepler’s equation: M = E - e * sin(E)
    float E = (e < 0.8) ? M : PI;
    for (int i = 0; i < 100; ++i) { // Newton-Raphson iteration
        float f = E - e * sin(E) - M;
        float fPrime = 1.0 - e * cos(E);
        float dE = -f / fPrime;
        E += dE;
        if (abs(dE) < 1e-10) break;
    }

    // True anomaly
    float sinE2 = sin(E * 0.5);
    float cosE2 = cos(E * 0.5);
    float nu = 2.0 * atan(sqrt((1.0 + e) / (1.0 - e)) * sinE2, cosE2);

    // Distance
    float r = a * (1.0 - e * cos(E));

    // Perifocal coordinates.
    float xpf = r * cos(nu);
    float ypf = r * sin(nu);
    float zpf = 0.0;

    float cosO = cos(raan);
    float sinO = sin(raan);
    float cosI = cos(i);
    float sinI = sin(i);
    float cosW = cos(argp);
    float sinW = sin(argp);

    mat3 R = mat3(
            vec3(cosO * cosW - sinO * sinW * cosI,
                -cosO * sinW - sinO * cosW * cosI,
                sinO * sinI),
            vec3(sinO * cosW + cosO * sinW * cosI,
                -sinO * sinW + cosO * cosW * cosI,
                -cosO * sinI),
            vec3(sinW * sinI,
                cosW * sinI,
                cosI)
        );

    vec3 position = vec3(xpf, ypf, zpf) * R;

    return vec4(position.yzx * KM_TO_U * u_vrScale, 1.0);
}

void main() {
    // Compute position for current time from orbital elements
    vec4 pos4;
    if (u_transformFlag) {
        pos4 = u_transform * (keplerToCartesian() * u_refSysTransform);
    } else {
        pos4 = keplerToCartesian() * u_refSysTransform;
    }
    // Particle position relative to camera.
    vec3 pos = pos4.xyz + u_datasetPos;

    // Distance to point.
    float dist = length(pos);

    #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    v_col = vec4(a_color.rgb, a_color.a * u_alpha);

    float quadSize = clamp(a_size * u_sizeFactor * u_vrScale, u_sizeLimits.x * dist, u_sizeLimits.y * dist);

    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    mat4 s_proj_view = u_projView;
    float s_size = quadSize;
    #include <shader/snippet/billboard.fast.glsl>

    gl_Position = gpos;

    computeShadingTypeOutputs(pos, s_up, s_right);
    computeShadingTypeColor(pos, u_datasetPos, v_col);

    v_uv = a_texCoord0;
    v_textureIndex = a_textureIndex;
}
