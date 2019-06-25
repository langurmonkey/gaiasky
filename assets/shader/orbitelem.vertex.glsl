#version 330 core

#include shader/lib_geometry.glsl
#include shader/lib_logdepthbuff.glsl

attribute vec4 a_color;
attribute vec4 a_orbitelems01;
attribute vec4 a_orbitelems02;
attribute float a_size;

uniform mat4 u_projModelView;
uniform mat4 u_eclToEq;
uniform vec3 u_camPos;
uniform vec3 u_camDir;
uniform float u_alpha;
uniform float u_size;
uniform float u_scaleFactor;
uniform int u_cubemap;
// Current julian date, in days
uniform float u_t;
// dt in seconds since epoch (assumes all objects have the same epoch!)
uniform float u_dt_s;

#ifdef relativisticEffects
    uniform vec3 u_velDir; // Velocity vector
    uniform float u_vc; // Fraction of the speed of light, v/c
    
    #include shader/lib_relativity.glsl
#endif // relativisticEffects

#ifdef gravitationalWaves
    uniform vec4 u_hterms; // hpluscos, hplussin, htimescos, htimessin
    uniform vec3 u_gw; // Location of gravitational wave, cartesian
    uniform mat3 u_gwmat3; // Rotation matrix so that u_gw = u_gw_mat * (0 0 1)^T
    uniform float u_ts; // Time in seconds since start
    uniform float u_omgw; // Wave frequency
    #include shader/lib_gravwaves.glsl
#endif // gravitationalWaves
    
out vec4 v_col;
out float v_depth;

#define M_TO_U 1e-9

// see https://downloads.rene-schwarz.com/download/M001-Keplerian_Orbit_Elements_to_Cartesian_State_Vectors.pdf
vec4 keplerToCartesian() {
    float musola3 = a_orbitelems01.x;
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
    float deltat = u_dt_s;
    float M = M0 + deltat * musola3;
    
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
    x *= M_TO_U;
    y *= M_TO_U;
    z *= M_TO_U;
    
    return vec4(y, z, x, 1.0);
}

void main() {
    // Compute position for current time from orbital elements
    vec4 pos4 = keplerToCartesian() * u_eclToEq;
    vec3 pos = pos4.xyz - u_camPos;

    // Distance to point
    float dist = length(pos);

    float cubemapSizeFactor = 1.0;
    if(u_cubemap == 1) {
        // Cosine of angle between star position and camera direction
        // Correct point primitive size error due to perspective projection
        float cosphi = pow(dot(u_camDir, pos) / dist, 2.0);
        cubemapSizeFactor = 1.0 - cosphi * 0.65;
    }

    // Logarithmic depth buffer
    v_depth = getDepthValue(dist);

    #ifdef relativisticEffects
        pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    v_col = a_color * u_alpha;

    gl_Position = u_projModelView * vec4(pos, 0.0);
    float distNorm = dist / 300.0;
    gl_PointSize = clamp(u_size / distNorm, 1.5, 3.5) * u_scaleFactor * cubemapSizeFactor * a_size;
}
