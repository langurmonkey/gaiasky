#version 410 core

in vec2 o_texCoords;

in float o_opacity;

out vec4 fragColor;

#include shader/lib_logdepthbuff.glsl

void main() {
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue();
}

