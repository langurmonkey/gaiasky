#version 120

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl

// ATTRIBUTES
attribute vec3 a_position;
attribute vec3 a_pm;
attribute vec4 a_color;
attribute float a_size;


// UNIFORMS
uniform int u_t; // time in days since epoch
uniform mat4 u_projModelView;
uniform vec3 u_camPos;
uniform vec3 u_camDir;
uniform int u_cubemap;

uniform vec2 u_pointAlpha;
uniform float u_thAnglePoint;

// VARYINGS
varying float v_depth;

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

// 0 - alpha
// 1 - point size
// 2 - fov factor
// 3 - star brightness
uniform vec4 u_alphaSizeFovBr;

// VARYINGS
varying vec4 v_col;

#define len0 170000.0
#define len1 len0 * 100.0
#define day_to_year 1.0 / 365.25


#define z_near 1e9
#define z_far 1e24
#define z_onear 1.0 / z_near
#define z_ofar 1.0 / z_far
#define C 1.0

void main() {
    vec3 pos = a_position - u_camPos;
    // Proper motion
    pos = pos + a_pm * float(u_t) * day_to_year;
    
    // Distance to star
    float dist = length(pos);

    // Regular depth buffer
    //v_depth = clamp((1.0 / dist - z_onear) / (z_ofar - z_onear), 0.0, 1.0);
    //zp = z*(n+f)/(n-f) + 2fn/(n-f)
    //v_depth = dist * (z_near + z_far)/(z_near - z_far) + (2.0 * z_near * z_far) / (z_near - z_far);
    // Logarithmic depth buffer
    v_depth = log(C * dist + 1.0) / log(C * z_far + 1.0);

    float sizefactor = 1.0;
    if(u_cubemap == 1) {
        // Cosine of angle between star position and camera direction
        // Correct point primitive size error due to perspective projection
        float cosphi = pow(dot(u_camDir, pos) / dist, 2.0);
        sizefactor = 1.0 - cosphi * 0.65;
    }
    
    #ifdef relativisticEffects
    	pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves
    
    float viewAngleApparent = atan((a_size * u_alphaSizeFovBr.w) / dist) / u_alphaSizeFovBr.z;
    float opacity = pow(lint2(viewAngleApparent, 0.0, u_thAnglePoint, u_pointAlpha.x, u_pointAlpha.y), 1.2);

    float fadeout = smoothstep(dist, len0, len1);
    v_col = vec4(a_color.rgb, opacity * u_alphaSizeFovBr.x * fadeout);

	// Discard vertex if too close or Gaia Fov1or2 and not observed
    float v_discard = 1.0;
    if(dist < len0) {
        v_discard = 0.0;
        v_col *= 0.0;
    }

    gl_Position = u_projModelView * vec4(pos, 0.0) * v_discard;
    gl_PointSize = u_alphaSizeFovBr.y * sizefactor;
}
