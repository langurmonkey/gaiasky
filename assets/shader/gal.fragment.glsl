#version 330

uniform sampler2D u_texture0;
// Distance in u to the star
uniform float u_distance;
// Apparent angle in deg
uniform float u_apparent_angle;

// v_texCoords are UV coordinates in [0..1]
in vec2 v_texCoords;
in vec4 v_color;

layout (location = 0) out vec4 fragColor;

#define light_decay 0.3
#define PI 3.1415927

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

vec4 galaxyTexture(vec2 tc){
	return texture(u_texture0, tc);
}

float light(float distance_center, float decay) {
	return 1.0 - pow(abs(sin(PI * distance_center / 2.0)), decay);
}

vec4 drawSimple(vec2 tc) {
	float dist = distance(vec2(0.5), tc) * 2.0;
	if(dist > 1.0){
		discard;
	}
	float light = light(dist, light_decay);
	return v_color * light;
}


void main() {
	fragColor = drawSimple(v_texCoords);
	fragColor *= fragColor.a;

	// Add outline
	//if (v_texCoords.x > 0.99 || v_texCoords.x < 0.01 || v_texCoords.y > 0.99 || v_texCoords.y < 0.01) {
	//    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
	//}

	#ifdef ssrFlag
	ssrBuffers();
	#endif // ssrFlag

	#ifdef velocityBufferFlag
	velocityBuffer(fragColor.a);
    #endif
}
