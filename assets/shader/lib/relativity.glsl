#ifndef GLSL_LIB_RELATIVITY
#define GLSL_LIB_RELATIVITY
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