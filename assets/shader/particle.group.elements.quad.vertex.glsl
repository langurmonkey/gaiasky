#version 330 core

#include <shader/lib/geometry.glsl>

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

// ---------------------------------------------------------------------------
// Orbital mechanics in double-float. The time -> mean anomaly stage runs in
// double-float in both paths; the camera-relative path also runs Kepler's
// equation, the trig and the reference-system transform in double-float.
// ---------------------------------------------------------------------------
#include <shader/lib/doublefloat.glsl>
#include <shader/lib/doublefloat.trig.glsl>

// Mean anomaly (rad) in double-float, for the current time and this particle's
// orbital elements.
vec2 meanAnomaly() {
    float period = a_orbitelems01.x;
    float epoch = a_orbitelems01.y;
    float M0 = a_orbitelems02.w;
    vec2 deltat;
    if (epoch == 0.0) {
        // Epoch is start-of-time; u_t is already "time since epoch".
        deltat = u_t;
    } else {
        deltat = ds_sub(u_t, ds_set(epoch));
    }
    return ds_meanAnomaly(deltat, period, M0);
}

// Camera-relative position (internal units). Everything down to the last
// addition is computed in double-float; the result collapses once at the end.
vec3 keplerToCameraRelativeDS() {
    float a = a_orbitelems01.z;
    float e = a_orbitelems01.w;
    float i = a_orbitelems02.x;
    float raan = a_orbitelems02.y;
    float argp = a_orbitelems02.z;

    vec2 M_d = meanAnomaly();

    // Kepler's equation: float seed, then two Newton steps in double-float
    float M = M_d.x;
    float E = (e < 0.8) ? M : PI;
    for (int it = 0; it < 100; ++it) {
        float f = E - e * sin(E) - M;
        float dE = -f / (1.0 - e * cos(E));
        E += dE;
        if (abs(dE) < 1e-7) break;
    }
    vec2 Ed = ds_set(E);
    vec2 cE, sE;
    for (int it = 0; it < 2; ++it) {
        ds_sinCos(Ed, cE, sE);
        vec2 f = ds_sub(ds_sub(Ed, ds_mul(ds_set(e), sE)), M_d);
        Ed = ds_sub(Ed, ds_set(f.x / (1.0 - e * cE.x)));
    }
    ds_sinCos(Ed, cE, sE);

    // Perifocal coordinates (km)
    vec2 xpf = ds_mul(ds_set(a), ds_sub(cE, ds_set(e)));
    vec2 ypf = ds_mul(ds_set(a * sqrt(1.0 - e * e)), sE);

    float cosO = cos(raan);
    float sinO = sin(raan);
    float cosI = cos(i);
    float sinI = sin(i);
    float cosW = cos(argp);
    float sinW = sin(argp);
    // Rotation columns; position = vec3(xpf, ypf, 0) * R
    vec3 c0 = vec3(cosO * cosW - sinO * sinW * cosI, -cosO * sinW - sinO * cosW * cosI, sinO * sinI);
    vec3 c1 = vec3(sinO * cosW + cosO * sinW * cosI, -sinO * sinW + cosO * cosW * cosI, -cosO * sinI);
    vec3 c2 = vec3(sinW * sinI, cosW * sinI, cosI);
    vec2 px = ds_add(ds_mul(xpf, ds_set(c0.x)), ds_mul(ypf, ds_set(c0.y)));
    vec2 py = ds_add(ds_mul(xpf, ds_set(c1.x)), ds_mul(ypf, ds_set(c1.y)));
    vec2 pz = ds_add(ds_mul(xpf, ds_set(c2.x)), ds_mul(ypf, ds_set(c2.y)));

    // .yzx, km -> internal units
    vec2 sc = ds_set(KM_TO_U * u_vrScale);
    vec2 v0 = ds_mul(py, sc);
    vec2 v1 = ds_mul(pz, sc);
    vec2 v2 = ds_mul(px, sc);

    // vec4(v, 1) * u_refSysTransform: result[j] = dot(v, column j)
    vec2 r[3];
    for (int j = 0; j < 3; ++j) {
        vec4 col = u_refSysTransform[j];
        vec2 acc = ds_add(ds_mul(v0, ds_set(col.x)), ds_mul(v1, ds_set(col.y)));
        acc = ds_add(acc, ds_mul(v2, ds_set(col.z)));
        acc = ds_add(acc, ds_set(col.w));
        r[j] = acc;
    }

    // + u_datasetPos (camera-relative origin of the dataset), collapsed to float
    vec3 pos;
    pos.x = ds_add(r[0], ds_set(u_datasetPos.x)).x;
    pos.y = ds_add(r[1], ds_set(u_datasetPos.y)).x;
    pos.z = ds_add(r[2], ds_set(u_datasetPos.z)).x;
    return pos;
}

// see https://downloads.rene-schwarz.com/download/M001-Keplerian_Orbit_Elements_to_Cartesian_State_Vectors.pdf
vec4 keplerToCartesian() {
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

    // Mean anomaly: only the time stage is double-float here
    float M = meanAnomaly().x;

    // Solve Kepler's equation: M = E - e * sin(E)
    float E = (e < 0.8) ? M : PI;
    for (int i = 0; i < 100; ++i) { // Newton-Raphson iteration
        float f = E - e * sin(E) - M;
        float fPrime = 1.0 - e * cos(E);
        float dE = -f / fPrime;
        E += dE;
        if (abs(dE) < 1e-7) break;
    }

    // True anomaly
    float sinE2 = sin(E * 0.5);
    float cosE2 = cos(E * 0.5);
    float nu = 2.0 * atan(sqrt((1.0 + e) / (1.0 - e)) * sinE2, cosE2);

    // Distance
    float r = a * (1.0 - e * cos(E));

    // Perifocal coordinates (km)
    float xpf = r * cos(nu);
    float ypf = r * sin(nu);

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

    vec3 position = vec3(xpf, ypf, 0.0) * R;

    return vec4(position.yzx * KM_TO_U * u_vrScale, 1.0);
}

void main() {
    // Particle position relative to camera.
    vec3 pos;
    if (u_transformFlag) {
        pos = (u_transform * (keplerToCartesian() * u_refSysTransform)).xyz + u_datasetPos;
    } else {
        // Already includes u_datasetPos; collapsed from double-float inside.
        pos = keplerToCameraRelativeDS();
    }

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
