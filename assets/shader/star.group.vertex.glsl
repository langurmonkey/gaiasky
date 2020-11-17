#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl
#include shader/lib_doublefloat.glsl

in vec3 a_position;
in vec3 a_pm;
in vec4 a_color;
// Additional attributes:
// x - size
// y - magnitude
in vec2 a_additional;

// time in days since epoch, as a 64-bit double encoded with two floats
uniform vec2 u_t;
uniform mat4 u_projModelView;
uniform vec3 u_camPos;
uniform vec3 u_camDir;
uniform int u_cubemap;

uniform vec2 u_pointAlpha;
uniform vec2 u_thAnglePoint;

uniform float u_brPow;

// VR scale factor
uniform float u_vrScale;

uniform float u_magLimit = 22.0;

#ifdef relativisticEffects
#include shader/lib_relativity.glsl
#endif // relativisticEffects

#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

// x - alpha
// y - point size/fov factor
// z - star brightness
uniform vec3 u_alphaSizeFovBr;

out vec4 v_col;

#define len0 20000.0
#define day_to_year 1.0 / 365.25

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
	// Lengths
	float l0 = len0 * u_vrScale;
	float l1 = l0 * 1e3;

    vec3 pos = a_position - u_camPos;

    // Proper motion
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

    float viewAngleApparent = atan((a_additional.x * u_alphaSizeFovBr.z) / dist);
    float opacity = lint(viewAngleApparent, u_thAnglePoint.x, u_thAnglePoint.y, u_pointAlpha.x, u_pointAlpha.y);

    float boundaryFade = smoothstep(l0, l1, dist);

    v_col = vec4(a_color.rgb, clamp(opacity * u_alphaSizeFovBr.x * boundaryFade, 0.0, 1.0));

    vec4 gpos = u_projModelView * vec4(pos, 1.0);
    gl_Position = gpos;
    gl_PointSize = max(4.0, pow(viewAngleApparent * .5e8, u_brPow) * u_alphaSizeFovBr.y * sizefactor);

    #ifdef velocityBufferFlag
    velocityBuffer(gpos, a_position, dist, pm, vec2(500.0, 3000.0), 1.0);
    #endif

    if(dist < l0 || a_additional.y > u_magLimit){
        // The pixels of this star will be discarded in the fragment shader
        v_col = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
