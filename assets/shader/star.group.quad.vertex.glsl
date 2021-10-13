#version 330 core

#include shader/lib_math.glsl
#include shader/lib_geometry.glsl
#include shader/lib_doublefloat.glsl

in vec4 a_position;
in vec3 a_starPos;
in vec3 a_pm;
in vec4 a_color;
in float a_size;
in vec2 a_texCoord;

// time in days since epoch, as a 64-bit double encoded with two floats
uniform vec2 u_t;
uniform mat4 u_projView;
uniform vec3 u_camPos;
uniform vec3 u_camDir;
uniform vec3 u_camUp;
uniform vec4 u_quaternion;

uniform vec2 u_thAnglePoint;

uniform float u_brPow;

// VR scale factor
uniform float u_vrScale;

#ifdef relativisticEffects
#include shader/lib_relativity.glsl
#endif // relativisticEffects

#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif // gravitationalWaves

// x - alpha
// y - point size/fov factor
// z - star brightness
uniform vec3 u_alphaSizeBr;
// Brightness power
uniform float u_brightnessPower;
// Minimum solid anlge of the quads
uniform float u_minSolidAngle;

// OUTPUT
out vec4 v_col;
out vec2 v_uv;

#define LEN0 20000.0
#define DAY_TO_YEAR 1.0 / 365.25

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
	// Lengths
	float l0 = LEN0 * u_vrScale;
	float l1 = l0 * 1e3;

    vec3 pos = a_starPos - u_camPos;

    // Proper motion using 64-bit emulated arithmetics:
    // pm = a_pm * t * DAY_TO_YEAR
    // pos = pos + pm
    vec2 t_yr = ds_mul(u_t, ds_set(DAY_TO_YEAR));
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

    #ifdef relativisticEffects
    	pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    float viewAngleApparent = atan(a_size / dist);
    float opacity = lint(viewAngleApparent, u_thAnglePoint.x, u_thAnglePoint.y, 0.0, 1.0) * (u_alphaSizeBr.z - 0.4f) / 7.6;
    float boundaryFade = smoothstep(l0, l1, dist);
    v_col = vec4(a_color.rgb * clamp(opacity * u_alphaSizeBr.x * boundaryFade, 0.0, 1.0), opacity * u_alphaSizeBr.x);
    float quadSize = max(u_minSolidAngle * dist, a_size * pow(viewAngleApparent, u_brightnessPower) * (u_alphaSizeBr.y * 0.05f));

    // Use billboard snippet
    vec4 s_vert_pos = a_position;
    vec3 s_obj_pos = pos;
    vec3 s_cam_up = u_camUp;
    mat4 s_proj_view = u_projView;
    float s_size = quadSize;
    #include shader/snip_billboard.glsl

    gl_Position = gpos;

    v_uv = a_texCoord;

    #ifdef velocityBufferFlag
    vec3 prevPos = a_starPos + u_dCamPos;
    mat4 ptransform = u_prevProjView;
    translation[3][0] = prevPos.x;
    translation[3][1] = prevPos.y;
    translation[3][2] = prevPos.z;
    ptransform *= translation;
    ptransform *= rotation;
    ptransform[0][0] *= a_size;
    ptransform[1][1] *= a_size;
    ptransform[2][2] *= a_size;

    vec4 gprevpos = ptransform * a_position;
    v_vel = ((gpos.xy / gpos.w) - (gprevpos.xy / gprevpos.w));
    #endif

    if(dist < l0){
        // The pixels of this star will be discarded in the fragment shader
        v_col = vec4(0.0, 0.0, 0.0, 0.0);
    }
}
