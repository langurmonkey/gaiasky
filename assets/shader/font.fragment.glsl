#version 330 core

#include <shader/lib/logdepthbuff.glsl>

uniform sampler2D u_texture;
uniform float u_scale;
uniform float u_opacity;
uniform float u_zfar;
uniform float u_k;

// INPUT
in vec4 v_color;
in vec2 v_texCoords;
in float v_opacity;

// OUTPUT
layout (location = 0) out vec4 fragColor;

void main(void){
    // Smoothing is adapted arbitrarily to produce crisp borders at all sizes)
    float smoothing = 1.0 / (16.0 * u_scale);
    float dist = texture(u_texture, v_texCoords).a;
    float alpha = smoothstep(0.6 - smoothing, 0.6 + smoothing, dist);
    float aa = alpha * v_opacity * u_opacity;
	
	if (aa < 0.001)
	    discard;

    // Additive
    float a = aa * v_color.a;
    fragColor = vec4(v_color.rgb, 1.0) * a;

    gl_FragDepth = getDepthValue(u_zfar, u_k);
}
