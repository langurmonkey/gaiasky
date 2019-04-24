#version 120

// v_texCoords are UV coordinates in [0..1]
varying vec2 v_texCoords;
varying vec4 v_color;

uniform sampler2D u_texture0;
// Distance in u to the star
uniform float u_distance;
// Apparent angle in deg
uniform float u_apparent_angle;
// Component alpha (galaxies)
uniform float u_alpha;

uniform float u_time;


#define distfac 3.24e-8 / 60000.0
#define distfacinv 60000.0 / 3.23e-8
#define light_decay 0.5


vec4 galaxyTexture(vec2 tc){
	return texture2D(u_texture0, tc);
}

float light(float distance_center, float decay) {
    return 1.0 - pow(distance_center, decay);
}

vec4 drawSimple(vec2 tc) {
	float dist = distance (vec2 (0.5), tc) * 2.0;
	float light = light(dist, light_decay);
	return vec4(v_color.rgb, v_color.a) * light;
}


void main() {

	// float factor = smoothstep(distfacinv/8.0, distfacinv, u_distance);
	gl_FragColor = drawSimple(v_texCoords) * u_alpha;

    // Debug! - visualise depth buffer
    //gl_FragColor = vec4(vec3(gl_FragCoord.z), 1.0f);
}
