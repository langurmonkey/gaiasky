#version 330

// Distance in u to the star
uniform float u_distance;
// Apparent angle in deg
uniform float u_apparent_angle;

// v_texCoords are UV coordinates in [0..1]
in vec2 v_texCoords;
in vec4 v_color;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#define light_decay 1.3
#define PI 3.1415927

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

float light(float distance_center, float decay) {
	return 1.0 - pow(abs(sin(PI * distance_center / 2.0)), decay);
}

vec4 drawSimple(vec2 tc) {
	float dist = distance(vec2(0.5), tc) * 2.0;
	if (dist > 1.0){
		discard;
	}
	float light = light(dist, light_decay);
	return v_color * light;
}


void main() {
	fragColor = drawSimple(v_texCoords);
	fragColor.rgb *= fragColor.a;
	layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

	// Add outline
	//if (v_texCoords.x > 0.99 || v_texCoords.x < 0.01 || v_texCoords.y > 0.99 || v_texCoords.y < 0.01) {
	//    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
	//}

	#ifdef ssrFlag
	ssrBuffers();
	#endif // ssrFlag
}
