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
uniform mat4 u_projModelView;
uniform vec3 u_camPos;
uniform vec3 u_camDir;
uniform vec4 u_quaternion;

uniform vec2 u_pointAlpha;
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
// w - rc primitive scale factor
uniform vec4 u_alphaSizeFovBr;

// OUTPUT
out vec4 v_col;
out vec2 v_uv;

#define len0 20000.0
#define day_to_year 1.0 / 365.25

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

void main() {
	// Lengths
	float l0 = len0 * u_vrScale;
	float l1 = l0 * 1e3;

    mat4 transform = u_projModelView;
    vec3 pos = a_starPos - u_camPos;

    // Proper motion using 64-bit emulated arithmetics:
    // pm = a_pm * t * day_to_yr
    // pos = pos + pm
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

    #ifdef relativisticEffects
    	pos = computeRelativisticAberration(pos, dist, u_velDir, u_vc);
    #endif // relativisticEffects
    
    #ifdef gravitationalWaves
        pos = computeGravitationalWaves(pos, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif // gravitationalWaves

    float viewAngleApparent = atan((a_size * u_alphaSizeFovBr.z) / dist);
    float opacity = lint(viewAngleApparent, u_thAnglePoint.x, u_thAnglePoint.y, u_pointAlpha.x, u_pointAlpha.y);
    float boundaryFade = smoothstep(l0, l1, dist);
    v_col = vec4(a_color.rgb, clamp(opacity * u_alphaSizeFovBr.x * boundaryFade, 0.0, 1.0));
    float quadSize = max(3.3 * u_alphaSizeFovBr.w, pow(viewAngleApparent * .5e8, u_brPow) * u_alphaSizeFovBr.y * sizefactor);

    // Compute quaternion
    vec4 quat = u_quaternion;

    // Translate
    mat4 translate = mat4(1.0);

    translate[3][0] = pos.x;
    translate[3][1] = pos.y;
    translate[3][2] = pos.z;
    transform *= translate;

    // Rotate
    mat4 rotation = mat4(0.0);
    float xx = quat.x * quat.x;
    float xy = quat.x * quat.y;
    float xz = quat.x * quat.z;
    float xw = quat.x * quat.w;
    float yy = quat.y * quat.y;
    float yz = quat.y * quat.z;
    float yw = quat.y * quat.w;
    float zz = quat.z * quat.z;
    float zw = quat.z * quat.w;

    rotation[0][0] = 1.0 - 2.0 * (yy + zz);
    rotation[1][0] = 2.0 * (xy - zw);
    rotation[2][0] = 2.0 * (xz + yw);
    rotation[0][1] = 2.0 * (xy + zw);
    rotation[1][1] = 1.0 - 2.0 * (xx + zz);
    rotation[2][1] = 2.0 * (yz - xw);
    rotation[3][1] = 0.0;
    rotation[0][2] = 2.0 * (xz - yw);
    rotation[1][2] = 2.0 * (yz + xw);
    rotation[2][2] = 1.0 - 2.0 * (xx + yy);
    rotation[3][3] = 1.0;
    transform *= rotation;

    // Scale
    transform[0][0] *= quadSize;
    transform[1][1] *= quadSize;
    transform[2][2] *= quadSize;

    vec4 gpos = transform * a_position;
    gl_Position = gpos;

    v_uv = a_texCoord;

    #ifdef velocityBufferFlag
    vec3 prevPos = a_starPos + u_dCamPos;
    mat4 ptransform = u_prevProjView;
    translate[3][0] = prevPos.x;
    translate[3][1] = prevPos.y;
    translate[3][2] = prevPos.z;
    ptransform *= translate;
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
