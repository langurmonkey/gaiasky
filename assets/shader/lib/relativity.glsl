#ifndef GLSL_LIB_RELATIVITY
#define GLSL_LIB_RELATIVITY


#ifndef PI2
#define PI2 3.14159265358979323846264338 * 2.0
#endif // PI2

#ifndef GLSL_GET_QUAT_ROTATION
#define GLSL_GET_QUAT_ROTATION
/*
 * Gets the quaternion to rotate around the given axis by the given angle in radians
 */
vec4 get_quat_rotation(vec3 axis, float radians) {
    float d = 1.0 / length(axis);
    float l_ang;
    if(radians < 0.0) {
        l_ang = PI2 - (mod(-radians, PI2));
    }else {
        l_ang = mod(radians, PI2);
    }
    float l_sin = sin(l_ang / 2.0);
    float l_cos = cos(l_ang / 2.0);
    return  normalize(vec4(d * axis.x * l_sin, d * axis.y * l_sin, d * axis.z * l_sin, l_cos));
}
#endif // GLSL_GET_QUAT_ROTATION

#ifndef GLSL_ROTATE_VERTEX_POS
#define GLSL_ROTATE_VERTEX_POS
vec3 rotate_vertex_position(vec3 position, vec3 axis, float angle) {
    vec4 q = get_quat_rotation(axis, angle);
    return position.xyz + 2.0 * cross(q.xyz, cross(q.xyz, position.xyz) + q.w * position.xyz);
}
#endif // GLSL_ROTATE_VERTEX_POS

uniform vec3 u_velDir;// Velocity vector
uniform float u_vc;// Fraction of the speed of light, v/c
// This needs lib_geometry to be included in main file
vec3 computeRelativisticAberration(vec3 pos, float poslen, vec3 veldir, float vc) { 	
	// Relativistic aberration
    // Current cosine of angle cos(th_s)
    vec3 cdir = veldir * -1.0;
    float costh_s = dot(cdir, pos) / poslen;
    float th_s = acos(costh_s);
    float costh_o = (costh_s - vc) / (1.0 - vc * costh_s);
    float th_o = acos(costh_o);
    return rotate_vertex_position(pos, normalize(cross(cdir, pos)), th_o - th_s);
}
#endif // GLSL_LIB_RELATIVITY