#version 330 core

#include <shader/lib/geometry.glsl>
#include <shader/lib/doublefloat.glsl>

// UNIFORMS
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camUp;
uniform float u_alpha;
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
in vec4 a_position;
in vec2 a_texCoord;
in vec4 a_color;
in vec4 a_orbitelems01;
in vec4 a_orbitelems02;
in float a_size;
in float a_textureIndex;

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

    // Time since epoch in seconds
    vec2 epoch_d = ds_set(epoch);
    vec2 deltat_d = ds_mul(ds_add(u_t, -epoch_d), ds_set(D_TO_S));
    float deltat = deltat_d.x;

    // Mean motion (rad/s)
    float n = PI2 / (period * D_TO_S);

    // Mean anomaly at target time
    float M = mod(M0 + n * deltat, PI2);

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
    vec3 pos = pos4.xyz - u_camPos;

    // Distance to point
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
    #include <shader/snippet/billboard.glsl>

    gl_Position = gpos;

    v_uv = a_texCoord;
    v_textureIndex = a_textureIndex;
}
