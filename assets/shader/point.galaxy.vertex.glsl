#version 120

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl

attribute vec4 a_position;
attribute vec4 a_color;
// x - size, y - th_angle_point
attribute vec4 a_additional;

uniform float u_pointAlphaMin;
uniform float u_pointAlphaMax;
uniform float u_starBrightness;
uniform mat4 u_projModelView;
uniform vec3 u_camPos;

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

varying vec4 v_col;

void main() {
    vec3 pos = a_position.xyz - u_camPos;
    float dist = length(pos);
    
    #ifdef relativisticEffects
        pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    float distNorm = dist / 800000000000.0;

    v_col = a_color;

    gl_Position = u_projModelView * vec4(pos, a_position.w);
    gl_PointSize = a_additional.x / distNorm;
}
