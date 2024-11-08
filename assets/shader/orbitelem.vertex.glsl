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

#define M_TO_U 1e-9
#define D_TO_S 86400.0

// see https://downloads.rene-schwarz.com/download/M001-Keplerian_Orbit_Elements_to_Cartesian_State_Vectors.pdf
vec4 keplerToCartesian() {
    // sqrt(mu/a^3), with a in m
    float sqrtMuOverA3 = a_orbitelems01.x;
    float epoch = a_orbitelems01.y;
    // Semi-major axis
    float a = a_orbitelems01.z;
    // Eccentricity
    float e = a_orbitelems01.w;
    // Inclination
    float i = a_orbitelems02.x;
    // Longitude of ascending node
    float omega_lan = a_orbitelems02.y;
    // Argument of periapsis
    float omega_ap = a_orbitelems02.z;
    // Mean anomaly at epoch 
    float M0 = a_orbitelems02.w;
    
    // 1
    vec2 epoch_d = ds_set(epoch);
    vec2 deltat_d = ds_mul(ds_add(u_t, -epoch_d), ds_set(D_TO_S));
    float deltat = deltat_d.x;

    float M = M0 + deltat * sqrtMuOverA3;
    
    // 2
    float E = M;
    for(int j = 0; j < 2; j++) {
        E = E - ((E - e * sin(E) - M) / ( 1.0 - e * cos(E))); 
    }
    float E_t = E;
    
    // 3
    float nu_t = 2.0 * atan(sqrt(1.0 + e) * sin(E_t / 2.0), sqrt(1.0 - e) * cos(E_t / 2.0)); 
            
    // 4
    float rc_t = a * (1.0 - e * cos(E_t));
    
    // 5
    float ox = rc_t * cos(nu_t);
    float oy = rc_t * sin(nu_t);


    // 6
    float sinomega = sin(omega_ap);
    float cosomega = cos(omega_ap);
    float sinOMEGA = sin(omega_lan);
    float cosOMEGA = cos(omega_lan);
    float cosi = cos(i);
    float sini = sin(i);
    
    float x = ox * (cosomega * cosOMEGA - sinomega * cosi * sinOMEGA) - oy * (sinomega * cosOMEGA + cosomega * cosi * sinOMEGA);
    float y = ox * (cosomega * sinOMEGA + sinomega * cosi * cosOMEGA) + oy * (cosomega * cosi * cosOMEGA - sinomega * sinOMEGA);
    float z = ox * (sinomega * sini) + oy * (cosomega * sini);
    
    // 7
    float fac = M_TO_U * u_vrScale;
    x *= fac;
    y *= fac;
    z *= fac;
    
    return vec4(y, z, x, 1.0);
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
