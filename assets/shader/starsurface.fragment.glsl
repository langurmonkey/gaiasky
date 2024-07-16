#version 330 core
// UNIFORMS

uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

// VARYINGS

// Time in seconds
in float v_time;
// Ambient color (star color in this case)
in vec3 v_lightDiffuse;
// The normal
in vec3 v_normal;
// Coordinate of the texture
in vec2 v_texCoords0;
// Opacity
in float v_opacity;
// View vector
in vec3 v_viewVec;

#include <shader/lib/logdepthbuff.glsl>

layout (location = 0) out vec4 fragColor;

#define time v_time * 0.003

#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/perlin.glsl>

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

void main() {
    // Perimeter is 1 when normal faces camera, 0 when normal is 90 degrees from view.
    float perimeter = dot(normalize(v_normal), vec3(v_viewVec)) * 0.5;
    vec3 percolor = v_lightDiffuse * min(1.0, perimeter + 0.4);
    
    // Star surface
    gln_tFBMOpts opts = gln_tFBMOpts(0.3245, // seed
                                    1.0,     // amplitude
                                    0.7,     // persistence
                                    30.0,    // frequency
                                    2.0,     // lacunarity
                                    vec3(6.0, 6.0, 6.0), // scale
                                    1.0,     // power
                                    4,       // octaves
                                    false,
                                    false);
    vec3 position = vec3(v_texCoords0, time * 0.4);
	float n = (gln_pfbm(position, opts) + 1.75) * 0.125;
	 
	// Sunspots
	float s = 0.6;
	float un_radius = 4.0;
	vec3 spos = position * un_radius;
	float frequency = 10.0;
	float t1 = gln_perlin(spos * frequency) - s;
	float t2 = gln_perlin((spos + un_radius) * frequency) - s;
	float ss = (max(t1, 0.0) * max(t2, 0.0)) * 2.0;
	// Accumulate total noise
	float total = n - ss;

	vec3 color = vec3(total, total, total);
    fragColor = vec4(min(vec3(0.9), color * 6.0 * v_lightDiffuse * percolor), v_opacity);

    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}