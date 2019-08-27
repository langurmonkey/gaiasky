#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform sampler2D u_texture0;

// v_texCoords are UV coordinates in [0..1]
in vec2 v_texCoords;
in vec4 v_color;

out vec4 fragColor;

vec4 draw() {
    vec4 tex = texture(u_texture0, v_texCoords);
    return vec4(tex.rgb * v_color.rgb, 1.0) * v_color.a;
}

void main() {
    fragColor = draw();

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue();
}
