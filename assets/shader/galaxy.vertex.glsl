#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl
#include shader/lib_logdepthbuff.glsl

uniform float u_pointAlphaMin;
uniform float u_pointAlphaMax;
uniform float u_starBrightness;
uniform mat4 u_projModelView;
uniform vec3 u_camPos;
uniform float u_sizeFactor;
uniform float u_intensity;
uniform float u_ar;

uniform mat4 u_view;

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

in vec4 a_position;
in vec4 a_color;
// x - size, y - 0: star, 1: dust
in vec2 a_additional;

out vec4 v_col;
out float v_depth;
out float v_dust;

#define pc_to_u 3.085e7
#define u_to_pc 1.0 / pc_to_u
#define edge_far 2e6 * pc_to_u
#define edge_near 10 * pc_to_u

void main() {
    vec3 pos = a_position.xyz - u_camPos;
    float dist = length(pos);

    // Logarithmic depth buffer
    v_depth = getDepthValue(dist);

    #ifdef relativisticEffects
        pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    v_col = vec4(a_color.rgb, a_color.a * u_intensity);
    v_dust = a_additional.y;

    float sizeCorrection = clamp((dist * u_to_pc) / 4000.0, 0.1, 6.0);

    float dscale = smoothstep(edge_far, edge_near, dist);
    dscale = pow(dscale, 25.0) * 1.5;

    gl_Position = u_projModelView * vec4(pos, 1.0);
    gl_PointSize = a_additional.x * u_sizeFactor * u_ar * dscale;

}
