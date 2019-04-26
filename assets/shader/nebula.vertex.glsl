#version 120

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl
#include shader/lib_logdepthbuff.glsl

attribute vec4 a_position;
attribute vec4 a_normal;
attribute vec2 a_texCoord0;
attribute vec2 a_additional;

uniform mat4 u_projModelView;
uniform vec3 u_camPos;
uniform float u_alpha;

#ifdef relativisticEffects
uniform vec3 u_velDir;// Velocity vector
uniform float u_vc;// Fraction of the speed of light, v/c

#include shader/lib_relativity.glsl
#endif// relativisticEffects

#ifdef gravitationalWaves
uniform vec4 u_hterms;// hpluscos, hplussin, htimescos, htimessin
uniform vec3 u_gw;// Location of gravitational wave, cartesian
uniform mat3 u_gwmat3;// Rotation matrix so that u_gw = u_gw_mat * (0 0 1)^T
uniform float u_ts;// Time in seconds since start
uniform float u_omgw;// Wave frequency
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

varying vec2 v_texCoords;
varying float v_alpha;
varying float v_textureNumber;
varying float v_depth;

void main()
{
    // Tex coordinates
    v_texCoords = a_texCoord0;

    // Num texture
    v_textureNumber = a_additional.x;

    // Correct position with camera
    vec3 pos = a_position.xyz - u_camPos;

    // Logarithmic depth buffer
    v_depth = getDepthValue(length(pos));

    // cross product to get angle
    float a = abs(dot(normalize(pos), a_normal.xyz));
    v_alpha = clamp(u_alpha * a * a_additional.y, 0.0, 1.0);

    #ifdef relativisticEffects
    pos = computeRelativisticAberration(pos, length(pos), u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    // Position
    gl_Position = u_projModelView * vec4(pos, 0.0);
}
