#version 330 core

#include <shader/lib/logdepthbuff.glsl>

uniform sampler2D u_texture;
uniform float u_scale;
uniform float u_zfar;
uniform float u_k;

// INPUT
in vec4 v_color;
in vec2 v_texCoords;
in float v_opacity;

// OUTPUT
// We use the location of the layer buffer (1).
layout (location = 1) out vec4 layerBuffer;

void main(void) {
    // Discard early based on opacity.
    if (v_opacity < 0.001) {
        discard;
    }

    // Smoothing is adapted arbitrarily to produce crisp borders at all sizes)
    float smoothing = 1.0 / (16.0 * u_scale);
    float dist = texture(u_texture, v_texCoords).a;
    float alpha = smoothstep(0.6 - smoothing, 0.6 + smoothing, dist) * v_opacity;

    // Discard based on final alpha.
	if (alpha < 0.001) {
        discard;
    }

    // Additive blending, premultiply color alpha.
    alpha *= v_color.a;
    layerBuffer = vec4(v_color.rgb, 1.0) * alpha;

    gl_FragDepth = getDepthValue(u_zfar, u_k);
}
